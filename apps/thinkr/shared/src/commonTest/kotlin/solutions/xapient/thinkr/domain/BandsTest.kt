package solutions.xapient.thinkr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class BandsTest {
    private val b = Bands(listOf(1, 6, 10), listOf(2600.0, 2800.0, 3000.0), 16, 0.9)

    @Test fun modes() {
        assertEquals(1, b.modeStart(5))
        assertEquals(6, b.modeStart(6))
        assertEquals(10, b.modeStart(16))
        assertEquals(10, b.modeStart(99))
        assertEquals(5, b.modeEnd(1))
        assertEquals(9, b.modeEnd(6))
        assertEquals(16, b.modeEnd(10))
    }

    @Test fun levelFollowsDeadlineAndStaysNearMode() {
        assertEquals(3, b.levelOf(1, b.deadline(1, 3)))
        assertEquals(2, b.levelOf(1, b.deadline(1, 3) + 1))
        // hoechstens eine Stufe ueber das Ende der Spielart
        assertEquals(6, b.levelOf(1, 1.0))
        // hoechstens eine unter den Beginn
        assertEquals(5, b.levelOf(6, 99_999.0))
        assertEquals(16, b.levelOf(10, 1.0))
    }

    @Test fun warmUpNeverBelowModeStart() {
        assertEquals(1, b.warmUp(1))
        assertEquals(3, b.warmUp(5))
        assertEquals(6, b.warmUp(7))
        assertEquals(10, b.warmUp(11))
        assertEquals(14, b.warmUp(40))
    }

    @Test fun stairWithinBand() {
        val s = b.stairFor(6, 7)
        assertEquals(b.deadline(6, 7), s.value, 1e-9)
        assertTrue(s.min < b.deadline(6, 10))
        assertTrue(s.max > b.deadline(6, 6))
    }
}
