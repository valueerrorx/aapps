package solutions.xapient.nordr.domain

import kotlin.math.exp

enum class Accuracy { GOOD, LOW, INTERFERENCE }

// Bewertet Magnetometer-Status (0 = unzuverlaessig .. 3 = hoch) und Feldstaerke (µT).
// Schlecht erst nach einer Wartezeit, gut sofort; Feldgrenzen mit Hysterese
class AccuracyMonitor(
    private val graceSeconds: Float = 1f,
    private val minField: Float = 20f,
    private val maxField: Float = 70f,
    private val margin: Float = 3f,
) {
    var state: Accuracy = Accuracy.GOOD
        private set
    var field: Float = Float.NaN
        private set

    private var disturbed = false
    private var badSince = -1L
    private var lastNanos = 0L

    fun reset() {
        state = Accuracy.GOOD
        field = Float.NaN
        disturbed = false
        badSince = -1L
        lastNanos = 0L
    }

    // status: SensorManager.SENSOR_STATUS_* des Magnetometers
    fun update(magnetic: Vec3, status: Int, timestampNanos: Long): Accuracy {
        val dt = if (lastNanos == 0L) 0f else ((timestampNanos - lastNanos) / 1e9f).coerceIn(0f, 0.5f)
        lastNanos = timestampNanos
        val m = magnetic.length
        field = if (field.isNaN()) m else field + (m - field) * (1f - exp(-dt / FIELD_TAU))

        disturbed = if (disturbed) {
            field < minField + margin || field > maxField - margin
        } else {
            field < minField || field > maxField
        }
        val wanted = when {
            disturbed -> Accuracy.INTERFERENCE
            status <= STATUS_LOW -> Accuracy.LOW
            else -> Accuracy.GOOD
        }
        if (wanted == Accuracy.GOOD) {
            badSince = -1L
            state = Accuracy.GOOD
        } else {
            if (badSince < 0L) badSince = timestampNanos
            if (state != Accuracy.GOOD || (timestampNanos - badSince) / 1e9f >= graceSeconds) state = wanted
        }
        return state
    }

    private companion object {
        const val STATUS_LOW = 1
        const val FIELD_TAU = 0.4f
    }
}
