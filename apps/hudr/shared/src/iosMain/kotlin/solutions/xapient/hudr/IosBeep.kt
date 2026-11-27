package solutions.xapient.hudr

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// Warnton beim Ueberschreiten: Doppelpiep wie Android TONE_PROP_BEEP2; Ambient = Stummschalter gilt, Musik laeuft weiter
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosBeep {
    private val player: AVAudioPlayer? by lazy {
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryAmbient, null)
            setActive(true, null)
        }
        val bytes = wav(doubleBeep())
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        AVAudioPlayer(data = data, error = null).apply { prepareToPlay() }
    }

    fun play() {
        val p = player ?: return
        p.currentTime = 0.0
        p.play()
    }

    private companion object {
        const val RATE = 44_100

        // zwei Toene zu 100 ms mit 50 ms Pause, 5 ms Rampen gegen Knacksen
        fun doubleBeep(): ShortArray {
            val tone = RATE / 10
            val gap = RATE / 20
            val ramp = RATE / 200
            val out = ShortArray(tone * 2 + gap)
            for (start in listOf(0, tone + gap)) {
                for (i in 0 until tone) {
                    val env = min(1f, min(i, tone - 1 - i) / ramp.toFloat())
                    out[start + i] = (sin(2 * PI * 1400.0 * i / RATE) * env * 0.6 * Short.MAX_VALUE).toInt().toShort()
                }
            }
            return out
        }

        // PCM 16 bit mono als WAV
        fun wav(pcm: ShortArray): ByteArray {
            val data = pcm.size * 2
            val out = ByteArray(44 + data)
            fun text(at: Int, s: String) = s.forEachIndexed { i, c -> out[at + i] = c.code.toByte() }
            fun int(at: Int, v: Int, n: Int) = repeat(n) { out[at + it] = (v shr (8 * it) and 0xFF).toByte() }
            text(0, "RIFF"); int(4, 36 + data, 4); text(8, "WAVE")
            text(12, "fmt "); int(16, 16, 4); int(20, 1, 2); int(22, 1, 2)
            int(24, RATE, 4); int(28, RATE * 2, 4); int(32, 2, 2); int(34, 16, 2)
            text(36, "data"); int(40, data, 4)
            pcm.forEachIndexed { i, s -> int(44 + i * 2, s.toInt(), 2) }
            return out
        }
    }
}
