package solutions.xapient.tunr.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SlidingWindowTest {
    // je Hop rutscht alles nach vorn, Neues steht am Ende, Skala 1/32768
    @Test fun shiftsByHop() {
        val w = SlidingWindow(size = 6, hop = 2)
        w.push(shortArrayOf(1, 2))
        w.push(shortArrayOf(3, 4))
        w.push(shortArrayOf(5, -32768))
        val exp = floatArrayOf(1f, 2f, 3f, 4f, 5f, -32768f).map { it / 32768f }.toFloatArray()
        assertArrayEquals(exp, w.data, 0f)
        w.push(shortArrayOf(7, 8))
        assertArrayEquals(floatArrayOf(3f, 4f, 5f, -32768f, 7f, 8f).map { it / 32768f }.toFloatArray(), w.data, 0f)
    }

    // Pegel nur ueber die juengsten Samples
    @Test fun levelUsesNewest() {
        val w = SlidingWindow(size = 4800, hop = 1200)
        val loud = Signals.toPcm(Signals.sine(440.0, 48_000, 1200, 0.5))
        repeat(3) { w.push(loud) }
        w.push(ShortArray(1200))
        assertEquals(-120f, w.levelDb(1200), 0f)
        // halb Ton (-9 dBFS), halb Stille: 3 dB weniger
        assertEquals(-12.04f, w.levelDb(2400), 0.1f)
    }

    // nur Nullen: Lauf waechst, ein Sample genuegt zum Zuruecksetzen
    @Test fun zeroRun() {
        val w = SlidingWindow(size = 8, hop = 4)
        repeat(3) { w.push(ShortArray(4)) }
        assertEquals(12L, w.zeroRun)
        w.push(shortArrayOf(0, 0, 1, 0))
        assertEquals(0L, w.zeroRun)
        w.push(ShortArray(4))
        assertEquals(4L, w.zeroRun)
    }
}
