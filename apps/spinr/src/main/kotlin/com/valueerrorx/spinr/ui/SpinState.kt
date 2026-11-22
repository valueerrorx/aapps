package com.valueerrorx.spinr.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.valueerrorx.spinr.domain.Flapper
import com.valueerrorx.spinr.domain.Spin
import com.valueerrorx.spinr.domain.Spinner
import com.valueerrorx.spinr.domain.WheelMath
import kotlin.math.abs
import kotlin.random.Random

// Zustand des Rads fuer Compose; alle Aufrufe auf dem Main-Thread
@Stable
class SpinState(labels: List<String> = emptyList(), private val random: Random = Random.Default) {
    var labels by mutableStateOf(labels)
        private set

    // Drehung im Uhrzeigersinn in Grad; Float fuer die Grafik, Double fuer die Rechnung
    var angle by mutableFloatStateOf(0f)
        private set
    private var exact = 0.0

    var speed by mutableFloatStateOf(0f)
        private set
    var flap by mutableFloatStateOf(0f)
        private set
    var spinning by mutableStateOf(false)
        private set

    // Index in labels, sobald das Rad nach einer Drehung steht
    var winner by mutableStateOf<Int?>(null)
        private set

    // Zaehler: Ergebnis-Ereignisse, Physik wecken
    var results by mutableIntStateOf(0)
        private set
    var wake by mutableIntStateOf(0)
        private set

    private var spin: Spin? = null
    private var elapsed = 0.0
    private var direction = 1
    private val flapper = Flapper()

    val canSpin: Boolean get() = labels.size >= 2 && !spinning
    val isMoving: Boolean get() = spinning || !flapper.isSettled

    // Segment unter dem Zeiger (nur Anzeige, liest den beobachtbaren Winkel)
    val current: Int get() = if (labels.isEmpty()) -1 else WheelMath.segmentAt(angle.toDouble(), labels.size)

    fun showLabels(new: List<String>) {
        if (new == labels) return
        labels = new
        winner = null
        spin = null
        spinning = false
        speed = 0f
        wake++
    }

    fun tap(): Boolean {
        if (!canSpin) return false
        start(Spinner.tap(WheelMath.normalize(exact), random))
        return true
    }

    // false: zu langsam, das Rad bleibt, wo die Hand es gelassen hat
    fun fling(velocity: Float): Boolean {
        if (!canSpin) return false
        val s = Spinner.fling(WheelMath.normalize(exact), velocity.toDouble(), random) ?: run { wake++; return false }
        start(s)
        return true
    }

    private fun start(s: Spin) {
        exact = s.start
        spin = s
        elapsed = 0.0
        direction = s.direction
        winner = null
        spinning = true
        wake++
    }

    // Von Hand drehen; Rueckgabe: ueberstrichene Stifte
    fun dragBy(delta: Float): Int {
        if (spinning || labels.size < 2 || delta == 0f) return 0
        winner = null
        direction = if (delta > 0) 1 else -1
        wake++
        return moveTo(exact + delta, 0.0)
    }

    // Ein Frame; Rueckgabe: ueberstrichene Stifte
    fun step(dt: Double): Int {
        val s = spin
        if (s == null) {
            if (labels.size >= 2) moveTo(exact, dt)
            return 0
        }
        elapsed += dt
        val ticks = moveTo(s.angleAt(elapsed), dt)
        speed = s.velocityAt(elapsed).toFloat()
        if (s.isDone(elapsed)) {
            spin = null
            spinning = false
            speed = 0f
            winner = WheelMath.segmentAt(s.end, labels.size)
            results++
        }
        return ticks
    }

    private fun moveTo(a: Double, dt: Double): Int {
        val n = labels.size
        if (n < 2) return 0
        val ticks = abs(WheelMath.tickIndex(a, n) - WheelMath.tickIndex(exact, n)).toInt()
        exact = a
        angle = a.toFloat()
        flapper.update(WheelMath.pegOffset(a, n), direction, WheelMath.segment(n), dt)
        flap = flapper.value.toFloat()
        return ticks
    }

    // Fester Zustand fuer Screenshots
    fun pose(angle: Float, speed: Float = 0f, flap: Float = 0f, winner: Int? = null) {
        exact = angle.toDouble()
        this.angle = angle
        this.speed = speed
        spinning = speed != 0f
        this.flap = flap
        flapper.pose(flap.toDouble())
        this.winner = winner
    }
}
