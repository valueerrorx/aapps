package solutions.xapient.spinr.domain

import kotlin.math.exp
import kotlin.math.ln

// Abbremsen: Gleitreibung a (Grad/s²) plus Lagerreibung k (1/s), w' = -a - k*w; geschlossen loesbar
class Friction(val a: Double = 60.0, val k: Double = 0.5) {
    init {
        require(a > 0 && k > 0)
    }

    fun stopTime(w0: Double): Double = if (w0 <= 0) 0.0 else ln(1 + k * w0 / a) / k

    // zurueckgelegter Winkel bis zum Stillstand
    fun distance(w0: Double): Double = if (w0 <= 0) 0.0 else w0 / k - a / (k * k) * ln(1 + k * w0 / a)

    // Weg nach t Sekunden (t wird auf die Stoppzeit begrenzt)
    fun travelled(w0: Double, t: Double): Double {
        if (w0 <= 0 || t <= 0) return 0.0
        val tt = t.coerceAtMost(stopTime(w0))
        return (w0 + a / k) * (1 - exp(-k * tt)) / k - a * tt / k
    }

    fun velocity(w0: Double, t: Double): Double {
        if (w0 <= 0 || t >= stopTime(w0)) return 0.0
        return ((w0 + a / k) * exp(-k * t) - a / k).coerceAtLeast(0.0)
    }

    // Umkehrung von distance per Bisektion (streng monoton)
    fun velocityFor(distance: Double): Double {
        if (distance <= 0) return 0.0
        var lo = 0.0
        var hi = 100.0
        while (distance(hi) < distance) hi *= 2
        repeat(80) {
            val mid = (lo + hi) / 2
            if (distance(mid) < distance) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }
}
