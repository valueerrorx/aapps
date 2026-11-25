package solutions.xapient.thinkr.domain

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// Dezente Spieltoene, im Code erzeugt: weiche Glocken-Sinus mit kurzem Anschlag
object Tones {
    const val SAMPLE_RATE = 44_100

    enum class Kind { HIT, MISS, END }

    fun pcm(kind: Kind, rate: Int = SAMPLE_RATE): ShortArray = when (kind) {
        // heller Doppelton (Quinte), kurz
        Kind.HIT -> mix(rate, 0.16, listOf(Note(1318.5, 0.0, 0.05, 0.5), Note(1975.5, 0.0, 0.03, 0.18)))
        // tief und gedaempft, kein Alarm
        Kind.MISS -> mix(rate, 0.14, listOf(Note(277.2, 0.0, 0.045, 0.45), Note(415.3, 0.0, 0.03, 0.12)))
        // drei aufsteigende Noten
        Kind.END -> mix(
            rate, 0.62,
            listOf(Note(880.0, 0.0, 0.12, 0.38), Note(1108.7, 0.11, 0.12, 0.38), Note(1318.5, 0.22, 0.2, 0.42)),
        )
    }

    private data class Note(val hz: Double, val at: Double, val decay: Double, val gain: Double)

    private fun mix(rate: Int, seconds: Double, notes: List<Note>): ShortArray {
        val n = (rate * seconds).roundToInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / rate
            var s = 0.0
            for (note in notes) {
                val u = t - note.at
                if (u < 0) continue
                val attack = (u / 0.004).coerceAtMost(1.0)
                s += note.gain * attack * exp(-u / note.decay) * sin(2 * PI * note.hz * u)
            }
            // Ausblenden am Ende: kein Knacken
            val tail = ((seconds - t) / 0.01).coerceIn(0.0, 1.0)
            (s * tail).coerceIn(-1.0, 1.0).times(24_000).roundToInt().toShort()
        }
    }

    // 16-bit-Mono-WAV
    fun wav(samples: ShortArray, rate: Int = SAMPLE_RATE): ByteArray {
        val data = samples.size * 2
        val out = ByteArray(44 + data)
        fun str(at: Int, s: String) = s.forEachIndexed { i, ch -> out[at + i] = ch.code.toByte() }
        fun int(at: Int, v: Int) = repeat(4) { out[at + it] = (v ushr (8 * it)).toByte() }
        fun short(at: Int, v: Int) = repeat(2) { out[at + it] = (v ushr (8 * it)).toByte() }
        str(0, "RIFF"); int(4, 36 + data); str(8, "WAVE")
        str(12, "fmt "); int(16, 16); short(20, 1); short(22, 1)
        int(24, rate); int(28, rate * 2); short(32, 2); short(34, 16)
        str(36, "data"); int(40, data)
        samples.forEachIndexed { i, s -> short(44 + 2 * i, s.toInt()) }
        return out
    }
}
