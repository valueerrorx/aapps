package solutions.xapient.nordr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.math.abs

class FiltersTest {
    @Test
    fun smootherCrossesNorthTheShortWay() {
        val s = HeadingSmoother()
        s.update(359f, 0f)
        repeat(100) {
            val v = s.update(1f, 0.02f)
            // nie ueber Sueden zurueck: immer nahe Norden
            assertTrue(abs(Angles.delta(0f, v)) <= 1.01f, "Ausreisser $v")
        }
        assertEquals(1f, s.value, 0.01f)
    }

    @Test
    fun smootherDampsNoiseAroundNorth() {
        val s = HeadingSmoother()
        s.update(0f, 0f)
        var maxDev = 0f
        repeat(300) { i ->
            val raw = if (i % 2 == 0) 2f else 358f
            val v = s.update(raw, 0.02f)
            if (i > 50) maxDev = maxOf(maxDev, abs(Angles.delta(0f, v)))
        }
        assertTrue(maxDev < 0.3f, "Rest-Zittern $maxDev")
    }

    @Test
    fun smootherFollowsFastOnBigTurn() {
        val s = HeadingSmoother()
        s.update(0f, 0f)
        val v = s.update(90f, 0.02f)
        assertTrue(v > 20f)
    }

    @Test
    fun smootherIgnoresUndefinedHeading() {
        val s = HeadingSmoother()
        s.update(10f, 0f)
        assertEquals(10f, s.update(Float.NaN, 0.02f), 0f)
    }

    @Test
    fun deadbandWorksAcrossNorth() {
        val d = AngleDeadband(0.4f)
        assertEquals(359.9f, d.update(359.9f), 1e-4f)
        assertEquals(359.9f, d.update(0.2f), 1e-4f)
        assertEquals(0.4f, d.update(0.4f), 1e-4f)
        assertEquals(0.4f, d.update(Float.NaN), 1e-4f)
    }

    @Test
    fun springOnUnwrappedAngleTurnsShortWay() {
        val p = CompassPhysics()
        p.setTarget(CompassReading(valid = true, heading = 350f))
        p.setTarget(CompassReading(valid = true, heading = 10f))
        assertEquals(370f, p.dial.target, 1e-4f)
        var min = Float.MAX_VALUE
        var max = 0f
        repeat(300) {
            p.step(1f / 60f)
            min = minOf(min, p.dial.value)
            max = maxOf(max, p.dial.value)
        }
        assertTrue(min >= 350f, "dreht rueckwaerts: $min")
        assertTrue(max > 370.3f, "kein Nachschwingen: $max")
        assertEquals(370f, p.dial.value, 0.05f)
        assertTrue(p.isSettled)
    }

    @Test
    fun physicsIgnoresInvalidReading() {
        val p = CompassPhysics()
        p.setTarget(CompassReading(valid = false, heading = 123f))
        assertEquals(0f, p.dial.target, 0f)
        p.setTarget(CompassReading(valid = true, heading = 123f))
        assertEquals(123f, p.dial.value, 0f)
        assertTrue(p.isSettled)
    }
}
