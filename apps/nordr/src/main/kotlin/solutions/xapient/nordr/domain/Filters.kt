package solutions.xapient.nordr.domain

import kotlin.math.abs
import kotlin.math.exp

// Tiefpass auf dem Kreis: folgt immer dem kuerzesten Weg, bei grosser Drehung schneller
class HeadingSmoother(
    private val tauSlow: Float = 0.25f,
    private val tauFast: Float = 0.06f,
    private val fastThreshold: Float = 10f,
) {
    var value: Float = Float.NaN
        private set

    fun reset() {
        value = Float.NaN
    }

    fun update(deg: Float, dtSeconds: Float): Float {
        if (deg.isNaN()) return value
        if (value.isNaN()) return Angles.normalize(deg).also { value = it }
        val d = Angles.delta(value, deg)
        val dt = dtSeconds.coerceIn(0f, 0.5f)
        val tau = if (abs(d) > fastThreshold) tauFast else tauSlow
        val alpha = 1f - exp(-dt / tau)
        return Angles.normalize(value + d * alpha).also { value = it }
    }
}

// Haelt den Winkel, bis die Aenderung die Schwelle erreicht; auch ueber 0/360 hinweg
class AngleDeadband(private val threshold: Float) {
    var value: Float = Float.NaN
        private set

    fun update(deg: Float): Float {
        if (deg.isNaN()) return value
        if (value.isNaN() || abs(Angles.delta(value, deg)) >= threshold) value = Angles.normalize(deg)
        return value
    }

    fun reset() {
        value = Float.NaN
    }
}
