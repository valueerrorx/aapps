package solutions.xapient.hertzr

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.atomic.AtomicReference
import solutions.xapient.hertzr.domain.Params
import solutions.xapient.hertzr.domain.ToneRenderer

// Ein durchgehender AudioTrack (Stream, Float, Stereo) auf eigenem Thread; der Renderer rechnet jeden Block live.
class AudioEngine : Player {
    // pro Start ein eigener Durchgang; stop() beendet nur den eigenen
    private class Session(var track: AudioTrack, val renderer: ToneRenderer, val rate: Int) {
        @Volatile var running = true
        val pending = AtomicReference<Params?>()
        @Volatile var hz = Double.NaN
        @Volatile var progress = -1f
    }

    private val session = AtomicReference<Session?>()
    override var onEnd: (() -> Unit)? = null

    override fun start(p: Params): Boolean {
        stop()
        val rate = nativeRate()
        val track = newTrack(rate) ?: return false
        val run = Session(track, ToneRenderer(rate, p), rate)
        session.set(run)
        Thread({ loop(run) }, "hertzr-audio").start()
        return true
    }

    override fun update(p: Params) {
        session.get()?.pending?.set(p)
    }

    override fun stop() {
        session.getAndSet(null)?.running = false
    }

    override fun liveHz(): Double? = session.get()?.hz?.takeIf { !it.isNaN() }

    override fun progress(): Float? = session.get()?.progress?.takeIf { it >= 0f }

    private fun newTrack(rate: Int): AudioTrack? {
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        if (min <= 0) return null
        // Float Stereo: 8 Byte je Frame
        val minFrames = min / 8
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setBufferSizeInBytes(maxOf(min, rate * MAX_BUFFER_MS / 1_000 * 8))
                .build()
        } catch (_: Exception) {
            return null
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return null
        }
        // 4 Bloecke (~20 ms) Reserve, mindestens das Minimum der Plattform
        track.bufferSizeInFrames = maxOf(minFrames, blockFrames(rate) * 4)
        return track
    }

    private fun loop(run: Session) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (_: Exception) {
            // Standard-Prioritaet; der Puffer reicht meist trotzdem
        }
        val block = blockFrames(run.rate)
        val buf = FloatArray(block * 2)
        var underruns = 0
        var revived = false
        var ok = true
        try {
            run.track.play()
            // laeuft bis Stopp oder bis der Sweep ausgeblendet ist
            while (!run.renderer.done) {
                if (!run.running) run.renderer.release()
                run.pending.getAndSet(null)?.let { run.renderer.apply(it) }
                run.renderer.render(buf, block)
                run.hz = run.renderer.currentHz
                run.progress = run.renderer.progress ?: -1f
                val t = run.track
                if (!write(t, buf)) {
                    // Ausgabe weg (z. B. ERROR_DEAD_OBJECT): einmal mit neuem Track weiter, dann aufgeben
                    if (revived || !run.running) throw IllegalStateException("write")
                    revived = true
                    val fresh = newTrack(run.rate) ?: throw IllegalStateException("track")
                    t.release()
                    run.track = fresh
                    underruns = 0
                    fresh.play()
                    continue
                }
                // Aussetzer: Puffer schrittweise vergroessern, hoechstens bis zur Kapazitaet
                val u = t.underrunCount
                if (u > underruns) {
                    underruns = u
                    val grown = minOf(t.bufferSizeInFrames + block * 2, t.bufferCapacityInFrames)
                    if (grown > t.bufferSizeInFrames) t.bufferSizeInFrames = grown
                }
            }
            // Ausgeblendetes zu Ende spielen; release() vorher wuerde es abschneiden
            run.track.stop()
            Thread.sleep(run.track.bufferSizeInFrames * 1_000L / run.rate + 20)
        } catch (_: Exception) {
            ok = false
        } finally {
            try {
                run.track.release()
            } catch (_: Exception) {
                // schon freigegeben
            }
        }
        // Sweep fertig oder Ausgabe weg: nur melden, wenn dieser Durchgang noch der aktuelle ist
        if ((run.running || !ok) && session.compareAndSet(run, null)) {
            run.running = false
            onEnd?.invoke()
        }
    }

    // ganzer Block oder false bei Schreibfehler
    private fun write(t: AudioTrack, buf: FloatArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val w = t.write(buf, off, buf.size - off, AudioTrack.WRITE_BLOCKING)
            if (w <= 0) return false
            off += w
        }
        return true
    }

    companion object {
        // Deckel fuer das Vergroessern bei Aussetzern
        private const val MAX_BUFFER_MS = 80

        // ca. 5 ms je Block
        private fun blockFrames(rate: Int) = rate / 200

        fun nativeRate(): Int =
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf { it in 8_000..192_000 } ?: 48_000
    }
}
