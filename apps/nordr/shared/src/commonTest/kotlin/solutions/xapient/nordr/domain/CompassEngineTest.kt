package solutions.xapient.nordr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.math.abs

class CompassEngineTest {
    private var t = 1L

    // Haelt die Lage fuer einige Sekunden bei 50 Hz; zaehlt Aenderungen und Ticks
    private fun CompassEngine.hold(heading: Float, seconds: Float = 2f, noise: Float = 0f, pitch: Float = 0f): Pair<Int, Int> {
        var changes = 0
        var ticks = 0
        repeat((seconds * 50).toInt()) { i ->
            val jitter = if (i % 2 == 0) noise else -noise
            if (onRotation(Poses.pose(heading + jitter, pitch), t, 0)) changes++
            if (justCardinal) ticks++
            onMagnetic(Poses.magnetic(Poses.pose(heading, pitch)), 3, t)
            t += 20_000_000L
        }
        return changes to ticks
    }

    private fun near(expected: Float, actual: Float, eps: Float = 0.5f) =
        assertTrue(abs(Angles.delta(expected, actual)) < eps, "erwartet $expected, war $actual")

    @Test
    fun startsInvalidThenFollows() {
        val e = CompassEngine()
        assertFalse(e.reading.valid)
        e.hold(247f)
        assertTrue(e.reading.valid)
        near(247f, e.reading.heading)
        assertEquals(247, e.reading.whole)
        assertEquals(11, e.reading.point)
    }

    @Test
    fun stillPhoneStopsUpdating() {
        val e = CompassEngine()
        e.hold(120f, seconds = 2f)
        val (changes, _) = e.hold(120f, seconds = 2f, noise = 0.15f)
        assertEquals(0, changes)
    }

    @Test
    fun declinationGivesTrueNorth() {
        val e = CompassEngine()
        e.hold(358f)
        e.declination = 5f
        near(3f, e.reading.heading)
        near(358f, e.reading.magnetic)
        e.declination = -3f
        near(355f, e.reading.heading)
    }

    @Test
    fun courseIsKeptMagneticallyAndReportsDeviation() {
        val e = CompassEngine()
        e.hold(100f)
        e.setCourse()
        assertTrue(e.reading.onCourse)
        e.hold(110f)
        near(-10f, e.reading.deviation)
        assertFalse(e.reading.onCourse)
        // Wechsel auf rechtweisend: Kurs wandert mit, Abweichung bleibt
        e.declination = 4f
        near(104f, e.reading.course!!)
        near(-10f, e.reading.deviation)
    }

    @Test
    fun returningOnCourseFiresOnce() {
        val e = CompassEngine()
        e.hold(100f)
        e.setCourse()
        e.hold(120f)
        var fired = 0
        repeat(100) {
            e.onRotation(Poses.pose(100f), t, 0)
            if (e.justOnCourse) fired++
            // Magnetometer dazwischen darf das Ereignis nicht verschlucken
            e.onMagnetic(Poses.magnetic(Poses.pose(100f)), 3, t)
            if (e.justOnCourse) fired++
            t += 20_000_000L
        }
        assertEquals(1, fired)
        assertTrue(e.reading.onCourse)
    }

    @Test
    fun restoreAndClearCourse() {
        val e = CompassEngine()
        e.hold(30f)
        e.restoreCourse(200f)
        near(200f, e.reading.course!!)
        near(170f, e.reading.deviation)
        e.clearCourse()
        assertNull(e.reading.course)
        e.restoreCourse(Float.NaN)
        assertNull(e.reading.course)
    }

    @Test
    fun undeterminedHeadingKeepsLastValue() {
        val e = CompassEngine()
        e.hold(212f)
        // Bildschirm-rechts senkrecht: keine Richtung, Anzeige bleibt
        assertFalse(e.onRotation(Poses.pose(30f, 90f, 90f), t, 0))
        near(212f, e.reading.heading)
        assertTrue(e.reading.valid)
    }

    @Test
    fun courseNeedsValidHeading() {
        val e = CompassEngine()
        e.setCourse()
        assertNull(e.courseMagnetic)
    }

    @Test
    fun cardinalTickWhenTurningPastEast() {
        val e = CompassEngine()
        e.hold(70f)
        var ticks = 0
        for (h in 70..110) {
            e.onRotation(Poses.pose(h.toFloat()), t, 0)
            if (e.justCardinal) ticks++
            t += 20_000_000L
        }
        assertEquals(1, ticks)
    }

    @Test
    fun accuracyReachesReading() {
        val e = CompassEngine()
        e.hold(0f)
        repeat(100) {
            e.onMagnetic(Vec3(0f, 200f, 0f), 3, t)
            t += 20_000_000L
        }
        assertEquals(Accuracy.INTERFERENCE, e.reading.accuracy)
    }

    @Test
    fun uprightAndTiltReachReading() {
        val e = CompassEngine()
        e.hold(45f, pitch = 90f)
        near(45f, e.reading.heading)
        assertEquals(1f, e.upY, 0.03f)
    }

    @Test
    fun tiltAloneDoesNotChangeReading() {
        val e = CompassEngine()
        e.hold(80f)
        // nur nicken: Richtung bleibt, Anzeige bleibt
        var changes = 0
        for (pitch in 0..40) {
            if (e.onRotation(Poses.pose(80f, pitch.toFloat()), t, 0)) changes++
            t += 20_000_000L
        }
        assertEquals(0, changes)
        assertTrue(e.upY > 0.6f)
    }
}
