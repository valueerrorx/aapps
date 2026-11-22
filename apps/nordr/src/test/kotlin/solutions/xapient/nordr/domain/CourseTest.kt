package solutions.xapient.nordr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTest {
    @Test
    fun deviationSignAndWrap() {
        val c = CourseTracker()
        c.set(350f)
        assertEquals(-5f, c.deviation(355f), 1e-4f) // Kurs liegt links
        assertEquals(-15f, c.deviation(5f), 1e-4f)
        assertEquals(10f, c.deviation(340f), 1e-4f) // Kurs liegt rechts
    }

    @Test
    fun onCourseWithHysteresis() {
        val c = CourseTracker(enter = 2f, exit = 4f)
        c.set(100f)
        assertTrue(c.onCourse)
        assertFalse(c.update(103f)) // noch drin
        assertTrue(c.onCourse)
        assertFalse(c.update(105f))
        assertFalse(c.onCourse)
        assertFalse(c.update(103f)) // zu weit fuer das Einrasten
        assertTrue(c.update(101.5f))
        assertFalse(c.update(100f)) // meldet nicht doppelt
    }

    @Test
    fun clearedCourseIsQuiet() {
        val c = CourseTracker()
        c.set(10f)
        c.clear()
        assertNull(c.course)
        assertFalse(c.update(10f))
        assertEquals(0f, c.deviation(50f), 0f)
    }

    @Test
    fun cardinalFirstSampleIsSilent() {
        val d = CardinalDetector()
        assertFalse(d.update(0.5f))
        assertFalse(d.update(0.8f))
        assertFalse(d.update(10f))
        assertTrue(d.update(0.5f))
    }

    @Test
    fun cardinalFastSweepStillTicks() {
        val d = CardinalDetector()
        d.update(80f)
        assertTrue(d.update(100f))
        assertTrue(d.update(170f).not())
        assertTrue(d.update(190f))
    }

    @Test
    fun cardinalAcrossNorth() {
        val d = CardinalDetector()
        d.update(355f)
        assertTrue(d.update(3f))
    }

    @Test
    fun cardinalJitterTicksOnce() {
        val d = CardinalDetector()
        d.update(85f)
        var ticks = 0
        repeat(20) { i -> if (d.update(if (i % 2 == 0) 89.8f else 90.3f)) ticks++ }
        assertEquals(1, ticks)
    }

    @Test
    fun cardinalNoTickBetweenDirections() {
        val d = CardinalDetector()
        d.update(20f)
        assertFalse(d.update(40f))
        assertFalse(d.update(60f))
    }
}
