package com.valueerrorx.levlr.domain

import kotlin.math.abs
import kotlin.math.exp

// Tiefpass mit Zeitkonstante; bei grosser Bewegung schneller, damit die Anzeige nicht nachhinkt
class VectorSmoother(
    private val tauSlow: Float = 0.35f,
    private val tauFast: Float = 0.06f,
    private val fastThreshold: Float = 0.6f,
) {
    var value: Vec3? = null
        private set

    fun reset() {
        value = null
    }

    fun update(sample: Vec3, dtSeconds: Float): Vec3 {
        val prev = value ?: return sample.also { value = it }
        val dt = dtSeconds.coerceIn(0f, 0.5f)
        val tau = if ((sample - prev).length > fastThreshold) tauFast else tauSlow
        val alpha = 1f - exp(-dt / tau)
        return (prev + (sample - prev) * alpha).also { value = it }
    }
}

// Ignoriert Aenderungen unter der Schwelle, damit bei Stillstand nichts nachzittert
class Deadband(private val threshold: Float) {
    var value: Float = Float.NaN
        private set

    fun update(v: Float): Float {
        if (value.isNaN() || abs(v - value) >= threshold) value = v
        return value
    }

    fun reset() {
        value = Float.NaN
    }
}

// Ein-/Ausrasten mit Hysterese gegen Flattern an der Grenze
class SnapDetector(private val enter: Float = 0.05f, private val exit: Float = 0.1f) {
    var isLevel = false
        private set

    // true genau dann, wenn gerade eingerastet wurde
    fun update(absAngle: Float): Boolean {
        val was = isLevel
        isLevel = if (isLevel) absAngle <= exit else absAngle < enter
        return isLevel && !was
    }

    fun reset() {
        isLevel = false
    }
}

// Flach oder Kante, nach Gesamtneigung mit Hysterese
class ModeDetector(private val toEdge: Float = 50f, private val toFlat: Float = 35f) {
    var mode: LevelMode = LevelMode.FLAT
        private set
    private var initialized = false

    fun update(inclination: Float): LevelMode {
        mode = when {
            !initialized -> if (inclination < (toEdge + toFlat) / 2f) LevelMode.FLAT else LevelMode.EDGE
            mode == LevelMode.FLAT && inclination > toEdge -> LevelMode.EDGE
            mode == LevelMode.EDGE && inclination < toFlat -> LevelMode.FLAT
            else -> mode
        }
        initialized = true
        return mode
    }
}
