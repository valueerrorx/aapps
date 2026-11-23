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
    private class Session(var track: AudioTrack, val renderer: Renderer, val clock: BeatClock, val rate: Int) {
        @Volatile var running = true
        val pending = AtomicReference<Settings?>()
        @Volatile var stamp: Stamp? = null
    }

    @Volatile private var session: Session? = null
    override var onFailure: (() -> Unit)? = null

    override fun start(s: Settings): Boolean {
        stop()
        val rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf { it in 8_000..192_000 } ?: 48_000
        val track = newTrack(rate) ?: return false
        val clock = BeatClock(rate)
        // Vorlauf: der Track blendet beim Start ein, die erste Eins kommt erst danach
        val run = Session(track, Renderer(rate, s, clock, leadIn = rate * LEAD_IN_MS / 1_000), clock, rate)
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

    private fun newTrack(rate: Int): AudioTrack? {
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return null
        val minFrames = min / 2
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
                // Kapazitaet bis zum Deckel; genutzt wird anfangs nur ein kleiner Teil (siehe loop)
                .setBufferSizeInBytes(maxOf(min, rate * MAX_BUFFER_MS / 1_000 * 2))
                .build()
        } catch (_: Exception) {
            return null
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return null
        }
        // Mono 16 bit: 2 Byte je Frame; 4 Bloecke (~20 ms) Reserve, mindestens das Minimum der Plattform
        track.bufferSizeInFrames = maxOf(minFrames, blockFrames(rate) * 4)
        return track
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
        val block = blockFrames(run.rate)
        val buf = FloatArray(block)
        val pcm = ShortArray(block)
        val ts = AudioTimestamp()
        var lastStamp = 0L
        var timed = false
        var underruns = 0
        // Renderer-Frame, an dem der aktuelle Track bei 0 begann (nach einem Neustart > 0)
        var base = 0L
        var revived = false
        var failed = false
        try {
            run.track.play()
            while (run.running) {
                val t = run.track
                run.pending.getAndSet(null)?.let { run.renderer.apply(it) }
                run.renderer.render(buf, block)
                Renderer.toPcm(buf, pcm, block)
                if (!write(t, pcm, block)) {
                    // Ausgabe weg (z. B. ERROR_DEAD_OBJECT): einmal mit neuem Track weiter, dann aufgeben
                    if (revived) throw IllegalStateException("write")
                    revived = true
                    val fresh = newTrack(run.rate) ?: throw IllegalStateException("track")
                    t.release()
                    run.track = fresh
                    base = run.renderer.frame
                    timed = false
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
                // bis zum ersten gueltigen Zeitstempel in jedem Block, danach selten
                val now = System.nanoTime()
                if (!timed || now - lastStamp > STAMP_NS) {
                    lastStamp = now
                    if (t.getTimestamp(ts) && ts.nanoTime > 0 && ts.framePosition > 0) {
                        timed = true
                        run.stamp = Stamp(base + ts.framePosition, ts.nanoTime)
                    } else if (!timed) {
                        // grob: abgespielt minus Puffer als Latenz der Ausgabe
                        val head = t.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                        run.stamp = Stamp(base + head - t.bufferSizeInFrames, now)
                    }
                }
            }
            fadeOut(run, block, buf, pcm)
        } catch (_: Exception) {
            failed = run.running
        } finally {
            try {
                run.track.release()
            } catch (_: Exception) {
                // schon freigegeben
            }
            run.clock.clear()
        }
        if (failed) {
            run.running = false
            if (session === run) session = null
            onFailure?.invoke()
        }
    }

    // ganzer Block oder false bei Schreibfehler; blockiert hoechstens eine Pufferlaenge
    private fun write(t: AudioTrack, pcm: ShortArray, n: Int): Boolean {
        var off = 0
        while (off < n) {
            val w = t.write(pcm, off, n - off)
            if (w <= 0) return false
            off += w
        }
        return true
    }

    // Stopp ohne Knacksen: keine neuen Klicks, ein Ausblend-Block, Rest abspielen lassen, dann freigeben
    private fun fadeOut(run: Session, block: Int, buf: FloatArray, pcm: ShortArray) {
        val t = run.track
        run.renderer.fadeOut(buf, block)
        Renderer.toPcm(buf, pcm, block)
        if (!write(t, pcm, block)) return
        try {
            t.stop()
        } catch (_: IllegalStateException) {
            return
        }
        // stop() spielt im Stream-Modus den Puffer zu Ende; release() vorher wuerde ihn abschneiden
        Thread.sleep(t.bufferSizeInFrames * 1_000L / run.rate + 20)
    }

    private companion object {
        const val STAMP_NS = 250_000_000L
        // Stille vor der ersten Eins
        const val LEAD_IN_MS = 80
        // Deckel fuer das Vergroessern bei Aussetzern
        const val MAX_BUFFER_MS = 80
        // ca. 5 ms je Block
        fun blockFrames(rate: Int) = rate / 200
    }
}
