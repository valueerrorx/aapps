package solutions.xapient.magnr.domain

import kotlin.math.abs
import kotlin.math.exp

// Vollausschlag und Rauschschwelle in µT; Vollausschlag durch 4 teilbar fuer runde Skalenwerte
enum class Sensitivity(val fullScale: Float, val noise: Float) {
    LOW(100f, 3f),
    MEDIUM(40f, 0.9f),
    HIGH(12f, 0.6f);

    // Ruhebereich fuer den Driftausgleich: doppelte Rauschschwelle
    val quietBand: Float get() = 2f * noise

    // Ausschlag 0..1 aus der Abweichung, unter dem Rauschen 0
    fun strength(dev: Float): Float {
        if (dev.isNaN()) return 0f
        return ((abs(dev) - noise) / (fullScale - noise)).coerceIn(0f, 1f)
    }
}

// Stufen des Ausschlags; steigen sofort, fallen erst mit Hysterese
enum class Level(val from: Float) {
    QUIET(0f),
    WEAK(0.12f),
    NEAR(0.45f),
    STRONG(0.8f);

    companion object {
        const val HYSTERESIS = 0.04f

        fun of(strength: Float, previous: Level = QUIET): Level {
            val up = entries.last { strength >= it.from }
            if (up >= previous) return up
            return entries.last { it <= previous && (it == QUIET || strength >= it.from - HYSTERESIS) }
        }
    }
}

// Exponentielle Glaettung mit Zeitkonstante, unabhaengig von der Abtastrate
class Smoother(private val tauSeconds: Float) {
    var value = Float.NaN
        private set

    fun reset() {
        value = Float.NaN
    }

    fun step(x: Float, dtSeconds: Float): Float {
        value = if (value.isNaN()) x else value + (x - value) * (1f - exp(-dtSeconds / tauSeconds))
        return value
    }
}

// Erdfeld am Ort: Mittel der ersten Messungen, Nullen von Hand, langsame Drift nur im Ruhebereich
class Baseline(
    private val settleNanos: Long = 500_000_000L,
    private val driftTau: Float = 30f,
) {
    var value = Float.NaN
        private set
    val settled: Boolean get() = !value.isNaN()

    private var sum = 0.0
    private var count = 0
    private var startNanos = -1L

    fun reset() {
        value = Float.NaN
        sum = 0.0
        count = 0
        startNanos = -1L
    }

    fun set(v: Float) {
        reset()
        value = v
    }

    // quietBand: Drift nur, solange |Abweichung| darunter bleibt (an die Rauschschwelle gekoppelt)
    fun update(m: Float, dtSeconds: Float, timestampNanos: Long, drift: Boolean, quietBand: Float) {
        if (m.isNaN()) return
        if (!settled) {
            if (startNanos < 0L) startNanos = timestampNanos
            sum += m
            count++
            if (timestampNanos - startNanos >= settleNanos) value = (sum / count).toFloat()
            return
        }
        if (drift && abs(m - value) < quietBand) value += (m - value) * (1f - exp(-dtSeconds / driftTau))
    }
}

// Groesste Abweichung (mit Vorzeichen) bleibt kurz stehen, dann folgt sie dem aktuellen Wert
class PeakHold(private val holdNanos: Long = 2_000_000_000L) {
    var value = Float.NaN
        private set
    private var at = 0L

    fun reset() {
        value = Float.NaN
    }

    fun update(dev: Float, timestampNanos: Long): Float {
        if (dev.isNaN()) return value
        if (value.isNaN() || abs(dev) >= abs(value)) {
            value = dev
            at = timestampNanos
        } else if (timestampNanos - at >= holdNanos) {
            value = dev
        }
        return value
    }
}

/**
 * Sensor am Anschlag. Kalibrierte Werte sind um den Hart-Eisen-Versatz verschoben (bis einige 100 µT),
 * die Rohgrenze maximumRange liegt also nicht genau bei |Achse| = maxRange. Darum zwei Zeichen:
 * eine Achse nahe 90 % von maxRange, oder eine grosse Achse, die exakt stehen bleibt (geclippt, kein Rauschen mehr).
 */
class SaturationWatch(private val stuckSamples: Int = 10, private val stuckMin: Float = 200f) {
    private val last = FloatArray(3) { Float.NaN }
    private val same = IntArray(3)

    fun reset() {
        last.fill(Float.NaN)
        same.fill(0)
    }

    fun update(x: Float, y: Float, z: Float, maxRange: Float): Boolean {
        var saturated = false
        val limit = maxRange * NEAR_LIMIT
        floatArrayOf(x, y, z).forEachIndexed { i, v ->
            same[i] = if (v == last[i] && abs(v) >= stuckMin) same[i] + 1 else 0
            last[i] = v
            if (same[i] >= stuckSamples || (maxRange > 0f && abs(v) >= limit)) saturated = true
        }
        return saturated
    }

    private companion object {
        const val NEAR_LIMIT = 0.9f
    }
}

// Status des Sensors (0 unzuverlaessig .. 3 hoch); niedrig erst nach Wartezeit, gut sofort
class AccuracyWatch(private val graceNanos: Long = 1_000_000_000L) {
    var low = false
        private set
    private var badSince = -1L

    fun reset() {
        low = false
        badSince = -1L
    }

    fun update(status: Int, timestampNanos: Long): Boolean {
        if (status > STATUS_LOW) {
            badSince = -1L
            low = false
        } else {
            if (badSince < 0L) badSince = timestampNanos
            if (timestampNanos - badSince >= graceNanos) low = true
        }
        return low
    }

    private companion object {
        const val STATUS_LOW = 1
    }
}
