package com.valueerrorx.nordr.domain

import kotlin.math.abs

// Federmodell fuer Rose und Glas-Spiegelung; die UI zeichnet nur, was hier steht
class CompassPhysics {
    // Rose: fortlaufender Winkel (ohne Sprung bei 0/360), leicht unterdaempft = Nachschwingen
    val dial = SpringValue(stiffness = 50f, dampingRatio = 0.55f)

    // Spiegelung folgt der Neigung, ruhig und ohne Ueberschwingen
    val glintX = SpringValue(stiffness = 40f, dampingRatio = 0.8f)
    val glintY = SpringValue(stiffness = 40f, dampingRatio = 0.8f)

    private var initialized = false
    private var tiltInitialized = false

    fun setTarget(r: CompassReading) {
        if (!r.valid) return
        if (!initialized) {
            dial.snapTo(r.heading)
            initialized = true
            return
        }
        // kuerzester Weg: 350 -> 10 dreht um 20, nicht um 340 zurueck
        dial.target = Angles.unwrap(dial.target, r.heading)
    }

    // Welt-oben auf dem Bildschirm; Totband, damit Zittern die Physik nicht wach haelt. Rueckgabe: neues Ziel
    fun setTilt(x: Float, y: Float): Boolean {
        if (!tiltInitialized) {
            glintX.snapTo(x); glintY.snapTo(y)
            tiltInitialized = true
            return false
        }
        if (abs(x - glintX.target) < TILT_DEADBAND && abs(y - glintY.target) < TILT_DEADBAND) return false
        glintX.target = x
        glintY.target = y
        return true
    }

    fun step(dt: Float) {
        dial.step(dt); glintX.step(dt); glintY.step(dt)
    }

    val isSettled: Boolean
        get() = dial.isSettled(0.05f, 0.1f) && glintX.isSettled(2e-3f, 1e-2f) && glintY.isSettled(2e-3f, 1e-2f)

    private companion object {
        const val TILT_DEADBAND = 0.05f
    }
}
