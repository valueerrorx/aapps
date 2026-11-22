package solutions.xapient.tunr.domain

import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.sqrt

// Grundfrequenz und Klarheit (1 - normierte Differenz am Minimum)
data class Pitch(val hz: Float, val clarity: Float)

// YIN mit Differenzfunktion ueber FFT; nicht threadsicher, ein Objekt je Audio-Thread
class PitchDetector(
    val sampleRate: Int,
    minHz: Float = MIN_HZ,
    maxHz: Float = MAX_HZ,
    private val threshold: Double = 0.15,
) {
    // Fenster ~43 ms: gut 1,5 Perioden von E1
    val window: Int = (sampleRate * 0.0427).toInt()
    private val tauMax: Int = ceil(sampleRate / minHz).toInt()
    private val tauMin: Int = (sampleRate / maxHz).toInt().coerceAtLeast(2)

    // so viele Samples braucht detect()
    val size: Int = window + tauMax + 2
    private val fft = Fft(Fft.nextPow2(size))
    private val n = fft.size
    private val aRe = DoubleArray(n)
    private val aIm = DoubleArray(n)
    private val bRe = DoubleArray(n)
    private val bIm = DoubleArray(n)
    private val energy = DoubleArray(size + 1)
    private val d = DoubleArray(tauMax + 2)
    private val cm = DoubleArray(tauMax + 2)

    // x: mindestens size Samples, die aeltesten zuerst; null = kein Ton
    fun detect(x: FloatArray): Pitch? {
        require(x.size >= size)
        difference(x)
        normalize()
        var tau = firstDip() ?: return null
        tau = octaveCheck(tau)
        val est = tau + parabola(tau)
        if (est <= 0.0) return null
        val hz = sampleRate / est
        return Pitch(hz.toFloat(), (1.0 - cm[tau]).toFloat().coerceIn(0f, 1f))
    }

    // d(tau) = E(0..W) + E(tau..tau+W) - 2 * Kreuzkorrelation(tau)
    private fun difference(x: FloatArray) {
        aRe.fill(0.0); aIm.fill(0.0); bRe.fill(0.0); bIm.fill(0.0)
        energy[0] = 0.0
        for (i in 0 until size) {
            val v = x[i].toDouble()
            bRe[i] = v
            if (i < window) aRe[i] = v
            energy[i + 1] = energy[i] + v * v
        }
        fft.transform(aRe, aIm)
        fft.transform(bRe, bIm)
        // conj(A) * B
        for (k in 0 until n) {
            val r = aRe[k] * bRe[k] + aIm[k] * bIm[k]
            val i = aRe[k] * bIm[k] - aIm[k] * bRe[k]
            bRe[k] = r; bIm[k] = i
        }
        fft.transform(bRe, bIm, inverse = true)
        val e0 = energy[window]
        for (tau in 0..tauMax + 1) {
            val corr = bRe[tau] / n
            d[tau] = (e0 + energy[tau + window] - energy[tau] - 2 * corr).coerceAtLeast(0.0)
        }
    }

    // kumulativ normierte Differenz (YIN Schritt 3)
    private fun normalize() {
        cm[0] = 1.0
        var run = 0.0
        for (tau in 1..tauMax + 1) {
            run += d[tau]
            cm[tau] = if (run > 0) d[tau] * tau / run else 1.0
        }
    }

    // erstes Minimum unter der Schwelle, dann bis zum Tal absteigen
    private fun firstDip(): Int? {
        var tau = tauMin
        while (tau < tauMax) {
            if (cm[tau] < threshold) {
                while (tau + 1 < tauMax && cm[tau + 1] < cm[tau]) tau++
                return tau
            }
            tau++
        }
        return null
    }

    // schwacher Grundton: deutlich tieferes Tal bei 2 oder 3 Perioden gewinnt
    private fun octaveCheck(tau: Int): Int {
        val base = floor(tau)
        if (base < 0.01) return tau
        for (k in 2..3) {
            val c = k * tau
            if (c + 2 >= tauMax) break
            var best = c
            val span = (c * 0.03).toInt().coerceAtLeast(2)
            for (t in (c - span).coerceAtLeast(tauMin)..(c + span).coerceAtMost(tauMax - 1)) {
                if (cm[t] < cm[best]) best = t
            }
            if (floor(best) < 0.2 * base) return best
        }
        return tau
    }

    // Talsohle zwischen den Stuetzstellen (Parabel-Scheitel), sonst zaehlt bei hohen Toenen das Raster
    private fun floor(tau: Int): Double {
        val a = cm[tau - 1]
        val b = cm[tau]
        val c = cm[tau + 1]
        val den = a - 2 * b + c
        if (den <= 0.0) return b
        return (b - (a - c) * (a - c) / (8 * den)).coerceAtLeast(0.0)
    }

    // Scheitel der Parabel durch d(tau-k), d(tau), d(tau+k); weite Stuetzstellen bei langen Perioden daempfen Rauschen
    private fun parabola(tau: Int): Double {
        val k = (tau / 40).coerceIn(1, tauMax + 1 - tau)
        if (tau - k < 0 || k < 1) return 0.0
        val a = d[tau - k]
        val b = d[tau]
        val c = d[tau + k]
        val den = a - 2 * b + c
        if (den <= 0.0) return 0.0
        return (0.5 * k * (a - c) / den).coerceIn(-k.toDouble(), k.toDouble())
    }

    companion object {
        const val MIN_HZ = 38f
        const val MAX_HZ = 4500f

        // Effektivpegel in dBFS (Vollaussteuerung = 1,0)
        fun rmsDb(x: FloatArray, from: Int = 0, count: Int = x.size - from): Float {
            if (count <= 0) return -120f
            var s = 0.0
            for (i in from until from + count) s += x[i].toDouble() * x[i]
            val rms = sqrt(s / count)
            return if (rms < 1e-6) -120f else (20 * log10(rms)).toFloat()
        }
    }
}
