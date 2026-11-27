package solutions.xapient.tunr.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.random.Random

// Gezupfte Saite per Karplus-Strong; Allpass im Kreis trifft die Frequenz auf Bruchteile eines Samples
object Pluck {
    // Gesamtlaenge, davon das Ende sanft ausgeblendet
    const val SECONDS = 2.8f
    const val FADE_S = 0.8f
    const val ATTACK_S = 0.003f
    // Grundton faellt in dieser Zeit um 60 dB
    const val T60_S = 3.2f
    const val PEAK = 0.5f
    // Nachlauf fuer Ausgabe-/Eingabelatenz und Raum, bevor das Mikrofon wieder zaehlt
    const val TAIL_S = 0.3f

    // Analysen (je 25 ms), die waehrend des Referenztons nicht zaehlen
    val HUSH_FRAMES: Int = ceil((SECONDS + TAIL_S) * 40f).toInt()

    fun render(hz: Double, rate: Int, seconds: Float = SECONDS, seed: Int = 1): FloatArray {
        val n = (seconds * rate).roundToInt()
        val out = FloatArray(n)
        if (n == 0 || hz <= 0.0) return out
        // Schleife: Verzoegerung L + Mittelwert (0,5) + Allpass (frac) = eine Periode
        val period = rate / hz
        val len = floor(period - 0.6).toInt().coerceAtLeast(2)
        val frac = period - 0.5 - len
        val c = (1 - frac) / (1 + frac)
        val buf = DoubleArray(len)
        // Verlust je Umlauf, damit der Grundton nach T60 um 60 dB faellt
        val target = exp(ln(0.001) / (hz * T60_S))
        val rho = (target / cos(PI * hz / rate)).coerceAtMost(0.99999)

        val burst = excitation(period, rate, hz, seed)
        var prev = 0.0
        var apIn = 0.0
        var apOut = 0.0
        // Gleichanteil-Sperre (~15 Hz)
        val r = 1 - 2 * PI * 15 / rate
        var dcIn = 0.0
        var dcOut = 0.0
        for (i in 0 until n) {
            val d = buf[i % len]
            val lp = rho * 0.5 * (d + prev)
            prev = d
            val ap = c * lp + apIn - c * apOut
            apIn = lp
            apOut = ap
            val s = ap + (if (i < burst.size) burst[i] else 0.0)
            buf[i % len] = s
            dcOut = s - dcIn + r * dcOut
            dcIn = s
            out[i] = dcOut.toFloat()
        }
        shape(out, rate)
        return out
    }

    // Rauschstoss ueber eine Periode: weich gefiltert, Kammfilter = Anschlag nahe am Steg
    private fun excitation(period: Double, rate: Int, hz: Double, seed: Int): DoubleArray {
        val m = period.roundToInt().coerceAtLeast(2)
        val rnd = Random(seed)
        val fc = (hz * 12).coerceIn(1200.0, 5000.0)
        val a = 1 - exp(-2 * PI * fc / rate)
        val lp = DoubleArray(m)
        var y = 0.0
        for (i in 0 until m) {
            y += a * (rnd.nextDouble() * 2 - 1 - y)
            lp[i] = y
        }
        val mean = lp.average()
        val pos = (m / 7).coerceAtLeast(1)
        return DoubleArray(m) { i -> (lp[i] - mean) - (if (i >= pos) lp[i - pos] - mean else 0.0) }
    }

    // Spitze auf PEAK, weicher Einsatz, Ende sanft auf null
    private fun shape(x: FloatArray, rate: Int) {
        var peak = 0f
        for (v in x) peak = maxOf(peak, abs(v))
        val g = if (peak > 0f) PEAK / peak else 0f
        val attack = (ATTACK_S * rate).roundToInt().coerceAtLeast(1)
        val fade = (FADE_S * rate).roundToInt().coerceAtMost(x.size)
        val fadeFrom = x.size - fade
        for (i in x.indices) {
            var w = g
            if (i < attack) w *= raised(i.toDouble() / attack)
            if (i >= fadeFrom) w *= raised((x.size - 1 - i).toDouble() / (fade - 1).coerceAtLeast(1))
            x[i] *= w
        }
    }

    // 0 -> 0, 1 -> 1, ohne Knick an den Enden
    private fun raised(t: Double): Float = (0.5 * (1 - cos(PI * t.coerceIn(0.0, 1.0)))).toFloat()
}

// Mischt gerenderte Toene; ein neuer Ton blendet laufende in fadeMs aus. Nicht threadsicher.
class ToneMixer(val rate: Int, fadeMs: Float = 30f) {
    private class Voice(val x: FloatArray) {
        var pos = 0
        // verbleibende Ausblend-Samples, -1 = spielt voll
        var fade = -1
    }

    private val fadeLen = (rate * fadeMs / 1000f).roundToInt().coerceAtLeast(1)
    private val voices = ArrayList<Voice>()

    val idle: Boolean get() = voices.isEmpty()

    fun trigger(x: FloatArray) {
        release()
        voices += Voice(x)
    }

    // alles schnell ausblenden
    fun release() {
        for (v in voices) if (v.fade < 0) v.fade = fadeLen
    }

    fun render(out: FloatArray, n: Int = out.size) {
        out.fill(0f, 0, n)
        val it = voices.iterator()
        while (it.hasNext()) {
            val v = it.next()
            var i = 0
            while (i < n && v.pos < v.x.size && v.fade != 0) {
                var g = 1f
                if (v.fade > 0) {
                    g = (0.5 * (1 - cos(PI * v.fade / fadeLen))).toFloat()
                    v.fade--
                }
                out[i] += v.x[v.pos++] * g
                i++
            }
            if (v.pos >= v.x.size || v.fade == 0) it.remove()
        }
        for (i in 0 until n) out[i] = out[i].coerceIn(-1f, 1f)
    }
}
