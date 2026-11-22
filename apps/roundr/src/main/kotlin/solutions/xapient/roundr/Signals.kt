package solutions.xapient.roundr

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import solutions.xapient.roundr.domain.Cue
import solutions.xapient.roundr.domain.Phase
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// Toene und Vibration je Ansage; Toene laufen ueber die Medienlautstaerke und mischen sich in Musik
class Signals(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    private val tracks = HashMap<String, AudioTrack?>()

    fun play(cue: Cue, sound: Boolean, vibrate: Boolean) {
        if (sound) tone(cue)
        if (vibrate) buzz(cue)
    }

    private fun tone(cue: Cue) {
        val key = when (cue) {
            is Cue.Count -> "count"
            is Cue.Start -> if (cue.phase == Phase.WORK) "work" else "rest"
            Cue.Done -> "done"
        }
        val t = tracks.getOrPut(key) { build(notes(key)) } ?: return
        try {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.play()
        } catch (_: IllegalStateException) {
            // Audio gerade nicht verfuegbar: Ansage faellt aus, Timer laeuft weiter
        }
    }

    // Toene als (Frequenz Hz, Dauer ms); 0 Hz = Stille
    private fun notes(key: String): List<Pair<Double, Int>> = when (key) {
        "count" -> listOf(880.0 to 110)
        "work" -> listOf(1318.5 to 420)
        "rest" -> listOf(659.3 to 160, 0.0 to 50, 523.3 to 300)
        else -> listOf(784.0 to 140, 0.0 to 30, 1046.5 to 140, 0.0 to 30, 1568.0 to 420)
    }

    private fun build(notes: List<Pair<Double, Int>>): AudioTrack? {
        val rate = 44_100
        val total = notes.sumOf { it.second } * rate / 1000
        val pcm = ShortArray(total)
        var at = 0
        for ((hz, ms) in notes) {
            val n = ms * rate / 1000
            // weiche Flanken gegen Knacksen
            val fade = min(n / 4, rate * 8 / 1000)
            for (i in 0 until n) {
                if (hz <= 0) break
                val env = when {
                    i < fade -> i.toDouble() / fade
                    i > n - fade -> (n - i).toDouble() / fade
                    else -> 1.0
                }
                // Grundton plus leise Oktave klingt runder als reiner Sinus
                val x = 2 * PI * hz * i / rate
                pcm[at + i] = ((sin(x) * 0.8 + sin(2 * x) * 0.2) * env * 0.7 * Short.MAX_VALUE).toInt().toShort()
            }
            at += n
        }
        return try {
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
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
                .also { it.write(pcm, 0, pcm.size) }
        } catch (_: Exception) {
            null
        }
    }

    private fun buzz(cue: Cue) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val pattern = when (cue) {
            is Cue.Count -> longArrayOf(0, 40)
            is Cue.Start -> if (cue.phase == Phase.WORK) longArrayOf(0, 380) else longArrayOf(0, 120, 90, 120)
            Cue.Done -> longArrayOf(0, 160, 80, 160, 80, 480)
        }
        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun release() {
        tracks.values.forEach { it?.release() }
        tracks.clear()
    }
}
