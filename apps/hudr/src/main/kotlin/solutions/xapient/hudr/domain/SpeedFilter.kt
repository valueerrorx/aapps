package solutions.xapient.hudr.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Geschwindigkeit je Fix: Doppler-Wert oder Fallback aus Positionen, geglaettet,
 * mit Stillstand-Erkennung (Anzeige 0 statt Zittern im Rauschen) und Trend aus der Beschleunigung.
 */
class SpeedFilter {
    // geglaettet in m/s; 0 im Stillstand, NaN bis zum ersten Messwert
    var speed = Float.NaN
        private set
    var moving = false
        private set
    // geglaettete Beschleunigung in m/s², 0 im Stillstand
    var accel = 0f
        private set
    // letzter Wert verlaesslich (Speed-Genauigkeit bekannt und gut, oder unbekannt)
    var reliable = false
        private set
    private var smooth = Float.NaN
    private var lastRaw = Float.NaN
    private var lastNanos = 0L
    private var aboveSince = NONE
    private var below = 0
    private var rejected = 0
    // Position beim Anhalten: Fahrt erst nach echter Verschiebung (gegen Mehrwege-Echos im Stand)
    private var anchor: Fix? = null
    private val window = ArrayDeque<Fix>()

    fun update(fix: Fix): Float {
        if (lastNanos != 0L && fix.nanos <= lastNanos) return speed
        val raw = measured(fix)
        val dt = if (lastNanos == 0L) Float.MAX_VALUE else (fix.nanos - lastNanos) / 1e9f
        if (raw.isNaN()) {
            lastNanos = fix.nanos
            if (anchor == null) anchor = fix
            return speed
        }
        // Sprung mit mehr als 10 m/s² ist ein Ausreisser; nach drei in Folge gilt er doch
        if (!lastRaw.isNaN() && dt <= MAX_GAP && abs(raw - lastRaw) / dt > MAX_ACCEL && rejected < 3) {
            rejected++
            return speed
        }
        rejected = 0
        lastNanos = fix.nanos
        lastRaw = raw
        reliable = fix.speedAccuracy.isNaN() || fix.speedAccuracy <= MAX_NOISE
        // nach laengerer Luecke frisch anfangen
        val fresh = smooth.isNaN() || dt > MAX_GAP
        val alpha = if (fresh) 1f else 1f - exp(-dt / TAU)
        val before = smooth
        smooth = if (smooth.isNaN()) raw else smooth + alpha * (raw - smooth)
        if (!fresh) accel += (1f - exp(-dt / ACCEL_TAU)) * ((smooth - before) / dt - accel)
        val noise = if (fix.speedAccuracy.isNaN()) STOP else max(STOP, min(fix.speedAccuracy, MAX_NOISE))
        if (moving) {
            below = if (raw < noise) below + 1 else 0
            if (below >= STOP_FIXES) {
                moving = false
                below = 0
                anchor = fix
            }
        } else {
            if (anchor == null) anchor = fix
            val candidate = raw >= START && raw > noise && fix.accuracy <= MAX_START_ACCURACY
            aboveSince = if (candidate) (if (aboveSince == NONE) fix.nanos else aboveSince) else NONE
            val a = anchor!!
            val shifted = Geo.distance(a, fix) >= max(max(a.accuracy, fix.accuracy).toDouble(), MIN_SHIFT)
            if (aboveSince != NONE && fix.nanos - aboveSince >= CONFIRM_NS && shifted) {
                moving = true
                aboveSince = NONE
            }
        }
        if (!moving) accel = 0f
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
        accel = 0f
        reliable = false
        smooth = Float.NaN
        lastRaw = Float.NaN
        lastNanos = 0L
        aboveSince = NONE
        below = 0
        rejected = 0
        anchor = null
        window.clear()
    }

    companion object {
        // Zeitkonstanten der Glaettung und der Beschleunigung in s
        const val TAU = 0.8f
        const val ACCEL_TAU = 1f
        // Stillstand unter 0,7 m/s (2,5 km/h), Fahrt ab 1,1 m/s (4 km/h)
        const val STOP = 0.7f
        const val START = 1.1f
        const val MAX_NOISE = 2f
        const val MAX_ACCEL = 10f
        // Fahrt erst nach 2 s Bewegung, guter Genauigkeit und Verschiebung >= max(Genauigkeit, 5 m)
        const val CONFIRM_NS = 2_000_000_000L
        const val MAX_START_ACCURACY = 25f
        const val MIN_SHIFT = 5.0
        const val STOP_FIXES = 2
        const val MAX_GAP = 3f
        const val MIN_BASE = 2.0
        const val WINDOW_NS = 5_000_000_000L
        private const val NONE = Long.MIN_VALUE
    }
}
