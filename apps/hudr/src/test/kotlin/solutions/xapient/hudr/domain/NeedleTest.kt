package solutions.xapient.hudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedleTest {
    @Test fun snapsFirstValue() {
        val n = Needle()
        assertTrue(n.settled)
        assertEquals(0.4f, n.step(0.4f, 0.016f), 0f)
        assertTrue(n.settled)
    }

    // kritisch gedaempft: monoton, nie ueber das Ziel, schlaeft ein
    @Test fun noOvershootAndSettles() {
        val n = Needle()
        n.snap(0f)
        var prev = 0f
        var frames = 0
        do {
            val v = n.step(1f, 1 / 60f)
            assertTrue(v >= prev - 1e-6f)
            assertTrue(v <= 1f)
            prev = v
            frames++
        } while (!n.settled && frames < 600)
        assertTrue("Bilder bis Ruhe: $frames", frames in 30..150)
        assertEquals(1f, n.value, 0f)
    }

    @Test fun movingIsNotSettled() {
        val n = Needle()
        n.snap(0f)
        n.step(0.5f, 1 / 60f)
        assertFalse(n.settled)
    }

    @Test fun bigFrameGapIsStable() {
        val n = Needle()
        n.snap(0f)
        repeat(50) { n.step(1f, 2f) }
        assertEquals(1f, n.value, 1e-3f)
    }
}
