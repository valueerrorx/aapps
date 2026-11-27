package solutions.xapient.loudr.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

// A-Bewertung nach IEC 61672-1: drei Biquads aus der bilinearen Transformation, bei 1 kHz auf 0 dB normiert
class AWeighting(val sampleRate: Int) {
    private val b = Array(3) { DoubleArray(3) }
    private val a = Array(3) { DoubleArray(3) }
    private val z1 = DoubleArray(3)
    private val z2 = DoubleArray(3)

    init {
        require(sampleRate in 8_000..384_000) { "Abtastrate $sampleRate" }
        val k = 2.0 * sampleRate
        fun pole(f: Double) = (k - 2 * PI * f) / (k + 2 * PI * f)
        // Hochpass doppelt bei f1, Hochpass f2/f3, Tiefpass doppelt bei f4 (Nullstellen bei Nyquist)
        section(0, 1.0, pole(F1), pole(F1))
        section(1, 1.0, pole(F2), pole(F3))
        section(2, -1.0, pole(F4), pole(F4))
        val g = magnitude(1000.0)
        for (j in 0..2) b[0][j] /= g
    }

    // Doppelte Nullstelle bei q, Pole r1/r2
    private fun section(i: Int, q: Double, r1: Double, r2: Double) {
        b[i][0] = 1.0; b[i][1] = -2 * q; b[i][2] = q * q
        a[i][0] = 1.0; a[i][1] = -(r1 + r2); a[i][2] = r1 * r2
    }

    // Direktform II transponiert
    fun process(x: Double): Double {
        var v = x
        for (i in 0..2) {
            val bi = b[i]
            val ai = a[i]
            val y = bi[0] * v + z1[i]
            z1[i] = bi[1] * v - ai[1] * y + z2[i]
            z2[i] = bi[2] * v - ai[2] * y
            v = y
        }
        return v
    }

    fun reset() {
        z1.fill(0.0)
        z2.fill(0.0)
    }

    // Betrag des Frequenzgangs der Kette
    fun magnitude(f: Double): Double {
        val w = 2 * PI * f / sampleRate
        var m = 1.0
        for (i in 0..2) m *= poly(b[i], w) / poly(a[i], w)
        return m
    }

    fun gainDb(f: Double): Double = 20 * log10(magnitude(f))

    private fun poly(c: DoubleArray, w: Double): Double =
        hypot(c[0] + c[1] * cos(w) + c[2] * cos(2 * w), c[1] * sin(w) + c[2] * sin(2 * w))

    companion object {
        const val F1 = 20.598997
        const val F2 = 107.65265
        const val F3 = 737.86223
        const val F4 = 12194.217

        // Sollkurve A(f) in dB laut Norm-Formel
        fun reference(f: Double): Double {
            val f2 = f * f
            val ra = F4 * F4 * f2 * f2 /
                ((f2 + F1 * F1) * sqrt((f2 + F2 * F2) * (f2 + F3 * F3)) * (f2 + F4 * F4))
            return 20 * log10(ra) + 2.0
        }
    }
}
