package solutions.xapient.pulsr.domain

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

// Vorab erzeugte Klicks je Klang: Betonung, Schlag, Unterteilung (Mono, -1..1)
class ClickSet(val accent: FloatArray, val beat: FloatArray, val sub: FloatArray) {
    fun of(kind: Kind): FloatArray = when (kind) {
        Kind.ACCENT -> accent
        Kind.BEAT -> beat
        Kind.SUB -> sub
    }
}

object Clicks {
    fun make(sound: Sound, rate: Int): ClickSet = when (sound) {
        Sound.CLICK -> ClickSet(click(rate, 2400.0, 0.9), click(rate, 1600.0, 0.62), click(rate, 1600.0, 0.34))
        Sound.WOOD -> ClickSet(wood(rate, 1250.0, 0.9), wood(rate, 880.0, 0.62), wood(rate, 880.0, 0.34))
        Sound.BEEP -> ClickSet(beep(rate, 1760.0, 0.7), beep(rate, 1320.0, 0.5), beep(rate, 990.0, 0.28))
    }

    // kurzer heller Ton mit Rausch-Anschlag
    private fun click(rate: Int, hz: Double, amp: Double): FloatArray {
        var seed = 0x2545F491
        return render(rate, 0.035) { t ->
            seed = seed * 1103515245 + 12345
            val noise = ((seed ushr 16) and 0x7FFF) / 16384.0 - 1.0
            sin(2 * PI * hz * t) * exp(-t / 0.006) * amp + noise * exp(-t / 0.0012) * amp * 0.35
        }
    }

    // Holzblock: zwei unharmonische Teiltoene, schnell abklingend
    private fun wood(rate: Int, hz: Double, amp: Double): FloatArray = render(rate, 0.08) { t ->
        (sin(2 * PI * hz * t) * 0.75 + sin(2 * PI * hz * 2.76 * t) * 0.25 * exp(-t / 0.006)) * exp(-t / 0.016) * amp
    }

    // weicher Sinus-Piep
    private fun beep(rate: Int, hz: Double, amp: Double): FloatArray = render(rate, 0.06) { t ->
        val x = 2 * PI * hz * t
        (sin(x) * 0.85 + sin(2 * x) * 0.15) * amp
    }

    // kurze Anstiegsflanke und Ausblenden am Ende gegen Knacksen
    private inline fun render(rate: Int, seconds: Double, wave: (Double) -> Double): FloatArray {
        val n = (seconds * rate).toInt()
        val attack = min(n / 8, rate / 2000).coerceAtLeast(1)
        val release = min(n / 3, rate / 100).coerceAtLeast(1)
        return FloatArray(n) { i ->
            val env = when {
                i < attack -> (i + 1).toDouble() / attack
                i >= n - release -> (n - 1 - i).toDouble() / release
                else -> 1.0
            }
            (wave(i.toDouble() / rate) * env).toFloat()
        }
    }
}
