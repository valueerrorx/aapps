package solutions.xapient.hudr.domain

import kotlin.math.PI
import kotlin.math.cos

// Testhelfer: Fixes entlang eines Breitengrads, deterministisch
object Track {
    const val LAT = 48.2
    const val SEC = 1_000_000_000L
    private const val METERS_PER_DEG_LAT = 111_195.0

    fun lonFor(meters: Double): Double = meters / (METERS_PER_DEG_LAT * cos(LAT * PI / 180))

    fun fix(t: Double, meters: Double = 0.0, speed: Float = Float.NaN, acc: Float = 4f, speedAcc: Float = Float.NaN) =
        Fix((t * SEC).toLong(), LAT, 16.3 + lonFor(meters), acc, speed, speedAcc)
}
