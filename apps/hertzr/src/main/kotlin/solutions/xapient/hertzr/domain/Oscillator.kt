package solutions.xapient.hertzr.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// Wellenformen ueber der Phase t in [0,1), alle in Phase mit dem Sinus (Nulldurchgang steigend bei t = 0), Spitze 1.
// dt = Phasenschritt je Sample (f / Rate). Spruenge mit PolyBLEP, Knicke mit PolyBLAMP geglaettet;
// ab 1/10 der Rate gleitend zum Sinus, bei 1/6 rein (3. Harmonische laege ueber Nyquist).
object Oscillator {
    const val BLEND_FROM = 0.1
    const val BLEND_TO = 1.0 / 6.0

    // Grundton-Amplitude je Form, hoechstens 1 (Rechteck 4/pi waere lauter als die Spitze)
    fun fundamental(w: Wave): Double = when (w) {
        Wave.SINE -> 1.0
        Wave.SQUARE -> 1.0
        Wave.TRIANGLE -> 8 / (PI * PI)
        Wave.SAW -> 2 / PI
    }

    // Anteil Sinus 0..1, weich (smoothstep)
    fun sineBlend(dt: Double): Double {
        val x = ((dt - BLEND_FROM) / (BLEND_TO - BLEND_FROM)).coerceIn(0.0, 1.0)
        return x * x * (3 - 2 * x)
    }

    fun sample(w: Wave, t: Double, dt: Double): Double {
        val sine = sin(2 * PI * t)
        if (w == Wave.SINE) return sine
        val b = sineBlend(dt)
        if (b >= 1.0) return fundamental(w) * sine
        val shaped = bandlimited(w, t, dt)
        return if (b <= 0.0) shaped else (1 - b) * shaped + b * fundamental(w) * sine
    }

    fun bandlimited(w: Wave, t: Double, dt: Double): Double = when (w) {
        Wave.SINE -> sin(2 * PI * t)
        Wave.SQUARE -> naive(w, t) + 2 * blep(dist(t, 0.0) / dt) - 2 * blep(dist(t, 0.5) / dt)
        Wave.SAW -> naive(w, t) - 2 * blep(dist(t, 0.5) / dt)
        // Steigung je Phase: +4 / -4; Knick oben -8, unten +8
        Wave.TRIANGLE -> naive(w, t) - 8 * dt * blamp(dist(t, 0.25) / dt) + 8 * dt * blamp(dist(t, 0.75) / dt)
    }

    // ohne Glaettung (Vergleich in Tests, Oszilloskop-Bild)
    fun naive(w: Wave, t: Double): Double = when (w) {
        Wave.SINE -> sin(2 * PI * t)
        Wave.SQUARE -> if (t < 0.5) 1.0 else -1.0
        Wave.SAW -> 2 * frac(t + 0.5) - 1
        Wave.TRIANGLE -> 1 - 4 * abs(frac(t + 0.25) - 0.5)
    }

    // Abstand t - edge, auf -0.5..0.5 gefaltet
    private fun dist(t: Double, edge: Double): Double {
        var d = t - edge
        if (d >= 0.5) d -= 1.0
        if (d < -0.5) d += 1.0
        return d
    }

    // Rest eines Sprungs der Hoehe 1 (geglaettet minus ideal), x in Samples zum Sprung
    fun blep(x: Double): Double = when {
        x <= -1.0 || x >= 1.0 -> 0.0
        x < 0.0 -> (x + 1) * (x + 1) / 2
        else -> -(1 - x) * (1 - x) / 2
    }

    // Integral davon: Rest eines Knicks mit Steigungsaenderung 1 je Sample
    fun blamp(x: Double): Double = when {
        x <= -1.0 || x >= 1.0 -> 0.0
        x < 0.0 -> (x + 1) * (x + 1) * (x + 1) / 6
        else -> (1 - x) * (1 - x) * (1 - x) / 6
    }

    private fun frac(x: Double): Double = x - kotlin.math.floor(x)
}
