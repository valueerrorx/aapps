package com.valueerrorx.nordr.domain

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

// Gedaempfte Feder, reines Kotlin; Kopie aus levlr (Domain bleibt ohne core/ui)
class SpringValue(
    initial: Float = 0f,
    var stiffness: Float = 120f,
    var dampingRatio: Float = 0.5f,
) {
    var value: Float = initial
        private set
    var velocity: Float = 0f
        private set
    var target: Float = initial

    fun snapTo(v: Float) {
        value = v; target = v; velocity = 0f
    }

    // Zustand direkt setzen, z. B. fuer Screenshots mitten in der Bewegung
    fun pose(value: Float, velocity: Float) {
        this.value = value; this.velocity = velocity
    }

    // Semi-implizites Euler mit kleinen Teilschritten, stabil auch bei Frame-Aussetzern
    fun step(dtSeconds: Float) {
        var left = min(dtSeconds, MAX_DT)
        val damping = 2f * dampingRatio * sqrt(stiffness)
        while (left > 0f) {
            val h = min(left, SUB_STEP)
            val acc = -stiffness * (value - target) - damping * velocity
            velocity += acc * h
            value += velocity * h
            left -= h
        }
    }

    fun isSettled(posEps: Float = 1e-3f, velEps: Float = 1e-3f): Boolean =
        abs(value - target) < posEps && abs(velocity) < velEps

    private companion object {
        const val SUB_STEP = 1f / 240f
        const val MAX_DT = 0.1f
    }
}
