package solutions.xapient.hertzr.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

// Messwerkzeuge fuer die Tests: Rendern, Frequenz ueber Nulldurchgaenge, Spektrum
object Signal {
    const val RATE = 48_000

    // n Frames, Bloecke wie im Audio-Thread (240 = 5 ms)
    fun render(r: ToneRenderer, frames: Int, block: Int = 240): FloatArray {
        val out = FloatArray(frames * 2)
        val buf = FloatArray(block * 2)
        var done = 0
        while (done < frames) {
            val n = minOf(block, frames - done)
            r.render(buf, n)
            System.arraycopy(buf, 0, out, done * 2, n * 2)
            done += n
        }
        return out
    }

    fun left(st: FloatArray) = DoubleArray(st.size / 2) { st[2 * it].toDouble() }
    fun right(st: FloatArray) = DoubleArray(st.size / 2) { st[2 * it + 1].toDouble() }

    // steigende Nulldurchgaenge, linear interpoliert: Frequenz = Perioden / Zeit
    fun zeroCrossHz(x: DoubleArray, from: Int = 0, to: Int = x.size, rate: Int = RATE): Double {
        var first = -1.0
        var last = -1.0
        var count = 0
        for (i in maxOf(from, 1) until to) {
            if (x[i - 1] < 0 && x[i] >= 0) {
                val t = i - 1 + (-x[i - 1]) / (x[i] - x[i - 1])
                if (first < 0) first = t
                last = t
                count++
            }
        }
        return if (count < 2) Double.NaN else (count - 1) * rate / (last - first)
    }

    fun rmsDb(x: DoubleArray, from: Int = 0, to: Int = x.size): Double {
        var s = 0.0
        for (i in from until to) s += x[i] * x[i]
        return 10 * log10(s / (to - from))
    }

    fun maxStep(x: DoubleArray): Double {
        var m = 0.0
        for (i in 1 until x.size) m = maxOf(m, kotlin.math.abs(x[i] - x[i - 1]))
        return m
    }

    // Leistungsspektrum (Blackman-Harris, n = Zweierpotenz)
    fun spectrum(x: DoubleArray, from: Int, n: Int): DoubleArray {
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        for (i in 0 until n) {
            val a = 2 * PI * i / (n - 1)
            val w = 0.35875 - 0.48829 * cos(a) + 0.14128 * cos(2 * a) - 0.01168 * cos(3 * a)
            re[i] = x[from + i] * w
        }
        fft(re, im)
        return DoubleArray(n / 2) { re[it] * re[it] + im[it] * im[it] }
    }

    // staerkste Nicht-Harmonische relativ zum Grundton in dB; Bins nahe k*f zaehlen als Harmonische
    // bandHz: nur Anteile darunter zaehlen (Hoerbereich), Standard bis Nyquist
    fun worstAliasDb(x: DoubleArray, hz: Double, bandHz: Double = RATE / 2.0, from: Int = 8_192, n: Int = 32_768, rate: Int = RATE): Double {
        val p = spectrum(x, from, n)
        val bin = rate.toDouble() / n
        val guard = 8
        val harm = BooleanArray(p.size)
        var k = 1
        while (k * hz < rate / 2.0) {
            val c = (k * hz / bin).toInt()
            for (j in c - guard..c + guard + 1) if (j in harm.indices) harm[j] = true
            k++
        }
        val f0 = (hz / bin).toInt()
        val fund = (f0 - 2..f0 + 2).maxOf { p[it] }
        var worst = 0.0
        // unter 20 Hz liegt nur Fensterleck
        for (i in (20 / bin).toInt() until minOf(p.size, (bandHz / bin).toInt())) if (!harm[i]) worst = maxOf(worst, p[i])
        return 10 * log10(worst / fund)
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2 * PI / len
            val wr = cos(ang)
            val wi = sin(ang)
            var i = 0
            while (i < n) {
                var cr = 1.0
                var ci = 0.0
                for (k in 0 until len / 2) {
                    val ur = re[i + k]
                    val ui = im[i + k]
                    val vr = re[i + k + len / 2] * cr - im[i + k + len / 2] * ci
                    val vi = re[i + k + len / 2] * ci + im[i + k + len / 2] * cr
                    re[i + k] = ur + vr
                    im[i + k] = ui + vi
                    re[i + k + len / 2] = ur - vr
                    im[i + k + len / 2] = ui - vi
                    val nr = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = nr
                }
                i += len
            }
            len = len shl 1
        }
    }

    fun sqrtHalfDb(): Double = 20 * log10(sqrt(0.5))
}
