package solutions.xapient.aapps.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticGateTest {
    @Test fun firstEventAtTimeZeroPasses() {
        val g = HapticGate()
        assertTrue(g.allow(false, 0))
        assertFalse(g.allow(false, 60))
    }

    @Test fun ticksAreDebounced() {
        val g = HapticGate()
        assertTrue(g.allow(false, 1_000))
        assertFalse(g.allow(false, 1_050))
        assertFalse(g.allow(false, 1_119))
        assertTrue(g.allow(false, 1_120))
    }

    @Test fun fastWobbleBuzzesAtMostEvery120ms() {
        val g = HapticGate()
        // Ereignis alle 20 ms ueber 1 s
        assertEquals(9, (0 until 50).count { g.allow(false, it * 20L) })
    }

    @Test fun strongOvertakesFreshTickButNotItself() {
        val g = HapticGate()
        assertTrue(g.allow(false, 0))
        assertTrue(g.allow(true, 30))
        assertFalse(g.allow(true, 100))
        assertFalse(g.allow(false, 140))
        assertTrue(g.allow(false, 150))
    }
}
