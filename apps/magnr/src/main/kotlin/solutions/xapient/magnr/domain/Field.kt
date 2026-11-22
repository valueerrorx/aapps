package solutions.xapient.magnr.domain

import kotlin.math.abs
import kotlin.math.exp

// Vollausschlag und Rauschschwelle in µT; Vollausschlag durch 4 teilbar fuer runde Skalenwerte
enum class Sensitivity(val fullScale: Float, val noise: Float) {
    LOW(100f, 3f),
    MEDIUM(40f, 0.9f),
    HIGH(12f, 0.6f);

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
    private val quietBand: Float = 2f,
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

    fun update(m: Float, dtSeconds: Float, timestampNanos: Long, drift: Boolean) {
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
