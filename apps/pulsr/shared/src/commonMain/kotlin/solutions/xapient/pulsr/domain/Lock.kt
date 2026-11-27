package solutions.xapient.pulsr.domain

import kotlin.math.PI

// Sperre zwischen Audio-Thread und Oberflaeche; Android synchronized, iOS NSLock
expect class Lock() {
    fun <T> locked(block: () -> T): T
}

// wie Math.toDegrees/toRadians (gleiche Konstanten, gleiche Rundung)
object Angles {
    fun deg(rad: Double): Double = rad * (180.0 / PI)
    fun rad(deg: Double): Double = deg * (PI / 180.0)
}
