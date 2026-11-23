package solutions.xapient.hudr.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Eine GPS-Position ohne Android-Typen.
 * speed/speedAccuracy in m/s, NaN wenn das Geraet keine liefert; accuracy horizontal in m.
 */
data class Fix(
    val nanos: Long,
    val lat: Double,
    val lon: Double,
    val accuracy: Float,
    val speed: Float = Float.NaN,
    val speedAccuracy: Float = Float.NaN,
)

object Geo {
    private const val EARTH_RADIUS = 6_371_008.8

    // Grosskreis-Abstand in m (Haversine)
    fun distance(a: Fix, b: Fix): Double {
        val p1 = a.lat * PI / 180
        val p2 = b.lat * PI / 180
        val dp = p2 - p1
        val dl = (b.lon - a.lon) * PI / 180
        val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
