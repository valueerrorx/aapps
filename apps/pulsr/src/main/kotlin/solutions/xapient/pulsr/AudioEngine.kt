package solutions.xapient.pulsr

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.atomic.AtomicReference
import solutions.xapient.pulsr.domain.BeatClock
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.domain.Renderer
import solutions.xapient.pulsr.domain.Settings

// Ein durchgehender AudioTrack (MODE_STREAM) auf eigenem Thread; Klicks sample-genau im Puffer, kein Timer.
// Bild und Ton: die Oberflaeche fragt die Uhr mit dem hoerbaren Frame aus getTimestamp.
class AudioEngine : Player {
    private class Stamp(val frame: Long, val nanos: Long)

    // Laeuft pro Start ein eigener Durchgang; stop() beendet nur den eigenen
    private class Session(val track: AudioTrack, val renderer: Renderer, val clock: BeatClock, val rate: Int) {
        @Volatile var running = true
        val pending = AtomicReference<Settings?>()
        @Volatile var stamp: Stamp? = null
    }

    @Volatile private var session: Session? = null
    override var onFailure: (() -> Unit)? = null

    override fun start(s: Settings): Boolean {
        stop()
        val rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf { it in 8_000..192_000 } ?: 48_000
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return false
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                // kleiner Puffer: Tempowechsel sind schnell hoerbar, zwei Bloecke Reserve gegen Aussetzer
                .setBufferSizeInBytes(maxOf(min, blockFrames(rate) * 2 * 4))
                .build()
        } catch (_: Exception) {
            return false
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return false
        }
        val clock = BeatClock(rate)
        // Klicks aller Klaenge werden hier vorab erzeugt, nicht im Lauf
        val run = Session(track, Renderer(rate, s, clock), clock, rate)
        session = run
        Thread({ loop(run) }, "pulsr-audio").start()
        return true
    }

    override fun update(s: Settings) {
        session?.pending?.set(s)
    }

    override fun stop() {
        session?.running = false
        session = null
    }

    override fun pulse(nowNanos: Long): Pulse? {
        val run = session ?: return null
        val st = run.stamp ?: return null
        return run.clock.at(BeatClock.presented(st.frame, st.nanos, nowNanos, run.rate))
    }

    private fun loop(run: Session) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (_: Exception) {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Exception) {
                // Standard-Prioritaet; der Puffer reicht meist trotzdem
            }
        }
        val t = run.track
        val block = blockFrames(run.rate)
        val buf = FloatArray(block)
        val pcm = ShortArray(block)
        val ts = AudioTimestamp()
        var lastStamp = 0L
        var failed = false
        try {
            t.play()
            while (run.running) {
                run.pending.getAndSet(null)?.let { run.renderer.apply(it) }
                run.renderer.render(buf, block)
                Renderer.toPcm(buf, pcm, block)
                var off = 0
                while (off < block && run.running) {
                    val w = t.write(pcm, off, block - off)
                    if (w < 0) throw IllegalStateException("write $w")
                    off += w
                }
                // Zeitstempel der Ausgabe selten abfragen, dazwischen rechnet die Oberflaeche weiter
                val now = System.nanoTime()
                if (now - lastStamp > STAMP_NS || run.stamp == null) {
                    lastStamp = now
                    run.stamp = if (t.getTimestamp(ts)) {
                        Stamp(ts.framePosition, ts.nanoTime)
                    } else {
                        // vor dem ersten Zeitstempel: Abspielposition ohne Latenzausgleich
                        Stamp(t.playbackHeadPosition.toLong() and 0xFFFFFFFFL, now)
                    }
                }
            }
        } catch (_: Exception) {
            failed = run.running
        } finally {
            try {
                t.pause()
                t.flush()
            } catch (_: IllegalStateException) {
                // schon beendet
            }
            t.release()
            run.clock.clear()
        }
        if (failed) {
            run.running = false
            if (session === run) session = null
            onFailure?.invoke()
        }
    }

    private companion object {
        const val STAMP_NS = 250_000_000L
        // ca. 5 ms je Block
        fun blockFrames(rate: Int) = rate / 200
    }
}
