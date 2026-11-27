package solutions.xapient.hertzr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import solutions.xapient.hertzr.domain.Params
import solutions.xapient.hertzr.domain.ToneRenderer
import kotlin.concurrent.Volatile

/**
 * Ton ueber AVAudioEngine: ein Player-Knoten bekommt reihum vier kurze Puffer (je 512 Frames, zusammen rund 43 ms),
 * jeder wird nach dem Abspielen auf einer eigenen Queue neu berechnet. Wie auf Android rechnet der Renderer live,
 * Aenderungen (Frequenz, Pegel, Form) kommen als Ganzes beim naechsten Puffer an.
 */
@OptIn(ExperimentalForeignApi::class)
class IosToneEngine : Player {
    // pro Start ein eigener Durchgang; stop() beendet nur den eigenen
    private class Session(val engine: AVAudioEngine, val node: AVAudioPlayerNode, val renderer: ToneRenderer) {
        @Volatile var running = true
        @Volatile var pending: Params? = null
        @Volatile var hz = Double.NaN
        @Volatile var progress = -1f
        var queued = 0 // nur auf der Audio-Queue
        var ended = false // nur auf der Audio-Queue
    }

    @Volatile private var session: Session? = null
    override var onEnd: (() -> Unit)? = null
    private val format = AVAudioFormat(standardFormatWithSampleRate = RATE.toDouble(), channels = 2u)
    private val queue = dispatch_queue_create("hertzr.audio", null)
    private val block = FloatArray(FRAMES * 2)

    override fun start(p: Params): Boolean {
        stop()
        val audio = AVAudioSession.sharedInstance()
        audio.setCategory(AVAudioSessionCategoryPlayback, null)
        if (!audio.setActive(true, null)) return false
        val e = AVAudioEngine()
        val n = AVAudioPlayerNode()
        e.attachNode(n)
        e.connect(n, e.mainMixerNode, format)
        e.prepare()
        if (!e.startAndReturnError(null)) {
            audio.setActive(false, null)
            return false
        }
        val run = Session(e, n, ToneRenderer(RATE, p))
        session = run
        dispatch_async(queue) {
            repeat(BUFFERS) { fill(run, AVAudioPCMBuffer(pCMFormat = format, frameCapacity = FRAMES.toUInt())) }
            n.play()
        }
        return true
    }

    override fun update(p: Params) {
        session?.pending = p
    }

    // blendet weich aus (40 ms), danach raeumt finish() auf
    override fun stop() {
        session?.let { it.running = false; session = null }
    }

    override fun liveHz(): Double? = session?.hz?.takeIf { !it.isNaN() }

    override fun progress(): Float? = session?.progress?.takeIf { it >= 0f }

    // Block berechnen und anhaengen (Audio-Queue); nach dem Ausblenden keine neuen mehr
    private fun fill(run: Session, buf: AVAudioPCMBuffer) {
        val r = run.renderer
        if (r.done) {
            if (run.queued == 0) finish(run)
            return
        }
        if (!run.running) r.release()
        run.pending?.let { run.pending = null; r.apply(it) }
        r.render(block, FRAMES)
        run.hz = r.currentHz
        run.progress = r.progress ?: -1f
        val ch = buf.floatChannelData ?: return
        val l = ch[0] ?: return
        val rr = ch[1] ?: return
        for (i in 0 until FRAMES) { l[i] = block[2 * i]; rr[i] = block[2 * i + 1] }
        buf.frameLength = FRAMES.toUInt()
        run.queued++
        run.node.scheduleBuffer(buf) {
            dispatch_async(queue) {
                run.queued--
                fill(run, buf)
            }
        }
    }

    // Durchgang zu Ende (Stopp oder Sweep fertig): Engine weg, Sitzung nur frei, wenn nichts Neues laeuft
    private fun finish(run: Session) {
        if (run.ended) return
        run.ended = true
        dispatch_async(dispatch_get_main_queue()) {
            run.node.stop()
            run.engine.stop()
            val sweepDone = run.running
            if (session === run) session = null
            if (session == null) AVAudioSession.sharedInstance().setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
            if (sweepDone) onEnd?.invoke()
        }
    }

    companion object {
        // feste Rate; der Mixer passt an die Hardware an
        const val RATE = 48_000
        const val FRAMES = 512
        const val BUFFERS = 4
    }
}
