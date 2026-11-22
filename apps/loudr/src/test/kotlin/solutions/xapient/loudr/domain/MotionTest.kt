package solutions.xapient.loudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test fun needleSnapsFirstThenFollowsWithoutOvershoot() {
        val n = Needle()
        assertEquals(40f, n.step(40f, 0.016f), 0f)
        var last = 40f
        repeat(120) {
            val v = n.step(80f, 1 / 60f)
            assertTrue("faellt zurueck", v >= last - 1e-4f)
            assertTrue("schwingt ueber: $v", v <= 80f + 1e-4f)
            last = v
        }
        assertEquals(80f, last, 0.05f)
    }

    @Test fun needleIsQuick() {
        val n = Needle()
        n.snap(40f)
        repeat(15) { n.step(80f, 1 / 60f) }
        // nach 0,25 s gut 95 % des Wegs
        assertTrue(n.value > 77f)
    }

    @Test fun needleSurvivesLongGapsAndNaN() {
        val n = Needle()
        n.snap(50f)
        assertTrue(n.step(70f, 10f).isFinite())
        assertTrue(n.step(Float.NaN, 0.01f).isNaN())
        assertEquals(30f, n.step(30f, 0.01f), 0f)
    }

    @Test fun holdKeepsValueForInterval() {
        val h = Hold<Int>(250_000_000L)
        assertEquals(1, h.update(0L, 1))
        assertEquals(1, h.update(100_000_000L, 2))
        assertEquals(3, h.update(250_000_000L, 3))
        assertEquals(3, h.update(300_000_000L, 4))
        assertEquals(5, h.update(310_000_000L, 5, force = true))
    }
}
