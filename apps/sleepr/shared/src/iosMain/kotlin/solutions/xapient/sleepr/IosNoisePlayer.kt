package solutions.xapient.sleepr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import solutions.xapient.sleepr.domain.FS
import kotlin.concurrent.Volatile
import kotlin.random.Random

/**
 * Rauschen ueber AVAudioEngine: ein Player-Knoten bekommt reihum vier Puffer (je 4096 Frames, zusammen rund 340 ms),
 * jeder wird nach dem Abspielen neu berechnet. Kategorie Playback = spielt bei gesperrtem Bildschirm weiter
 * (UIBackgroundModes audio). Unterbrechung (Anruf, Wecker) oder abgezogene Kopfhoerer = Stopp, wie der Audio-Fokus auf Android.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNoisePlayer(private val mix: Mix) : NoisePlayer {
    @Volatile override var playing = false
        private set
    @Volatile override var renderer: NoiseRenderer? = null
        private set

    private var engine: AVAudioEngine? = null
    private var node: AVAudioPlayerNode? = null
    private val format = AVAudioFormat(standardFormatWithSampleRate = FS.toDouble(), channels = 2u)
    private val queue = dispatch_queue_create("sleepr.audio", null)
    private val left = FloatArray(FRAMES)
    private val right = FloatArray(FRAMES)
    private var pending = 0 // nur auf der Audio-Queue
    @Volatile private var done = false
    @Volatile private var session = 0
    private var observers = listOf<Any>()
    private var text = APP_NAME

    init {
        val center = NSNotificationCenter.defaultCenter
        observers = listOf(
            center.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) { n -> onInterruption(n) },
            center.addObserverForName(AVAudioSessionRouteChangeNotification, null, NSOperationQueue.mainQueue) { n -> onRouteChange(n) },
        )
        remoteCommands()
    }

    override fun play() {
        if (playing) return
        val audio = AVAudioSession.sharedInstance()
        audio.setCategory(AVAudioSessionCategoryPlayback, null)
        audio.setActive(true, null)
        val e = AVAudioEngine()
        val n = AVAudioPlayerNode()
        e.attachNode(n)
        e.connect(n, e.mainMixerNode, format)
        e.prepare()
        if (!e.startAndReturnError(null)) {
            audio.setActive(false, null)
            return
        }
        renderer = NoiseRenderer(mix, Random.nextLong())
        engine = e; node = n
        done = false
        playing = true
        val id = ++session
        // Berechnen nur auf der Audio-Queue: erste Runde sofort, danach jeweils nach dem Abspielen eines Puffers
        dispatch_async(queue) {
            pending = 0
            repeat(BUFFERS) { refill(AVAudioPCMBuffer(pCMFormat = format, frameCapacity = FRAMES.toUInt()), id) }
            n.play()
        }
        nowPlaying()
    }

    override fun stop() {
        // weich ausblenden (350 ms), danach raeumt finish() auf
        renderer?.stop()
    }

    override fun describe(text: String) {
        this.text = text
        if (playing) nowPlaying()
    }

    override fun release() {
        hardStop()
        val center = NSNotificationCenter.defaultCenter
        observers.forEach { center.removeObserver(it) }
        observers = emptyList()
    }

    // Puffer berechnen und anhaengen (Audio-Queue); nach dem letzten (gestoppt/Timer) keine neuen mehr
    private fun refill(buf: AVAudioPCMBuffer, id: Int) {
        val r = renderer ?: return
        val n = node ?: return
        if (id != session || done) return
        val finished = r.render(left, right, FRAMES)
        val ch = buf.floatChannelData ?: return
        val l = ch[0] ?: return
        val rr = ch[1] ?: return
        for (i in 0 until FRAMES) { l[i] = left[i]; rr[i] = right[i] }
        buf.frameLength = FRAMES.toUInt()
        if (finished) done = true
        pending++
        n.scheduleBuffer(buf) {
            dispatch_async(queue) {
                if (id != session) return@dispatch_async
                pending--
                if (!done) refill(buf, id)
                else if (pending == 0) dispatch_async(dispatch_get_main_queue()) { if (id == session) hardStop() }
            }
        }
    }

    private fun hardStop() {
        session++
        node?.stop()
        engine?.stop()
        node = null; engine = null
        if (playing) {
            playing = false
            AVAudioSession.sharedInstance().setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
        }
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun onInterruption(n: NSNotification?) {
        val type = (n?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)?.unsignedLongValue ?: return
        if (type == INTERRUPTION_BEGAN) hardStop()
    }

    private fun onRouteChange(n: NSNotification?) {
        val reason = (n?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)?.unsignedLongValue ?: return
        if (reason == ROUTE_OLD_DEVICE_UNAVAILABLE && playing) stop()
    }

    // Titel und Mischung am Sperrbildschirm, dort auch Stopp bzw. Start
    private fun nowPlaying() {
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to APP_NAME,
            MPMediaItemPropertyArtist to text,
            MPNowPlayingInfoPropertyPlaybackRate to 1.0,
        )
    }

    private fun remoteCommands() {
        val c = MPRemoteCommandCenter.sharedCommandCenter()
        val ok = MPRemoteCommandHandlerStatusSuccess
        c.pauseCommand.addTargetWithHandler { _ -> stop(); ok }
        c.stopCommand.addTargetWithHandler { _ -> stop(); ok }
        c.playCommand.addTargetWithHandler { _ -> play(); ok }
        c.togglePlayPauseCommand.addTargetWithHandler { _ -> if (playing) stop() else play(); ok }
    }

    private companion object {
        const val FRAMES = 4096
        const val BUFFERS = 4
        const val INTERRUPTION_BEGAN = 1uL          // AVAudioSessionInterruptionTypeBegan
        const val ROUTE_OLD_DEVICE_UNAVAILABLE = 2uL // AVAudioSessionRouteChangeReasonOldDeviceUnavailable
    }
}
