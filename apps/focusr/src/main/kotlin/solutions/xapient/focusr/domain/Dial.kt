package solutions.xapient.focusr.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToLong

// Ein Ziehen: Fingerweg wird aufaddiert; ueber 12 Uhr hinaus bleibt der Rand voll bzw. leer,
// bis der Finger 12 Uhr zurueck ueberquert (Ueberdrehen zaehlt hoechstens bis zur naechsten 12)
class DialDrag {
    private var u = Double.NaN

    val value: Double get() = u.coerceIn(0.0, 1.0)

    fun move(finger: Double): Double {
        if (u.isNaN()) {
            u = finger
            return value
        }
        // kuerzester Weg ab der letzten Fingerposition
        var d = finger - (u % 1.0 + 1.0) % 1.0
        if (d > 0.5) d -= 1.0 else if (d < -0.5) d += 1.0
        u += d
        if (u > 1.0) u = 1.0 + (u - 1.0) % 1.0
        if (u < 0.0) u = -((-u) % 1.0)
        return value
    }
}

// Geometrie der Scheibe: Anteil 0..1 = Winkel ab 12 Uhr im Uhrzeigersinn
object Dial {
    // Fingerposition relativ zur Mitte (Bildschirm: y nach unten) -> Anteil in [0, 1)
    fun fraction(dx: Float, dy: Float): Double {
        val a = atan2(dx.toDouble(), -dy.toDouble()) / (2 * PI)
        return if (a < 0) a + 1.0 else a
    }

    // Winkelabstand auf dem Kreis, Ergebnis in [0, 0.5]
    fun distance(a: Double, b: Double): Double {
        val d = abs(a - b) % 1.0
        return min(d, 1.0 - d)
    }

    // Beim Lauf nur nahe der Kante greifen, damit ein Antippen die Restzeit nicht verstellt
    fun nearEdge(finger: Double, fill: Float): Boolean = distance(finger, fill.toDouble()) <= EDGE_GRAB

    const val EDGE_GRAB = 20.0 / 360.0

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
