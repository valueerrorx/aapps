package solutions.xapient.spinr.domain

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Kurzer, trockener Klick wie Holz auf Kunststoff; im Code erzeugt, ohne Audiodatei
object TickSound {
    const val SAMPLE_RATE = 44_100
    private const val SECONDS = 0.035

    fun pcm(rate: Int = SAMPLE_RATE): ShortArray {
        val n = (rate * SECONDS).roundToInt()
        val noise = Random(7)
        var lp = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / rate
            // gefiltertes Rauschen fuer den Anschlag, zwei gedaempfte Resonanzen fuer den Koerper
            lp += 0.35 * (noise.nextDouble(-1.0, 1.0) - lp)
            val hit = lp * exp(-t / 0.0025)
            val body = 0.55 * sin(2 * PI * 1850 * t) * exp(-t / 0.006) + 0.25 * sin(2 * PI * 3400 * t) * exp(-t / 0.003)
            val attack = (t / 0.0004).coerceAtMost(1.0)
            val s = 0.8 * attack * (hit * 1.6 + body)
            (s.coerceIn(-1.0, 1.0) * 26_000).roundToInt().toShort()
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
