package com.valueerrorx.spinr.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

// Zunge am Zeiger: ein Stift nimmt sie mit, danach schnellt sie zurueck. Wert -1..1, positiv = nach rechts
class Flapper(private val zoneDeg: Double = 7.0, private val releaseSeconds: Double = 0.045) {
    var value = 0.0
        private set
    private var contact = 0.0

    // Auslenkung allein aus der Lage des naechsten Stifts und der Drehrichtung
    fun contact(pegOffset: Double, direction: Int, segment: Double): Double {
        val zone = min(zoneDeg, segment * 0.45)
        val approach = -pegOffset * direction // > 0: Stift kommt noch auf die Zunge zu
        return if (approach in 0.0..zone) (1 - approach / zone) * direction else 0.0
    }

    // Stift drueckt sofort, das Zurueckschnellen klingt in releaseSeconds ab
    fun update(pegOffset: Double, direction: Int, segment: Double, dt: Double) {
        contact = contact(pegOffset, direction, segment)
        value = if (contact * value >= 0 && abs(contact) >= abs(value)) {
            contact
        } else {
            contact + (value - contact) * exp(-dt / releaseSeconds)
        }
        if (abs(value - contact) < 1e-3) value = contact
    }

    val isSettled: Boolean get() = value == contact

    // Fester Zustand, z. B. fuer Screenshots
    fun pose(v: Double) {
        value = v; contact = v
    }
}
