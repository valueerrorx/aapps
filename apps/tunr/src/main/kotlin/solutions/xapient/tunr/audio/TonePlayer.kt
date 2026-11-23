package solutions.xapient.tunr.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import solutions.xapient.tunr.domain.Pluck
import solutions.xapient.tunr.domain.ToneMixer

// Referenzton; im Test ersetzt. Aufrufe vom Main-Thread
interface TonePlayer {
    fun play(hz: Float)
    fun stop()
}

// AudioTrack (Medien) auf eigenem Thread; Synthese dort, die UI wartet nie
class AudioTrackTone : TonePlayer {
    private val lock = Any()
    private var pending = 0f
    private var running = false
    // zaehlt stop(); ein waehrend der Synthese gestoppter Ton startet nicht mehr
    private var gen = 0
    // erst auf dem Audio-Thread bestimmt (Geraete-Rate)
    private var rate = FALLBACK_RATE
    private var mixer: ToneMixer? = null

    override fun play(hz: Float) {
        synchronized(lock) {
            pending = hz
            if (running) return
            running = true
        }
        Thread(::loop, "tunr-tone").start()
    }

    // schnell ausblenden; der Thread endet danach von selbst
    override fun stop() {
        synchronized(lock) {
            pending = 0f
            gen++
            mixer?.release()
        }
    }

    private fun loop() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        } catch (_: Exception) {
            // Standard-Prioritaet reicht
        }
        val native = try {
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        } catch (_: Exception) {
            0
        }
        rate = if (native in 8_000..192_000) native else FALLBACK_RATE
        val mixer = synchronized(lock) { mixer?.takeIf { it.rate == rate } ?: ToneMixer(rate).also { this.mixer = it } }
        val chunk = rate / 100
        val track = open(chunk)
        if (track == null) {
            synchronized(lock) { running = false; pending = 0f; mixer.release() }
            return
        }
        val buf = FloatArray(chunk)
        try {
            track.play()
            while (true) {
                val (hz, g) = synchronized(lock) { (pending to gen).also { pending = 0f } }
                // Synthese ausserhalb der Sperre (wenige ms)
                if (hz > 0f) {
                    val x = Pluck.render(hz.toDouble(), rate)
                    synchronized(lock) { if (g == gen) mixer.trigger(x) }
                }
                val done = synchronized(lock) {
                    if (mixer.idle && pending <= 0f) {
                        running = false
                        true
                    } else {
                        mixer.render(buf)
                        false
                    }
                }
                if (done) break
                if (track.write(buf, 0, chunk, AudioTrack.WRITE_BLOCKING) < 0) break
            }
            // Rest im Puffer ausspielen lassen
            track.stop()
        } catch (_: Exception) {
            // Ausgabe weg: Ton faellt aus, Stimmen geht weiter
        } finally {
            track.release()
            synchronized(lock) {
                if (running) {
                    running = false
                    pending = 0f
                    mixer.release()
                }
            }
        }
    }

    private fun open(chunk: Int): AudioTrack? = try {
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(min, chunk * 4 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .let { t ->
                if (t.state == AudioTrack.STATE_INITIALIZED) {
                    t
                } else {
                    t.release()
                    null
                }
            }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val FALLBACK_RATE = 48_000
    }
}
