package solutions.xapient.magnr.sensor

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import solutions.xapient.magnr.domain.BeepSynth
import solutions.xapient.magnr.domain.Beeper

/**
 * Pieptoene ueber einen AudioTrack-Stream auf eigenem Thread; braucht keine Berechtigung.
 * Hoechstens ein Thread (und damit ein AudioTrack): stop blockiert nicht, start waehrend des
 * Ausklingens laesst denselben Thread weiterspielen.
 */
class AudioBeeper : Beeper {
    @Volatile override var strength = 0f
    private val lock = Any()
    // beide nur unter lock geaendert
    @Volatile private var running = false
    private var thread: Thread? = null

    override fun start() = synchronized(lock) {
        running = true
        if (thread == null) thread = Thread(::run, "magnr-beep").apply { start() }
    }

    override fun stop() = synchronized(lock) {
        running = false
    }

    // spielt, bis gestoppt; wurde beim Ausklingen neu gestartet, gleich weiter
    private fun run() {
        while (true) {
            val ok = play()
            synchronized(lock) {
                // Fehler: aufgeben statt in einer Schleife neu zu versuchen
                if (!ok) running = false
                if (!running) {
                    thread = null
                    return
                }
            }
        }
    }

    // false = kein Audio moeglich
    private fun play(): Boolean {
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return false
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
                        .setSampleRate(RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minBuf, BLOCK * 2 * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } catch (_: Exception) {
            return false
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return false
        }
        val synth = BeepSynth(RATE)
        val block = ShortArray(BLOCK)
        return try {
            track.play()
            var ok = true
            while (running && ok) {
                synth.render(block, block.size, strength)
                ok = track.write(block, 0, block.size) >= 0
            }
            // sanft ausklingen statt Knacks
            synth.render(block, block.size, 0f)
            track.write(block, 0, block.size)
            ok
        } catch (_: Exception) {
            // Audio weg (z. B. Geraet getrennt): still bleiben
            false
        } finally {
            try {
                track.stop()
            } catch (_: IllegalStateException) {
            }
            track.release()
        }
    }

    private companion object {
        const val RATE = 48_000
        // 10 ms je Block
        const val BLOCK = RATE / 100
    }
}
