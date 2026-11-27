package solutions.xapient.tunr.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Radix-2-FFT in place, Groesse = Zweierpotenz; Tabellen einmal je Groesse
class Fft(val size: Int) {
    private val cosT: DoubleArray
    private val sinT: DoubleArray
    private val rev: IntArray

    init {
        require(size >= 2 && size and (size - 1) == 0) { "Groesse muss Zweierpotenz sein: $size" }
        cosT = DoubleArray(size / 2) { cos(2 * PI * it / size) }
        sinT = DoubleArray(size / 2) { sin(2 * PI * it / size) }
        val bits = size.countTrailingZeroBits()
        rev = IntArray(size) { reverseBits(it) ushr (32 - bits) }
    }

    // inverse = true: ohne 1/n-Skalierung
    fun transform(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
        for (i in 0 until size) {
            val j = rev[i]
            if (j > i) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        val sign = if (inverse) 1.0 else -1.0
        var len = 2
        while (len <= size) {
            val half = len / 2
            val step = size / len
            var start = 0
            while (start < size) {
                for (k in 0 until half) {
                    val wr = cosT[k * step]
                    val wi = sign * sinT[k * step]
                    val a = start + k
                    val b = a + half
                    val xr = re[b] * wr - im[b] * wi
                    val xi = re[b] * wi + im[b] * wr
                    re[b] = re[a] - xr; im[b] = im[a] - xi
                    re[a] += xr; im[a] += xi
                }
                start += len
            }
            len *= 2
        }
    }

    companion object {
        fun nextPow2(n: Int): Int {
            var p = 2
            while (p < n) p *= 2
            return p
        }
    }
}

// Bitreihenfolge umdrehen (wie Integer.reverse, ohne JVM)
private fun reverseBits(x: Int): Int {
    var v = x
    var r = 0
    repeat(32) {
        r = (r shl 1) or (v and 1)
        v = v ushr 1
    }
    return r
}
