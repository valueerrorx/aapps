package solutions.xapient.pulsr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEngineConfigurationChangeNotification
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioTime
import platform.AVFAudio.setActive
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_time
import solutions.xapient.pulsr.domain.BeatClock
import solutions.xapient.pulsr.domain.Lock
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.domain.Renderer
import solutions.xapient.pulsr.domain.Settings

// AVAudioEngine + Player-Knoten: Bloecke werden auf eigener Queue gerechnet und sample-genau
// (atTime = Renderer-Frame) eingereiht, immer QUEUED Bloecke voraus; kein Kotlin im Echtzeit-Thread.
// Bild und Ton: Puls am hoerbaren Frame aus lastRenderTime + Ausgabelatenz (wie getTimestamp auf Android).
@OptIn(ExperimentalForeignApi::class)
class IosAudioEngine : Player {
    private class Session(
        val engine: AVAudioEngine,
        val node: AVAudioPlayerNode,
        val format: AVAudioFormat,
        val renderer: Renderer,
        val clock: BeatClock,
        val rate: Int,
    ) {
        // nur auf der Audio-Queue gelesen/geschrieben
        var running = true
        val buf = FloatArray(BLOCK)
        val lock = Lock()
        var pending: Settings? = null
        val observers = ArrayList<NSObjectProtocol>()
    }

    private val queue = dispatch_queue_create("pulsr.audio", null)
    private var session: Session? = null
    override var onFailure: (() -> Unit)? = null

    override fun start(s: Settings): Boolean {
        stop()
        val audio = AVAudioSession.sharedInstance()
        // Playback: spielt bei gesperrtem Bildschirm weiter (UIBackgroundModes audio); mischt sich zu anderer Musik
        audio.setCategory(AVAudioSessionCategoryPlayback, withOptions = AVAudioSessionCategoryOptionMixWithOthers, error = null)
        audio.setActive(true, null)
        val engine = AVAudioEngine()
        val node = AVAudioPlayerNode()
        engine.attachNode(node)
        val out = engine.outputNode.outputFormatForBus(0u).sampleRate.toInt()
        val rate = out.takeIf { it in 8_000..192_000 } ?: 48_000
        val format = AVAudioFormat(standardFormatWithSampleRate = rate.toDouble(), channels = 1u)
        engine.connect(node, to = engine.mainMixerNode, format = format)
        engine.prepare()
        if (!engine.startAndReturnError(null)) return false
        val clock = BeatClock(rate)
        // Vorlauf: die Ausgabe schwingt ein, die erste Eins kommt erst danach
        val run = Session(engine, node, format, Renderer(rate, s, clock, leadIn = rate * LEAD_IN_MS / 1_000), clock, rate)
        session = run
        watch(run)
        dispatch_async(queue) {
            repeat(QUEUED) { schedule(run) }
            run.node.play()
        }
        return true
    }

    override fun update(s: Settings) {
        val run = session ?: return
        run.lock.locked { run.pending = s }
    }

    override fun stop() {
        val run = session ?: return
        session = null
        end(run)
    }

    override fun pulse(nowNanos: Long): Pulse? {
        val run = session ?: return null
        val t = run.node.lastRenderTime ?: return null
        if (!t.sampleTimeValid || !t.hostTimeValid) return null
        val pt = run.node.playerTimeForNodeTime(t) ?: return null
        if (!pt.sampleTimeValid) return null
        val audio = AVAudioSession.sharedInstance()
        val heard = AVAudioTime.secondsForHostTime(t.hostTime) + audio.outputLatency + audio.IOBufferDuration
        return run.clock.at(BeatClock.presented(pt.sampleTime, (heard * 1e9).toLong(), nowNanos, run.rate))
    }

    // naechsten Block rechnen und genau an seinem Frame einreihen; Fertig-Meldung rechnet den uebernaechsten
    private fun schedule(run: Session) {
        if (!run.running) return
        run.lock.locked { run.pending.also { run.pending = null } }?.let { run.renderer.apply(it) }
        val at = run.renderer.frame
        run.renderer.render(run.buf, BLOCK)
        enqueue(run, at) { dispatch_async(queue) { schedule(run) } }
    }

    private fun enqueue(run: Session, at: Long, done: () -> Unit) {
        val pcm = AVAudioPCMBuffer(pCMFormat = run.format, frameCapacity = BLOCK.toUInt()) ?: return
        pcm.frameLength = BLOCK.toUInt()
        val ch = pcm.floatChannelData?.get(0) ?: return
        for (i in 0 until BLOCK) ch[i] = run.buf[i]
        run.node.scheduleBuffer(pcm, atTime = AVAudioTime(sampleTime = at, atRate = run.rate.toDouble()), options = 0uL, completionHandler = done)
    }

    // Stopp ohne Knacksen: keine neuen Bloecke, ein Ausblend-Block, Rest abspielen lassen, dann aus
    private fun end(run: Session) {
        unwatch(run)
        dispatch_async(queue) {
            if (!run.running) return@dispatch_async
            run.running = false
            val at = run.renderer.frame
            run.renderer.fadeOut(run.buf, BLOCK)
            enqueue(run, at) {}
            val tail = (QUEUED + 2) * BLOCK * 1_000_000_000L / run.rate
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, tail), dispatch_get_main_queue()) {
                run.node.stop()
                run.engine.stop()
                run.clock.clear()
            }
        }
    }

    // Ausgabe weg: Metronom aus und melden (Store setzt „spielt nicht")
    private fun fail(run: Session) {
        if (session !== run) return
        session = null
        end(run)
        onFailure?.invoke()
    }

    // Anruf/Siri (Unterbrechung), Kopfhoerer ab (nicht ploetzlich laut aus dem Lautsprecher), Ausgabe neu konfiguriert
    private fun watch(run: Session) {
        val center = NSNotificationCenter.defaultCenter
        val main = NSOperationQueue.mainQueue
        run.observers += center.addObserverForName(AVAudioSessionInterruptionNotification, null, main) { n: NSNotification? ->
            if (number(n, AVAudioSessionInterruptionTypeKey) == INTERRUPTION_BEGAN) fail(run)
        }
        run.observers += center.addObserverForName(AVAudioSessionRouteChangeNotification, null, main) { n: NSNotification? ->
            if (number(n, AVAudioSessionRouteChangeReasonKey) == ROUTE_OLD_DEVICE_UNAVAILABLE) fail(run)
        }
        run.observers += center.addObserverForName(AVAudioEngineConfigurationChangeNotification, run.engine, main) { _: NSNotification? ->
            fail(run)
        }
    }

    private fun unwatch(run: Session) {
        run.observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        run.observers.clear()
    }

    private fun number(n: NSNotification?, key: String?): ULong? = (n?.userInfo?.get(key) as? NSNumber)?.unsignedLongValue

    private companion object {
        // ~21 ms je Block bei 48 kHz, vier Bloecke voraus
        const val BLOCK = 1_024
        const val QUEUED = 4
        // Stille vor der ersten Eins
        const val LEAD_IN_MS = 80
        // AVAudioSessionInterruptionTypeBegan, AVAudioSessionRouteChangeReasonOldDeviceUnavailable
        const val INTERRUPTION_BEGAN = 1uL
        const val ROUTE_OLD_DEVICE_UNAVAILABLE = 2uL
    }
}
