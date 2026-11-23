package solutions.xapient.magnr.domain

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

// Mittel von B und |B|² der Rohwerte beim Einschwingen/Nullen: Grundwert fuer jeden Versatz neu rechenbar
class BaselineRef {
    private val sum = DoubleArray(4)
    private var n = 0
    val valid: Boolean get() = n > 0

    fun reset() {
        sum.fill(0.0)
        n = 0
    }

    fun add(x: Float, y: Float, z: Float) {
        if (x.isNaN() || y.isNaN() || z.isNaN()) return
        sum[0] += x
        sum[1] += y
        sum[2] += z
        sum[3] += x * x + y * y + z * z
        n++
    }

    // quadratisches Mittel von |B - o| ueber die Bezugswerte
    fun norm(ox: Float, oy: Float, oz: Float): Float {
        if (n == 0) return Float.NaN
        val q = (sum[3] - 2 * (sum[0] * ox + sum[1] * oy + sum[2] * oz)) / n + ox * ox + oy * oy + oz * oz
        return sqrt(q.coerceAtLeast(0.0)).toFloat()
    }

    fun save(): FloatArray = if (n == 0) FloatArray(SIZE) { Float.NaN } else FloatArray(SIZE) { (sum[it] / n).toFloat() }

    fun restore(s: FloatArray) {
        reset()
        if (s.size < SIZE || s.any { it.isNaN() }) return
        for (i in 0 until SIZE) sum[i] = s[i].toDouble()
        n = 1
    }

    companion object {
        const val SIZE = 4
    }
}

/**
 * Magnetfeld-Messer: Betrag glaetten, Grundwert, Abweichung, Ausschlag, Stufe, Spitze, Verlauf.
 * Erwartet kalibrierte Werte in µT; den Rest-Versatz schaetzt es aus Drehungen nach (onRotation), siehe SPEC.md.
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
    private val saturation = SaturationWatch()
    private val track = RotationTrack()
    private val offset = OffsetEstimator()
    // Rohwerte, aus denen der Grundwert entstand: Grundwert folgt einem neuen Versatz
    private val ref = BaselineRef()
    private val lastRaw = FloatArray(3) { Float.NaN }
    private var level = Level.QUIET
    private var lastNanos = 0L
    // Beginn der laufenden Pause in Sekunden seit Boot, NaN = misst
    private var pausedAt = Float.NaN

    var reading = Reading.EMPTY
        private set

    // Stufe beim letzten Wert gestiegen (fuer Haptik)
    var rose = false
        private set

    // Lage Handy -> Welt als Einheitsquaternion (Game Rotation Vector, ohne Magnetometer)
    fun onRotation(w: Float, x: Float, y: Float, z: Float, timestampNanos: Long) = track.add(w, x, y, z, timestampNanos)

    // x, y, z in µT; maxRange <= 0 = unbekannt
    fun onSample(x: Float, y: Float, z: Float, maxRange: Float, status: Int, timestampNanos: Long): Reading {
        val dt = if (lastNanos == 0L) 0f else ((timestampNanos - lastNanos) / 1e9f).coerceIn(0f, 0.5f)
        lastNanos = timestampNanos
        val o = offset.offset
        val before0 = o[0]
        val before1 = o[1]
        val before2 = o[2]
        val r = track.at(timestampNanos)
        if (r != null && offset.add(x, y, z, r, timestampNanos)) {
            // neuer Versatz: Glaettung und Grundwert in derselben Lage nachziehen, kein Sprung
            smooth.shift(norm(x, y, z, o) - norm(x, y, z, before0, before1, before2))
            if (baseline.settled && ref.valid) {
                baseline.shift(ref.norm(o[0], o[1], o[2]) - ref.norm(before0, before1, before2))
            }
        }
        val m = smooth.step(norm(x, y, z, o), dt)
        val settling = !baseline.settled
        if (settling) ref.add(x, y, z)
        // Drift nur in Ruhe: ein Fund wird nie zum Grundwert
        baseline.update(m, dt, timestampNanos, drift && level == Level.QUIET, sensitivity.quietBand)
        // eingeschwungen: Grundwert mit dem jetzigen Versatz aus allen Rohwerten der Einschwingzeit
        if (settling && baseline.settled) baseline.set(ref.norm(o[0], o[1], o[2]))
        val base = baseline.value
        val dev = m - base
        val s = sensitivity.strength(dev)
        val before = level
        level = if (base.isNaN()) Level.QUIET else Level.of(s, level)
        rose = level > before
        val saturated = saturation.update(x, y, z, maxRange)
        if (!base.isNaN()) {
            peak.update(dev, timestampNanos)
            history.add(s, timestampNanos)
        }
        lastRaw[0] = x
        lastRaw[1] = y
        lastRaw[2] = z
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

    // Nullen: aktueller Betrag wird Grundwert; ohne Messung (pausiert) neu einschwingen
    fun tare() {
        val m = smooth.value
        peak.reset()
        level = Level.QUIET
        if (m.isNaN()) {
            baseline.reset()
            ref.reset()
            reading = reading.copy(baseline = Float.NaN, peak = Float.NaN)
            return
        }
        baseline.set(m)
        ref.reset()
        ref.add(lastRaw[0], lastRaw[1], lastRaw[2])
        reading = reading.copy(baseline = m, dev = 0f, strength = 0f, level = Level.QUIET, peak = Float.NaN)
    }

    // Sensor aus: neue Zeitbasis, Glaettung frisch, Grundwert bleibt; nowSeconds = Uhr seit Boot (elapsedRealtime)
    fun pause(nowSeconds: Float) {
        if (pausedAt.isNaN()) pausedAt = nowSeconds
        lastNanos = 0L
        smooth.reset()
        peak.reset()
        accuracy.reset()
        saturation.reset()
        track.reset()
        offset.restart()
        lastRaw.fill(Float.NaN)
        history.pause()
        level = Level.QUIET
        rose = false
        reading = Reading(baseline = baseline.value, history = history.snapshot)
    }

    // Sensor wieder an: nach langer Pause (oder Neustart des Geraets) ist der Grundwert veraltet
    fun resume(nowSeconds: Float) {
        val away = nowSeconds - pausedAt
        if (!pausedAt.isNaN() && (away > STALE_SECONDS || away < 0f)) {
            baseline.reset()
            ref.reset()
            reading = reading.copy(baseline = Float.NaN)
        }
        pausedAt = Float.NaN
    }

    // Empfindlichkeit wechseln: Stufe aus der aktuellen Abweichung, ohne Steigen zu melden
    fun changeSensitivity(s: Sensitivity) {
        if (s == sensitivity) return
        sensitivity = s
        peak.reset()
        val dev = reading.dev
        level = if (dev.isNaN()) Level.QUIET else Level.of(s.strength(dev))
        rose = false
        reading = reading.copy(strength = s.strength(dev), level = level, peak = Float.NaN)
    }

    // Grundwert, Pausenbeginn, Bezugswert, Versatz-Schaetzung und Verlauf ueberstehen Prozess-Tod
    fun save(): FloatArray = floatArrayOf(baseline.value, pausedAt) + ref.save() + offset.save() + history.snapshot

    fun restore(state: FloatArray) {
        if (state.size < HEAD) return
        if (!state[0].isNaN()) baseline.set(state[0])
        pausedAt = state[1]
        ref.restore(state.copyOfRange(2, 2 + BaselineRef.SIZE))
        offset.restore(state.copyOfRange(2 + BaselineRef.SIZE, HEAD))
        history.restore(state.copyOfRange(HEAD, state.size))
        history.pause()
        reading = Reading(baseline = baseline.value, history = history.snapshot)
    }

    private companion object {
        const val SMOOTH_TAU = 0.12f
        // laenger weg: Handy steht vermutlich woanders, neu einschwingen
        const val STALE_SECONDS = 60f
        const val HEAD = 2 + BaselineRef.SIZE + OffsetEstimator.SIZE

        fun norm(x: Float, y: Float, z: Float, o: FloatArray) = norm(x, y, z, o[0], o[1], o[2])

        fun norm(x: Float, y: Float, z: Float, ox: Float, oy: Float, oz: Float): Float {
            val a = x - ox
            val b = y - oy
            val c = z - oz
            return sqrt(a * a + b * b + c * c)
        }
    }
}
