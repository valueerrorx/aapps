package solutions.xapient.dozr.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import java.util.Locale

// Mondbahn: Halbbogen ueber dem Horizont, 0 = rechter Horizont (0 min), 1 = linker Horizont (120 min)
object Moon {
    private const val FULL_MS = Sleep.MAX_MIN * Sleep.MINUTE
    const val STEPS = Sleep.MAX_MIN / Sleep.STEP_MIN

    fun fraction(ms: Long): Float = (ms.toDouble() / FULL_MS).coerceIn(0.0, 1.0).toFloat()

    fun fractionOf(minutes: Int): Float = fraction(minutes * Sleep.MINUTE)

    // Auf 5-min-Rasten, 5..120
    fun snap(frac: Double): Int = Sleep.clampMin((frac.coerceIn(0.0, 1.0) * STEPS).roundToInt() * Sleep.STEP_MIN)

    // Bahnwinkel im Bogenmass, gegen den Uhrzeigersinn ab rechts
    fun angle(frac: Float): Double = PI * frac

    // Alle wie viele ms sich der Mond auf einem Bogen mit Radius r (px) um 1 px bewegt
    fun msPerPixel(radiusPx: Float): Long {
        if (radiusPx <= 1f) return 30_000L
        return (FULL_MS / (PI * radiusPx)).toLong().coerceIn(16L, 30_000L)
    }
}

// Ziehen: Fingerrichtung vom Drehpunkt; unter dem Horizont bleibt der Mond am naechsten Ende, kein Sprung
class MoonDrag(start: Double) {
    var last: Double = start
        private set
    private var fresh = true

    // dx nach rechts, up nach oben positiv; der erste Punkt greift absolut
    fun move(dx: Float, up: Float): Double {
        val next = if (up < 0f) {
            if (last >= 0.5) 1.0 else 0.0
        } else {
            atan2(up.toDouble(), dx.toDouble()) / PI
        }
        // Halbe Bahn in einem Schritt = Finger ging unten herum: ignorieren
        if (fresh || abs(next - last) <= 0.5) last = next
        fresh = false
        return last
    }
}

object TimeText {
    // Aufgerundet auf volle Sekunden: 0:00 erst am Ende
    fun seconds(ms: Long): Long = (ms.coerceAtLeast(0L) + 999L) / 1000L

    // Ganze Minuten plus Restsekunden fuer die Anzeige "23 min 41 s"
    data class Parts(val minutes: Long, val seconds: Long)

    fun parts(ms: Long): Parts {
        val s = seconds(ms)
        return Parts(s / 60, s % 60)
    }

    fun clock(ms: Long): String {
        val s = seconds(ms)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, sec) else "%d:%02d".format(Locale.ROOT, m, sec)
    }
}
