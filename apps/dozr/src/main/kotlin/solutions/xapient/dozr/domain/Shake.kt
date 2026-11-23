package solutions.xapient.dozr.domain

import kotlin.math.sqrt

// Kraeftiges Schuetteln: mehrere Stoesse ueber der Schwelle in kurzer Zeit, danach Pause
class ShakeDetector(
    private val thresholdG: Float = 2.4f,
    private val hits: Int = 3,
    private val windowMs: Long = 1_000L,
    private val gapMs: Long = 80L,
    private val cooldownMs: Long = 3_000L,
) {
    private val times = ArrayDeque<Long>()
    private var lastFire = Long.MIN_VALUE / 2
    private var lastAbove = Long.MIN_VALUE / 2

    // Beschleunigung in m/s^2 inklusive Schwerkraft; true = Schuetteln erkannt
    fun onSample(x: Float, y: Float, z: Float, tMs: Long): Boolean {
        if (tMs - lastFire < cooldownMs) return false
        val g = sqrt(x * x + y * y + z * z) / EARTH
        if (g < thresholdG) return false
        // ein Stoss besteht aus mehreren Proben: nur getrennte Spitzen zaehlen
        val prev = lastAbove
        lastAbove = tMs
        if (tMs - prev < gapMs) return false
        times.addLast(tMs)
        while (times.isNotEmpty() && tMs - times.first() > windowMs) times.removeFirst()
        if (times.size < hits) return false
        times.clear()
        lastFire = tMs
        return true
    }

    companion object {
        const val EARTH = 9.80665f
    }
}
