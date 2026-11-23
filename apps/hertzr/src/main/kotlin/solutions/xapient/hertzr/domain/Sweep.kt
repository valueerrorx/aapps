package solutions.xapient.hertzr.domain

import kotlin.math.abs
import kotlin.math.pow

// Logarithmischer Sweep: gleiche Zeit je Oktave, auch abwaerts
data class SweepSpec(val from: Double, val to: Double, val seconds: Int) {
    fun hzAt(progress: Double): Double = from * (to / from).pow(progress.coerceIn(0.0, 1.0))
}

object Sweep {
    val DURATIONS = listOf(5, 10, 20, 30, 60)

    fun nearestDuration(s: Int): Int = DURATIONS.minBy { abs(it - s) }

    // Tippen auf die Dauer: naechste, nach der laengsten wieder die kuerzeste
    fun nextDuration(s: Int): Int {
        val i = DURATIONS.indexOf(nearestDuration(s))
        return DURATIONS[(i + 1) % DURATIONS.size]
    }
}
