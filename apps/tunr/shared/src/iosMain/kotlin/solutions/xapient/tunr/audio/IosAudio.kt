package solutions.xapient.tunr.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Foundation.NSLock
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import solutions.xapient.tunr.domain.Frame
import solutions.xapient.tunr.domain.PitchDetector
import solutions.xapient.tunr.domain.Pluck
import solutions.xapient.tunr.domain.SlidingWindow
import solutions.xapient.tunr.domain.ToneMixer
import kotlin.concurrent.Volatile

// Gemeinsame Audio-Sitzung: Aufnahme und Referenzton zugleich, Modus Measurement (ohne automatische Verstaerkung),
// Ton ueber den Lautsprecher statt die Hoermuschel
@OptIn(ExperimentalForeignApi::class)
internal object TunrSession {
    // Mikrofon (Main-Thread) und Ton (eigene Queue) teilen die Sitzung
    private val lock = NSLock()
    private var users = 0

    fun acquire(): Boolean {
        lock.lock()
        try {
            val s = AVAudioSession.sharedInstance()
            if (users == 0) {
                if (!s.setCategory(AVAudioSessionCategoryPlayAndRecord, AVAudioSessionModeMeasurement, DEFAULT_TO_SPEAKER or ALLOW_BLUETOOTH_A2DP, null)) return false
                if (!s.setActive(true, null)) return false
            }
            users++
            return true
        } finally {
            lock.unlock()
        }
    }

    fun release() {
        lock.lock()
        try {
            if (users == 0) return
            if (--users == 0) AVAudioSession.sharedInstance().setActive(false, null)
        } finally {
            lock.unlock()
        }
    }

    private const val DEFAULT_TO_SPEAKER = 0x8uL     // AVAudioSessionCategoryOptionDefaultToSpeaker
    private const val ALLOW_BLUETOOTH_A2DP = 0x20uL  // AVAudioSessionCategoryOptionAllowBluetoothA2DP
}

/**
 * Mikrofon ueber den Eingangs-Tap von AVAudioEngine. Analyse wie auf Android in 25-ms-Schritten auf dem Tap-Thread,
 * nur fertige Frames gehen auf den Main-Thread. Unterbrechung (Anruf) = Fehler, die Oberflaeche bietet "Erneut".
 */
@OptIn(ExperimentalForeignApi::class)
class IosMic : Mic {
    private class Session(val engine: AVAudioEngine) {
        @Volatile var running = true
    }

    private var session: Session? = null
    private var onError: (() -> Unit)? = null

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) { _ ->
            if (session != null) {
                stop()
                onError?.invoke()
            }
        }
    }

    override fun start(sink: (Frame) -> Unit, onError: () -> Unit): Boolean {
        stop()
        if (!granted()) return false
        if (!TunrSession.acquire()) return false
        val e = AVAudioEngine()
        val input = e.inputNode
        val format = input.outputFormatForBus(0u)
        val rate = format.sampleRate.toInt()
        if (rate <= 0 || format.channelCount == 0u) {
            TunrSession.release()
            return false
        }
        val run = Session(e)
        val det = PitchDetector(rate)
        val hop = rate / 40
        val win = SlidingWindow(det.size, hop)
        val pcm = ShortArray(hop)
        var fill = 0
        val deadAfter = 2L * rate
        input.installTapOnBus(0u, (hop * 2).toUInt(), format) { buf, _ ->
            val b = buf ?: return@installTapOnBus
            val ch = b.floatChannelData?.get(0) ?: return@installTapOnBus
            val n = b.frameLength.toInt()
            for (i in 0 until n) {
                if (!run.running) return@installTapOnBus
                pcm[fill++] = (ch[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                if (fill < hop) continue
                fill = 0
                win.push(pcm)
                val db = win.levelDb(det.window)
                val p = if (db >= LEVEL_FLOOR) det.detect(win.data) else null
                val f = Frame(p?.hz ?: 0f, db, dead = win.zeroRun >= deadAfter)
                dispatch_async(dispatch_get_main_queue()) { if (session === run) sink(f) }
            }
        }
        e.prepare()
        if (!e.startAndReturnError(null)) {
            input.removeTapOnBus(0u)
            TunrSession.release()
            return false
        }
        session = run
        this.onError = onError
        return true
    }

    override fun stop() {
        val run = session ?: return
        session = null
        run.running = false
        run.engine.inputNode.removeTapOnBus(0u)
        run.engine.stop()
        TunrSession.release()
    }

    companion object {
        // darunter lohnt keine Analyse (Pegel-Tor liegt hoeher)
        private const val LEVEL_FLOOR = -70f

        fun granted(): Boolean = AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionGranted
    }
}

/**
 * Referenzton ueber einen Player-Knoten: reihum vier Puffer zu 10 ms, berechnet auf einer eigenen Queue.
 * Die Saiten-Synthese laeuft dort ausserhalb des Main-Threads wie auf Android.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTone : TonePlayer {
    private val queue = dispatch_queue_create("tunr.tone", null)
    private val format = AVAudioFormat(standardFormatWithSampleRate = RATE.toDouble(), channels = 1u)
    private val mixer = ToneMixer(RATE)
    private val chunk = FloatArray(CHUNK)
    // nur auf der Queue
    private var engine: AVAudioEngine? = null
    private var node: AVAudioPlayerNode? = null
    private var queued = 0
    private var gen = 0

    override fun play(hz: Float) {
        dispatch_async(queue) {
            mixer.trigger(Pluck.render(hz.toDouble(), RATE))
            if (engine == null && !open()) {
                mixer.release()
                return@dispatch_async
            }
            repeat(BUFFERS - queued) { schedule(AVAudioPCMBuffer(pCMFormat = format, frameCapacity = CHUNK.toUInt()), gen) }
        }
    }

    // schnell ausblenden; danach schliesst sich die Engine von selbst
    override fun stop() {
        dispatch_async(queue) { mixer.release() }
    }

    private fun open(): Boolean {
        if (!TunrSession.acquire()) return false
        val e = AVAudioEngine()
        val n = AVAudioPlayerNode()
        e.attachNode(n)
        e.connect(n, e.mainMixerNode, format)
        e.prepare()
        if (!e.startAndReturnError(null)) {
            TunrSession.release()
            return false
        }
        n.play()
        engine = e
        node = n
        gen++
        return true
    }

    private fun schedule(buf: AVAudioPCMBuffer, g: Int) {
        val n = node ?: return
        if (g != gen) return
        if (mixer.idle) {
            if (queued == 0) close()
            return
        }
        mixer.render(chunk)
        val out = buf.floatChannelData?.get(0) ?: return
        for (i in 0 until CHUNK) out[i] = chunk[i]
        buf.frameLength = CHUNK.toUInt()
        queued++
        n.scheduleBuffer(buf) {
            dispatch_async(queue) {
                if (g != gen) return@dispatch_async
                queued--
                schedule(buf, g)
            }
        }
    }

    private fun close() {
        gen++
        node?.stop()
        engine?.stop()
        node = null
        engine = null
        queued = 0
        TunrSession.release()
    }

    private companion object {
        const val RATE = 48_000
        const val CHUNK = 480
        const val BUFFERS = 4
    }
}
