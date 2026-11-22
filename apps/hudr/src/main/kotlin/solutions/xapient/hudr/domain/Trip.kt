package solutions.xapient.hudr.domain

import kotlin.math.max

// Fahrtwerte ohne Route: Strecke, Fahrzeit, Max; nur in Fahrt
class Trip {
    var meters = 0.0
        private set
    var movingSeconds = 0.0
        private set
    var maxMps = 0f
        private set
    private var last: Fix? = null
    private var lastSpeed = 0f

    // Ø in m/s, NaN vor der ersten Sekunde Fahrt
    val avgMps: Float get() = if (movingSeconds >= 1.0) (meters / movingSeconds).toFloat() else Float.NaN

    fun add(fix: Fix, speed: Float, moving: Boolean) {
        val prev = last
        if (prev != null && fix.nanos <= prev.nanos) return
        if (prev != null && moving) {
            val dt = (fix.nanos - prev.nanos) / 1e9
            if (dt <= MAX_GAP) {
                // Trapez aus Geschwindigkeit x Zeit: folgt der Anzeige, kein Positionsrauschen
                meters += (lastSpeed + speed) / 2.0 * dt
                movingSeconds += dt
            } else if (prev.accuracy <= BRIDGE_ACCURACY && fix.accuracy <= BRIDGE_ACCURACY) {
                // Luecke (Tunnel): Luftlinie, wenn plausibel
                val d = Geo.distance(prev, fix)
                if (d / dt <= MAX_PLAUSIBLE) {
                    meters += d
                    movingSeconds += dt
                }
            }
        }
        if (moving && !speed.isNaN()) maxMps = max(maxMps, speed)
        last = fix
        lastSpeed = if (moving && !speed.isNaN()) speed else 0f
    }

    fun reset() {
        meters = 0.0
        movingSeconds = 0.0
        maxMps = 0f
        last = null
        lastSpeed = 0f
    }

    fun save(): DoubleArray = doubleArrayOf(meters, movingSeconds, maxMps.toDouble())

    fun restore(s: DoubleArray) {
        if (s.size < 3 || s.any { it.isNaN() || it < 0 }) return
        meters = s[0]
        movingSeconds = s[1]
        maxMps = s[2].toFloat()
        last = null
        lastSpeed = 0f
    }

    companion object {
        const val MAX_GAP = 3.0
        const val BRIDGE_ACCURACY = 30f
        // 360 km/h: darueber ist es ein Sprung, keine Fahrt
        const val MAX_PLAUSIBLE = 100.0
    }
}
