package solutions.xapient.roundr.domain

import kotlin.test.assertEquals
import kotlin.test.Test

class TimeFormatTest {
    @Test fun ceilSeconds() {
        assertEquals(20, TimeFormat.ceilSec(20_000))
        assertEquals(20, TimeFormat.ceilSec(19_001))
        assertEquals(1, TimeFormat.ceilSec(1))
        assertEquals(0, TimeFormat.ceilSec(0))
        assertEquals(0, TimeFormat.ceilSec(-300))
    }

    @Test fun formats() {
        assertEquals("20", TimeFormat.big(20))
        assertEquals("59", TimeFormat.big(59))
        assertEquals("1:00", TimeFormat.big(60))
        assertEquals("0:20", TimeFormat.clock(20))
        assertEquals("3:05", TimeFormat.clock(185))
        assertEquals("1:00:00", TimeFormat.clock(3600))
        assertEquals("2:01:09", TimeFormat.clock(7269))
    }
}
