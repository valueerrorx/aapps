package com.valueerrorx.levlr.domain

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.tanh

// Federmodell fuer Blase und Fluessigkeit; die UI zeichnet nur, was hier steht
class LevelPhysics {
    // Kante: Blasenposition -1..1 entlang der Roehre, Oberflaeche in Bildschirmgrad
    val bubble = SpringValue(stiffness = 85f, dampingRatio = 0.5f)
    val surface = SpringValue(stiffness = 30f, dampingRatio = 0.2f)

    // Flach: Blasenposition im Kreis, Einheitskreis
    val bubbleX = SpringValue(stiffness = 70f, dampingRatio = 0.55f)
    val bubbleY = SpringValue(stiffness = 70f, dampingRatio = 0.55f)

    private var quadrant = 0
    private var initialized = false

    fun setTarget(r: LevelReading) {
        when (r.mode) {
            LevelMode.EDGE -> {
                // Blase wandert zur hoeheren Seite, also gegen die Drehung
                val b = -soft(r.edgeAngle, EDGE_SCALE)
                val s = -r.edgeAngle
                if (!initialized || r.quadrant != quadrant) {
                    bubble.snapTo(b); surface.snapTo(s)
                } else {
                    bubble.target = b; surface.target = s
                }
                quadrant = r.quadrant
            }
            LevelMode.FLAT -> {
                val total = hypot(r.flatX, r.flatY)
                val k = if (total < 1e-6f) 0f else soft(total, FLAT_SCALE) / total
                // Bildschirm-y zeigt nach unten: oben hoeher -> Blase nach oben
                val tx = r.flatX * k
                val ty = -r.flatY * k
                if (!initialized) {
                    bubbleX.snapTo(tx); bubbleY.snapTo(ty)
                } else {
                    bubbleX.target = tx; bubbleY.target = ty
                }
            }
        }
        initialized = true
    }

    fun step(dt: Float) {
        bubble.step(dt); surface.step(dt); bubbleX.step(dt); bubbleY.step(dt)
    }

    val isSettled: Boolean
        get() = bubble.isSettled(1e-3f, 1e-2f) && surface.isSettled(0.02f, 0.05f) &&
            bubbleX.isSettled(1e-3f, 1e-2f) && bubbleY.isSettled(1e-3f, 1e-2f)

    // Verformung der Blase durch Bewegung, 0..MAX_STRETCH
    val edgeStretch: Float get() = (abs(bubble.velocity) * 0.12f).coerceAtMost(MAX_STRETCH)
    val flatStretch: Float get() = (hypot(bubbleX.velocity, bubbleY.velocity) * 0.12f).coerceAtMost(MAX_STRETCH)

    // Welle auf der Oberflaeche, aus der Nachlauf-Geschwindigkeit
    val wave: Float get() = (surface.velocity * 0.018f).coerceIn(-1f, 1f)

    companion object {
        const val EDGE_SCALE = 7f
        const val FLAT_SCALE = 8f
        const val MAX_STRETCH = 0.35f

        // Weiche Saettigung: fein um 0, begrenzt an den Enden
        fun soft(angle: Float, scale: Float): Float = tanh(angle / scale)
    }
}
