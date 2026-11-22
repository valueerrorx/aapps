package solutions.xapient.magnr.domain

import kotlin.math.abs
import kotlin.math.sqrt

// Momentaufnahme fuer die Anzeige; history nur bei neuer Scheibe ein neues Array
data class Reading(
    val total: Float = Float.NaN,
    val baseline: Float = Float.NaN,
    val dev: Float = Float.NaN,
    val strength: Float = 0f,
    val level: Level = Level.QUIET,
    val peak: Float = Float.NaN,
    val history: FloatArray = FloatArray(0),
    val saturated: Boolean = false,
    val accuracyLow: Boolean = false,
) {
    val settling: Boolean get() = !total.isNaN() && baseline.isNaN()

    companion object {
        val EMPTY = Reading()
    }
}

// Verlauf: groesster Ausschlag je Scheibe, Luecke (NaN) nach einer Pause
class History(val slices: Int = SLICES, private val sliceNanos: Long = 100_000_000L) {
    private val buf = ArrayDeque<Float>()
    private var sliceStart = -1L
    private var current = Float.NaN
    private var gap = false
    var snapshot = FloatArray(0)
        private set

    fun restore(values: FloatArray) {
        buf.clear()
        values.forEach { buf.addLast(it) }
        while (buf.size > slices) buf.removeFirst()
        snapshot = buf.toFloatArray()
        sliceStart = -1L
        current = Float.NaN
    }

    // naechster Wert startet nach einer Luecke
    fun pause() {
        sliceStart = -1L
        current = Float.NaN
        gap = buf.isNotEmpty()
    }

    fun add(strength: Float, timestampNanos: Long) {
        if (sliceStart < 0L) {
            sliceStart = timestampNanos
            if (gap) push(Float.NaN)
            gap = false
        }
        while (timestampNanos - sliceStart >= sliceNanos) {
            push(if (current.isNaN()) strength else current)
            current = Float.NaN
            sliceStart += sliceNanos
        }
        current = if (current.isNaN()) strength else maxOf(current, strength)
    }

    private fun push(v: Float) {
        buf.addLast(v)
        while (buf.size > slices) buf.removeFirst()
        snapshot = buf.toFloatArray()
    }

    companion object {
        const val SLICES = 200
    }
}

/**
 * Magnetfeld-Messer: Betrag glaetten, Grundwert, Abweichung, Ausschlag, Stufe, Spitze, Verlauf.
 * Erwartet kalibrierte Werte in µT (Hart-Eisen-Versatz schon abgezogen), siehe SPEC.md.
 */
class FieldMeter(
    var sensitivity: Sensitivity = Sensitivity.MEDIUM,
    var drift: Boolean = true,
) {
    private val smooth = Smoother(SMOOTH_TAU)
    private val baseline = Baseline()
    private val peak = PeakHold()
    private val accuracy = AccuracyWatch()
    private val history = History()
    private var level = Level.QUIET
    private var lastNanos = 0L

    var reading = Reading.EMPTY
        private set

    // Stufe beim letzten Wert gestiegen (fuer Haptik)
    var rose = false
        private set

    // x, y, z in µT; maxRange <= 0 = unbekannt
    fun onSample(x: Float, y: Float, z: Float, maxRange: Float, status: Int, timestampNanos: Long): Reading {
        val dt = if (lastNanos == 0L) 0f else ((timestampNanos - lastNanos) / 1e9f).coerceIn(0f, 0.5f)
        lastNanos = timestampNanos
        val m = smooth.step(sqrt(x * x + y * y + z * z), dt)
        baseline.update(m, dt, timestampNanos, drift)
        val base = baseline.value
        val dev = m - base
        val s = sensitivity.strength(dev)
        val before = level
        level = if (base.isNaN()) Level.QUIET else Level.of(s, level)
        rose = level > before
        val limit = maxRange * SATURATION
        val saturated = maxRange > 0f && (abs(x) >= limit || abs(y) >= limit || abs(z) >= limit)
        if (!base.isNaN()) {
            peak.update(dev, timestampNanos)
            history.add(s, timestampNanos)
        }
        reading = Reading(
            total = m,
            baseline = base,
            dev = dev,
            strength = s,
            level = level,
            peak = peak.value,
            history = history.snapshot,
            saturated = saturated,
            accuracyLow = accuracy.update(status, timestampNanos),
        )
        return reading
    }

    // Nullen: aktueller Betrag wird Grundwert
    fun tare() {
        val m = smooth.value
        if (m.isNaN()) return
        baseline.set(m)
        peak.reset()
        level = Level.QUIET
        reading = reading.copy(baseline = m, dev = 0f, strength = 0f, level = Level.QUIET, peak = Float.NaN)
    }

    // Sensor aus: neue Zeitbasis, Glaettung frisch, Grundwert bleibt
    fun pause() {
        lastNanos = 0L
        smooth.reset()
        peak.reset()
        accuracy.reset()
        history.pause()
        level = Level.QUIET
        rose = false
        reading = Reading(baseline = baseline.value, history = history.snapshot)
    }

    // Empfindlichkeit wechseln: Ausschlag und Spitze neu
    fun changeSensitivity(s: Sensitivity) {
        if (s == sensitivity) return
        sensitivity = s
        peak.reset()
        level = Level.QUIET
    }

    // Grundwert und Verlauf ueberstehen Prozess-Tod
    fun save(): FloatArray = floatArrayOf(baseline.value) + history.snapshot

    fun restore(state: FloatArray) {
        if (state.isEmpty()) return
        if (!state[0].isNaN()) baseline.set(state[0])
        history.restore(state.copyOfRange(1, state.size))
        history.pause()
        reading = Reading(baseline = baseline.value, history = history.snapshot)
    }

    private companion object {
        const val SMOOTH_TAU = 0.12f
        const val SATURATION = 0.98f
    }
}
