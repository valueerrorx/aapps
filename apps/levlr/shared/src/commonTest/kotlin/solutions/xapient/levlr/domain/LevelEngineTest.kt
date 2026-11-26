package solutions.xapient.levlr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class LevelEngineTest {
    private fun LevelEngine.feed(v: Vec3, n: Int, turns: Int = 0, start: Long = 1L): Long {
        var t = start
        repeat(n) {
            onSample(v, t, turns)
            t += 20_000_000L
        }
        return t
    }

    @Test
    fun edgeReadingAndSnap() {
        val e = LevelEngine()
        e.feed(edgeSample(3.4f), 100)
        assertEquals(LevelMode.EDGE, e.reading.mode)
        assertEquals(3.4f, e.reading.edgeAngle, 0.05f)
        assertFalse(e.reading.isLevel)

        var snapped = 0
        var t = 3_000_000_000L
        repeat(100) {
            e.onSample(edgeSample(0.01f), t, 0)
            if (e.justLeveled) snapped++
            t += 20_000_000L
        }
        assertTrue(e.reading.isLevel)
        assertEquals(0f, e.reading.primary, 0f)
        assertEquals(1, snapped)
    }

    @Test
    fun flatReading() {
        val e = LevelEngine()
        e.feed(flatSample(2.1f, -1.3f), 100)
        assertEquals(LevelMode.FLAT, e.reading.mode)
        assertEquals(2.1f, e.reading.flatX, 0.05f)
        assertEquals(-1.3f, e.reading.flatY, 0.05f)
    }

    @Test
    fun stillPhoneStopsChangingReading() {
        val e = LevelEngine()
        var t = e.feed(edgeSample(1.234f), 200)
        // feines Rauschen unter dem Totband: keine Aenderung mehr melden
        var changes = 0
        repeat(100) { i ->
            val jitter = if (i % 2 == 0) 0.002f else -0.002f
            if (e.onSample(edgeSample(1.234f + jitter), t, 0)) changes++
            t += 20_000_000L
        }
        assertEquals(0, changes)
    }

    @Test
    fun calibrationIsApplied() {
        val bias = Bias(0.171f, 0f) // ~1°
        val e = LevelEngine(Calibration(flat = bias))
        e.feed(Vec3(bias.x, 0f, 9.81f), 100)
        assertTrue(e.reading.isLevel)
    }

    @Test
    fun landscapeEdgeUsesDisplayRotation() {
        val e = LevelEngine()
        e.feed(edgeSample(-90f + 2f), 100, turns = 1)
        assertEquals(0, e.reading.quadrant)
        assertEquals(2f, e.reading.edgeAngle, 0.05f)
    }
}
