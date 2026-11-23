package solutions.xapient.hudr.domain

import kotlin.math.max

// Fahrtwerte ohne Route: Strecke, Fahrzeit, Max
class Trip {
    var meters = 0.0
        private set
    var movingSeconds = 0.0
        private set
    var maxMps = 0f
        private set
    private var last: Fix? = null
    private var lastSpeed = 0f
    private var lastMoving = false
    // Max nur, was mindestens 2 s anlag: Minimum ueber das Fenster
    private val recent = ArrayDeque<Pair<Long, Float>>()

    // Ø in m/s, NaN vor der ersten Sekunde Fahrt
    val avgMps: Float get() = if (movingSeconds >= 1.0) (meters / movingSeconds).toFloat() else Float.NaN

    fun add(fix: Fix, speed: Float, moving: Boolean, reliable: Boolean = true) {
        val prev = last
        if (prev != null && fix.nanos <= prev.nanos) return
        val v = if (moving && !speed.isNaN()) speed else 0f
        // auch das letzte Stueck bis zum Stillstand zaehlt (Trapez bis 0)
        if (prev != null && (moving || lastMoving)) {
            val dt = (fix.nanos - prev.nanos) / 1e9
            if (dt <= MAX_GAP) {
                // Trapez aus Geschwindigkeit x Zeit: folgt der Anzeige, kein Positionsrauschen
                meters += (lastSpeed + v) / 2.0 * dt
                movingSeconds += dt
            } else if (moving && prev.accuracy <= BRIDGE_ACCURACY && fix.accuracy <= BRIDGE_ACCURACY) {
                // Luecke (Tunnel): Luftlinie, wenn plausibel
                val d = Geo.distance(prev, fix)
                if (d / dt <= MAX_PLAUSIBLE) {
                    meters += d
                    movingSeconds += dt
                }
            }
        }
        holdMax(fix.nanos, v, moving && reliable)
        last = fix
        lastSpeed = v
        lastMoving = moving
    }

    private fun holdMax(now: Long, v: Float, ok: Boolean) {
        if (!ok) {
            recent.clear()
            return
        }
        recent.addLast(now to v)
        while (recent.size >= 2 && recent[1].first <= now - HOLD_NS) recent.removeFirst()
        if (recent.first().first <= now - HOLD_NS) maxMps = max(maxMps, recent.minOf { it.second })
    }

    fun reset() {
        meters = 0.0
        movingSeconds = 0.0
        maxMps = 0f
        last = null
        lastSpeed = 0f
        lastMoving = false
        recent.clear()
    }

    fun save(): DoubleArray = doubleArrayOf(meters, movingSeconds, maxMps.toDouble())

    fun restore(s: DoubleArray) {
        if (s.size < 3 || s.any { it.isNaN() || it < 0 }) return
        reset()
        meters = s[0]
        movingSeconds = s[1]
        maxMps = s[2].toFloat()
    }

    companion object {
        const val MAX_GAP = 3.0
        const val BRIDGE_ACCURACY = 30f
        // 360 km/h: darueber ist es ein Sprung, keine Fahrt
        const val MAX_PLAUSIBLE = 100.0
        const val HOLD_NS = 2_000_000_000L
    }
}
