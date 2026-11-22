package solutions.xapient.hudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

class SpeedFilterTest {
    private val f = SpeedFilter()
    // Position folgt der Geschwindigkeit, wie bei echter Fahrt
    private var pos = 0.0
    private var lastT = Double.NaN

    private fun at(t: Double, v: Float, acc: Float = 4f, speedAcc: Float = Float.NaN, moved: Boolean = true): Float {
        if (moved && !lastT.isNaN()) pos += v * (t - lastT)
        lastT = t
        return f.update(Track.fix(t, meters = pos, speed = v, acc = acc, speedAcc = speedAcc))
    }

    private fun feed(from: Double, to: Double, dt: Double = 1.0, speed: (Double) -> Float): List<Float> {
        val out = mutableListOf<Float>()
        var t = from
        while (t <= to + 1e-9) {
            out += at(t, speed(t))
            t += dt
        }
        return out
    }

    @Test fun startsUnknownThenStationary() {
        assertTrue(f.speed.isNaN())
        assertEquals(0f, at(0.0, 0.2f), 0f)
        assertFalse(f.moving)
    }

    // Rauschen bei 0-3 km/h ohne Verschiebung: immer exakt 0
    @Test fun noJitterWhileStanding() {
        var t = 0.0
        while (t <= 120.0) {
            val v = (0.4 + 0.35 * sin(t * 1.7) + 0.1 * sin(t * 7.3)).toFloat().coerceAtLeast(0f)
            assertEquals(0f, at(t, v, moved = false), 0f)
            t += 0.5
        }
        assertFalse(f.moving)
    }

    // Mehrwege-Echo im Stand: Doppler meldet Fahrt, Position bleibt -> keine Fahrt
    @Test fun multipathWithoutShiftIsStanding() {
        for (i in 0..30) assertEquals(0f, at(i.toDouble(), 2.5f, moved = false), 0f)
        assertFalse(f.moving)
    }

    // kurzer Ausreisser ueber der Startschwelle reicht nicht (unter 2 s)
    @Test fun shortBurstIgnored() {
        at(0.0, 0f)
        assertEquals(0f, at(1.0, 3f), 0f)
        assertEquals(0f, at(2.0, 3f), 0f)
        assertEquals(0f, at(3.0, 0.2f), 0f)
        assertFalse(f.moving)
    }

    // Start erst nach 2 s Bewegung und Verschiebung >= max(Genauigkeit, 5 m)
    @Test fun startNeedsTimeAndShift() {
        at(0.0, 0f)
        assertEquals(0f, at(1.0, 5f), 0f)
        assertEquals(0f, at(2.0, 5f), 0f)
        assertTrue(at(3.0, 5f) > 4f)
        assertTrue(f.moving)
    }

    // langsam: 2 s um, aber erst 4,2 m weg -> warten bis 5 m
    @Test fun slowStartWaitsForShift() {
        at(0.0, 0f)
        at(1.0, 1.4f)
        at(2.0, 1.4f)
        at(3.0, 1.4f)
        assertFalse(f.moving)
        at(4.0, 1.4f)
        assertTrue(f.moving)
    }

    // schlechte Positionsgenauigkeit (> 25 m): kein Fahrtbeginn
    @Test fun noStartWithPoorAccuracy() {
        for (i in 0..20) at(i.toDouble(), 10f, acc = 30f)
        assertFalse(f.moving)
        at(21.0, 10f)
        at(22.0, 10f)
        at(23.0, 10f)
        assertTrue(f.moving)
    }

    // Speed-Genauigkeit hebt die Rauschgrenze (bis 2 m/s) und macht unzuverlaessig
    @Test fun poorSpeedAccuracyRaisesNoise() {
        repeat(5) { assertEquals(0f, at(it.toDouble(), 1.5f, speedAcc = 1.8f), 0f) }
        assertTrue(f.reliable)
        repeat(5) { at(5.0 + it, 2.5f, speedAcc = 5f) }
        assertTrue(f.moving)
        assertFalse(f.reliable)
    }

    @Test fun stopsAfterTwoFixes() {
        feed(0.0, 10.0) { 10f }
        assertTrue(f.moving)
        at(11.0, 0.3f)
        assertTrue(f.moving)
        assertEquals(0f, at(12.0, 0.3f), 0f)
        assertFalse(f.moving)
        assertEquals(0f, f.accel, 0f)
    }

    // nach dem Halt wieder Verschiebung noetig
    @Test fun restartNeedsShiftFromStop() {
        feed(0.0, 10.0) { 10f }
        at(11.0, 0.3f)
        at(12.0, 0.3f)
        for (i in 13..20) at(i.toDouble(), 2f, moved = false)
        assertFalse(f.moving)
    }

    // Sprung ueber 10 m/s² wird verworfen, nach drei in Folge gilt er
    @Test fun spikeRejected() {
        feed(0.0, 10.0) { 10f }
        assertEquals(10f, at(11.0, 30f), 1e-3f)
        assertEquals(10f, at(12.0, 10f), 1e-3f)
        at(13.0, 30f)
        at(13.2, 30f)
        at(13.4, 30f)
        assertEquals(10f, f.speed, 1e-3f)
        assertTrue(at(13.6, 30f) > 20f)
    }

    // Beschleunigung geglaettet: gleichmaessig 1 m/s² ergibt ~1
    @Test fun accelerationTracksRamp() {
        feed(0.0, 30.0) { t -> (5 + t).toFloat() }
        assertEquals(1f, f.accel, 0.05f)
        feed(31.0, 60.0) { 36f }
        assertEquals(0f, f.accel, 0.05f)
    }

    // exponentielle Glaettung mit tau = 0,8 s je Zeitabstand
    @Test fun smoothingFollowsTimeConstant() {
        feed(0.0, 20.0) { 10f }
        assertEquals(10f, f.speed, 1e-3f)
        val v = at(21.0, 20f)
        val expected = 10f + (1 - exp(-1.0 / SpeedFilter.TAU)).toFloat() * 10f
        assertEquals(expected, v, 1e-3f)
        // halber Abstand, halber Schritt in der Zeitkonstante
        val w = at(21.5, 20f)
        assertEquals(v + (1 - exp(-0.5 / SpeedFilter.TAU)).toFloat() * (20f - v), w, 1e-3f)
    }

    @Test fun smoothsNoiseWhileDriving() {
        val out = feed(0.0, 60.0, 0.5) { t -> (25 + 1.5 * sin(t * 3.1)).toFloat() }
        assertTrue(out.takeLast(60).all { abs(it - 25f) < 1.0f })
    }

    // nach langer Luecke kein Nachziehen alter Werte
    @Test fun gapRestartsSmoothing() {
        feed(0.0, 10.0) { 10f }
        assertEquals(30f, at(20.0, 30f), 1e-3f)
    }

    @Test fun outOfOrderIgnored() {
        feed(0.0, 10.0) { 10f }
        assertEquals(10f, f.update(Track.fix(5.0, speed = 50f)), 1e-3f)
    }

    // ohne Doppler: Strecke ueber >= 2 s Basis
    @Test fun fallbackFromPositions() {
        var last = Float.NaN
        for (i in 0..12) last = f.update(Track.fix(i.toDouble(), meters = i * 15.0))
        assertTrue(f.moving)
        assertEquals(15f, last, 0.2f)
    }

    @Test fun fallbackNeedsBaseline() {
        assertTrue(f.update(Track.fix(0.0, 0.0)).isNaN())
        assertTrue(f.update(Track.fix(1.0, 15.0)).isNaN())
        assertFalse(f.update(Track.fix(2.0, 30.0)).isNaN())
    }

    // Positionsrauschen innerhalb der Genauigkeit = Stillstand
    @Test fun fallbackNoiseIsStanding() {
        val jitter = listOf(0.0, 3.0, -2.0, 4.0, -3.5, 1.0, 2.5, -1.0, 3.0, 0.5, -2.5, 3.5)
        jitter.forEachIndexed { i, m -> f.update(Track.fix(i.toDouble(), meters = m, acc = 6f)) }
        assertEquals(0f, f.speed, 0f)
        assertFalse(f.moving)
    }

    // Doppler kommt zurueck: Fenster verworfen
    @Test fun dopplerWins() {
        for (i in 0..5) f.update(Track.fix(i.toDouble(), meters = i * 15.0))
        repeat(10) { f.update(Track.fix(6.0 + it, meters = 90.0 + it * 8, speed = 8f)) }
        assertEquals(8f, f.speed, 0.05f)
    }

    @Test fun reset() {
        feed(0.0, 10.0) { 10f }
        f.reset()
        assertTrue(f.speed.isNaN())
        assertFalse(f.moving)
        assertEquals(0f, f.accel, 0f)
    }
}
