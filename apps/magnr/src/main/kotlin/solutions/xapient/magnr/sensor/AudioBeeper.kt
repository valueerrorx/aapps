package solutions.xapient.magnr.sensor

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import solutions.xapient.magnr.domain.BeepSynth
import solutions.xapient.magnr.domain.Beeper

// Pieptoene ueber einen AudioTrack-Stream auf eigenem Thread; braucht keine Berechtigung
class AudioBeeper : Beeper {
    @Volatile override var strength = 0f
    @Volatile private var running = false
    private var thread: Thread? = null

    override fun start() {
        if (thread != null) return
        running = true
        thread = Thread(::loop, "magnr-beep").apply { start() }
    }

    override fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun loop() {
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return
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
            return
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            return
        }
        val synth = BeepSynth(RATE)
        val block = ShortArray(BLOCK)
        try {
            track.play()
            while (running) {
                synth.render(block, block.size, strength)
                if (track.write(block, 0, block.size) < 0) break
            }
            // sanft ausklingen statt Knacks
            synth.render(block, block.size, 0f)
            track.write(block, 0, block.size)
        } catch (_: Exception) {
            // Audio weg (z. B. Geraet getrennt): still bleiben
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
