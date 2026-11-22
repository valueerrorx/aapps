package com.valueerrorx.nordr.domain

// Federmodell fuer Rose und Glas-Spiegelung; die UI zeichnet nur, was hier steht
class CompassPhysics {
    // Rose: fortlaufender Winkel (ohne Sprung bei 0/360), leicht unterdaempft = Nachschwingen
    val dial = SpringValue(stiffness = 50f, dampingRatio = 0.55f)

    // Spiegelung folgt der Neigung, ruhig und ohne Ueberschwingen
    val glintX = SpringValue(stiffness = 40f, dampingRatio = 0.8f)
    val glintY = SpringValue(stiffness = 40f, dampingRatio = 0.8f)

    private var initialized = false

    fun setTarget(r: CompassReading) {
        if (!r.valid) return
        if (!initialized) {
            dial.snapTo(r.heading)
            glintX.snapTo(r.tiltX); glintY.snapTo(r.tiltY)
            initialized = true
            return
        }
        // kuerzester Weg: 350 -> 10 dreht um 20, nicht um 340 zurueck
        dial.target = Angles.unwrap(dial.target, r.heading)
        glintX.target = r.tiltX
        glintY.target = r.tiltY
    }

    fun step(dt: Float) {
        dial.step(dt); glintX.step(dt); glintY.step(dt)
    }

    val isSettled: Boolean
        get() = dial.isSettled(0.05f, 0.1f) && glintX.isSettled(2e-3f, 1e-2f) && glintY.isSettled(2e-3f, 1e-2f)
}
