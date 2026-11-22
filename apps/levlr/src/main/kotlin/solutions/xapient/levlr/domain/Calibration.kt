package solutions.xapient.levlr.domain

import kotlin.math.hypot

// Sensor-Versatz in Geraetekoordinaten (m/s²), je Modus getrennt
data class Bias(val x: Float = 0f, val y: Float = 0f) {
    val isZero: Boolean get() = x == 0f && y == 0f
    fun asVec() = Vec3(x, y, 0f)
}

data class Calibration(val flat: Bias = Bias(), val edge: Bias = Bias()) {
    fun biasFor(mode: LevelMode) = if (mode == LevelMode.FLAT) flat else edge
    fun with(mode: LevelMode, bias: Bias) = if (mode == LevelMode.FLAT) copy(flat = bias) else copy(edge = bias)
}

// Mittelwert ueber eine ruhige Phase; Bewegung startet die Messung neu
class StableAverager(private val duration: Float = 1.2f, private val tolerance: Float = 0.15f) {
    private var sum = Vec3.ZERO
    private var count = 0
    private var elapsed = 0f
    private var anchor: Vec3? = null

    val progress: Float get() = (elapsed / duration).coerceIn(0f, 1f)
    val mean: Vec3 get() = if (count == 0) Vec3.ZERO else sum * (1f / count)
    val isDone: Boolean get() = elapsed >= duration

    fun reset() {
        sum = Vec3.ZERO; count = 0; elapsed = 0f; anchor = null
    }

    // false, wenn die Messung wegen Bewegung neu begonnen hat
    fun offer(sample: Vec3, dt: Float): Boolean {
        val a = anchor
        if (a != null && (sample - a).length > tolerance) {
            reset()
            anchor = sample
            return false
        }
        if (a == null) anchor = sample
        sum += sample; count++; elapsed += dt
        return true
    }
}

// Zwei-Punkt-Kalibrierung: messen, um 180° um die Bildschirmachse drehen, messen.
// Die Neigung der Unterlage hebt sich auf, uebrig bleibt der Versatz des Sensors.
// Der Modus wird bei der ersten Messung festgelegt.
class CalibrationSession {
    enum class Step { FIRST, SECOND, DONE }
    enum class Problem { NONE, WRONG_MODE, NOT_ROTATED, IMPLAUSIBLE }

    var mode: LevelMode? = null
        private set
    var step = Step.FIRST
        private set
    var measuring = false
        private set
    var problem = Problem.NONE
        private set
    var result: Bias? = null
        private set
    val progress: Float get() = if (measuring) averager.progress else 0f

    private val averager = StableAverager()
    private var first: Vec3? = null

    fun startMeasuring(currentMode: LevelMode) {
        if (step == Step.DONE) return
        if (step == Step.FIRST) mode = currentMode
        averager.reset()
        measuring = true
        problem = Problem.NONE
    }

    // raw = unkalibrierter Messwert in Geraetekoordinaten
    fun offer(raw: Vec3, dt: Float, currentMode: LevelMode) {
        if (!measuring) return
        if (currentMode != mode) {
            averager.reset()
            problem = Problem.WRONG_MODE
            return
        }
        if (problem == Problem.WRONG_MODE) problem = Problem.NONE
        averager.offer(raw, dt)
        if (!averager.isDone) return
        measuring = false
        val m = averager.mean
        when (step) {
            Step.FIRST -> {
                first = m
                step = Step.SECOND
            }
            Step.SECOND -> finish(first!!, m)
            Step.DONE -> Unit
        }
    }

    private fun finish(a: Vec3, b: Vec3) {
        // Kante: nach der Drehung muss die gegenueberliegende Kante unten sein
        if (mode == LevelMode.EDGE && a.x * b.x + a.y * b.y > 0f) {
            problem = Problem.NOT_ROTATED
            return
        }
        // Flach: deutlich schief gemessen, aber zweimal gleich -> nicht gedreht
        if (mode == LevelMode.FLAT && hypot(a.x, a.y) > FLAT_TILTED && hypot(a.x - b.x, a.y - b.y) < FLAT_SAME) {
            problem = Problem.NOT_ROTATED
            return
        }
        val bias = Bias((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        if (hypot(bias.x, bias.y) > MAX_BIAS) {
            problem = Problem.IMPLAUSIBLE
            return
        }
        result = bias
        step = Step.DONE
    }

    private companion object {
        // ~3,5° bei 9,81 m/s²; mehr ist kein Sensorfehler (inkl. Kamerabuckel), sondern eine falsche Drehung
        const val MAX_BIAS = 0.6f
        const val FLAT_TILTED = 0.3f
        const val FLAT_SAME = 0.1f
    }
}
