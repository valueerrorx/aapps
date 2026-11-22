package solutions.xapient.focusr.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToLong

// Geometrie der Scheibe: Anteil 0..1 = Winkel ab 12 Uhr im Uhrzeigersinn
object Dial {
    // Fingerposition relativ zur Mitte (Bildschirm: y nach unten) -> Anteil in [0, 1)
    fun fraction(dx: Float, dy: Float): Double {
        val a = atan2(dx.toDouble(), -dy.toDouble()) / (2 * PI)
        return if (a < 0) a + 1.0 else a
    }

    // Rand folgt dem Finger; ein Sprung ueber 12 Uhr pinnt auf voll bzw. leer statt durchzuspringen
    fun follow(value: Double, finger: Double, first: Boolean = false): Double {
        if (first) return finger
        if (kotlin.math.abs(finger - value) <= 0.5) return finger
        return if (value > 0.5) 1.0 else 0.0
    }

    // Auf die naechste Raste runden
    fun snap(value: Double, scale: Scale): Long =
        ((value.coerceIn(0.0, 1.0) * Scale.STEPS).roundToLong() * scale.stepMs).coerceIn(0L, scale.ms)

    // Anteil fuer die Anzeige, ueber der Skala bleibt die Scheibe voll
    fun fill(ms: Long, scale: Scale): Float = (ms.toDouble() / scale.ms).coerceIn(0.0, 1.0).toFloat()

    // Neu zeichnen, sobald der Rand ~1 px weiter ist: hoechstens 60/s, mindestens 1/s
    fun tickMs(scale: Scale, radiusPx: Float): Long {
        if (radiusPx <= 0f) return MAX_TICK_MS
        val perPx = scale.ms / (2 * PI * radiusPx)
        return perPx.roundToLong().coerceIn(MIN_TICK_MS, MAX_TICK_MS)
    }

    // Bis zum naechsten Sekundenwechsel der Anzeige (aufgerundete Restzeit)
    fun msToNextSecond(remaining: Long): Long {
        val r = remaining % 1000L
        return if (r == 0L) 1000L else r
    }

    const val MIN_TICK_MS = 16L
    const val MAX_TICK_MS = 1000L
}
