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

    private fun feed(from: Double, to: Double, dt: Double = 1.0, speed: (Double) -> Float): List<Float> {
        val out = mutableListOf<Float>()
        var t = from
        while (t <= to + 1e-9) {
            out += f.update(Track.fix(t, speed = speed(t)))
            t += dt
        }
        return out
    }

    @Test fun startsUnknownThenStationary() {
        assertTrue(f.speed.isNaN())
        assertEquals(0f, f.update(Track.fix(0.0, speed = 0.2f)), 0f)
        assertFalse(f.moving)
    }

    // Rauschen bei 0-3 km/h: immer exakt 0, kein Zittern
    @Test fun noJitterWhileStanding() {
        val out = feed(0.0, 120.0, 0.5) { t -> (0.4 + 0.35 * sin(t * 1.7) + 0.1 * sin(t * 7.3)).toFloat().coerceAtLeast(0f) }
        assertTrue(out.all { it == 0f })
        assertFalse(f.moving)
    }

    // einzelner Ausreisser ueber der Startschwelle reicht nicht
    @Test fun singleSpikeIgnored() {
        f.update(Track.fix(0.0, speed = 0f))
        assertEquals(0f, f.update(Track.fix(1.0, speed = 3f)), 0f)
        assertEquals(0f, f.update(Track.fix(2.0, speed = 0.2f)), 0f)
        assertFalse(f.moving)
    }

    // Speed-Genauigkeit hebt die Rauschgrenze (bis 2 m/s)
    @Test fun poorSpeedAccuracyRaisesNoise() {
        repeat(5) { assertEquals(0f, f.update(Track.fix(it.toDouble(), speed = 1.5f, speedAcc = 1.8f)), 0f) }
        repeat(5) { f.update(Track.fix(5.0 + it, speed = 2.5f, speedAcc = 5f)) }
        assertTrue(f.moving)
    }

    @Test fun startsAfterTwoFixes() {
        f.update(Track.fix(0.0, speed = 0f))
        assertEquals(0f, f.update(Track.fix(1.0, speed = 1.4f)), 0f)
        assertTrue(f.update(Track.fix(2.0, speed = 1.4f)) > 1f)
        assertTrue(f.moving)
    }

    @Test fun stopsAfterTwoFixes() {
        feed(0.0, 10.0) { 10f }
        assertTrue(f.moving)
        f.update(Track.fix(11.0, speed = 0.3f))
        assertTrue(f.moving)
        assertEquals(0f, f.update(Track.fix(12.0, speed = 0.3f)), 0f)
        assertFalse(f.moving)
    }

    // exponentielle Glaettung mit tau = 0,8 s je Zeitabstand
    @Test fun smoothingFollowsTimeConstant() {
        feed(0.0, 20.0) { 10f }
        assertEquals(10f, f.speed, 1e-3f)
        val v = f.update(Track.fix(21.0, speed = 20f))
        val expected = 10f + (1 - exp(-1.0 / SpeedFilter.TAU)).toFloat() * 10f
        assertEquals(expected, v, 1e-3f)
        // halber Abstand, halber Schritt in der Zeitkonstante
        val w = f.update(Track.fix(21.5, speed = 20f))
        assertEquals(v + (1 - exp(-0.5 / SpeedFilter.TAU)).toFloat() * (20f - v), w, 1e-3f)
    }

    @Test fun smoothsNoiseWhileDriving() {
        val out = feed(0.0, 60.0, 0.5) { t -> (25 + 1.5 * sin(t * 3.1)).toFloat() }
        val tail = out.takeLast(60)
        assertTrue(tail.all { abs(it - 25f) < 1.0f })
    }

    // nach langer Luecke kein Nachziehen alter Werte
    @Test fun gapRestartsSmoothing() {
        feed(0.0, 10.0) { 10f }
        assertEquals(30f, f.update(Track.fix(20.0, speed = 30f)), 1e-3f)
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
        repeat(10) { f.update(Track.fix(6.0 + it, meters = 0.0, speed = 8f)) }
        assertEquals(8f, f.speed, 0.05f)
    }

    @Test fun reset() {
        feed(0.0, 10.0) { 10f }
        f.reset()
        assertTrue(f.speed.isNaN())
        assertFalse(f.moving)
    }
}
