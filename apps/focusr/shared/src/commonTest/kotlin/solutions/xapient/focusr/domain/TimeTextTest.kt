package solutions.xapient.focusr.domain

import kotlin.test.assertEquals
import kotlin.test.Test

class TimeTextTest {
    @Test fun roundsUpToFullSeconds() {
        assertEquals("0:00", TimeText.clock(0))
        assertEquals("0:01", TimeText.clock(1))
        assertEquals("0:01", TimeText.clock(1000))
        assertEquals("0:02", TimeText.clock(1001))
        assertEquals("25:00", TimeText.clock(25 * Timer.MINUTE))
        assertEquals("17:42", TimeText.clock(17 * Timer.MINUTE + 41_500))
    }

    @Test fun hoursAboveSixtyMinutes() {
        assertEquals("59:59", TimeText.clock(3_599_000))
        assertEquals("1:00:00", TimeText.clock(3_600_000))
        assertEquals("2:00:00", TimeText.clock(Timer.MAX_MS))
        assertEquals("1:35:00", TimeText.clock(95 * Timer.MINUTE))
    }

    @Test fun negativeIsZero() {
        assertEquals("0:00", TimeText.clock(-5000))
    }
}
