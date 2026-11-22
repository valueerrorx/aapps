package solutions.xapient.hudr.domain

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Geschwindigkeit je Fix: Doppler-Wert oder Fallback aus Positionen, geglaettet,
 * mit Stillstand-Erkennung (Anzeige 0 statt Zittern im Rauschen).
 */
class SpeedFilter {
    // geglaettet in m/s; 0 im Stillstand, NaN bis zum ersten Messwert
    var speed = Float.NaN
        private set
    var moving = false
        private set
    private var smooth = Float.NaN
    private var lastNanos = 0L
    private var above = 0
    private var below = 0
    private val window = ArrayDeque<Fix>()

    fun update(fix: Fix): Float {
        if (lastNanos != 0L && fix.nanos <= lastNanos) return speed
        val raw = measured(fix)
        val dt = if (lastNanos == 0L) Float.MAX_VALUE else (fix.nanos - lastNanos) / 1e9f
        lastNanos = fix.nanos
        if (raw.isNaN()) return speed
        // nach laengerer Luecke frisch anfangen
        val alpha = if (smooth.isNaN() || dt > MAX_GAP) 1f else 1f - exp(-dt / TAU)
        smooth = if (smooth.isNaN()) raw else smooth + alpha * (raw - smooth)
        val noise = if (fix.speedAccuracy.isNaN()) STOP else max(STOP, min(fix.speedAccuracy, MAX_NOISE))
        if (moving) {
            below = if (raw < noise) below + 1 else 0
            if (below >= CONFIRM) {
                moving = false
                below = 0
            }
        } else {
            above = if (raw >= START && raw > noise) above + 1 else 0
            if (above >= CONFIRM) {
                moving = true
                above = 0
            }
        }
        speed = if (moving) smooth.coerceAtLeast(0f) else 0f
        return speed
    }

    // Doppler, sonst Strecke ueber >= 2 s Basis; Strecke im Rauschen = Stillstand
    private fun measured(fix: Fix): Float {
        if (!fix.speed.isNaN()) {
            window.clear()
            return fix.speed.coerceAtLeast(0f)
        }
        window.addLast(fix)
        while (fix.nanos - window.first().nanos > WINDOW_NS) window.removeFirst()
        val first = window.first()
        val dt = (fix.nanos - first.nanos) / 1e9
        if (dt < MIN_BASE) return Float.NaN
        val d = Geo.distance(first, fix)
        val noise = (first.accuracy + fix.accuracy) / 2.0
        return if (d <= noise) 0f else (d / dt).toFloat()
    }

    fun reset() {
        speed = Float.NaN
        moving = false
        smooth = Float.NaN
        lastNanos = 0L
        above = 0
        below = 0
        window.clear()
    }

    companion object {
        // Zeitkonstante der Glaettung in s
        const val TAU = 0.8f
        // Stillstand unter 0,7 m/s (2,5 km/h), Fahrt ab 1,1 m/s (4 km/h)
        const val STOP = 0.7f
        const val START = 1.1f
        const val MAX_NOISE = 2f
        const val CONFIRM = 2
        const val MAX_GAP = 3f
        const val MIN_BASE = 2.0
        const val WINDOW_NS = 5_000_000_000L
    }
}
