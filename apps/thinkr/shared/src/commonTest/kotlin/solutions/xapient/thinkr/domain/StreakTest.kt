package solutions.xapient.thinkr.domain

import kotlin.test.assertEquals
import kotlin.test.Test

class StreakTest {
    private val today = 20261123 // Montag

    private fun back(vararg n: Int): Set<Int> = n.map { Days.add(today, -it) }.toSet()

    @Test fun nothingDone() {
        assertEquals(0, Streak.count(emptySet(), today))
    }

    @Test fun countsConsecutiveDays() {
        assertEquals(3, Streak.count(back(0, 1, 2), today))
    }

    @Test fun todayStillOpenKeepsStreak() {
        assertEquals(2, Streak.count(back(1, 2), today))
    }

    @Test fun singleGapIsBridged() {
        assertEquals(4, Streak.count(back(0, 1, 3, 4), today))
        // auch gestern verpasst, heute offen
        assertEquals(2, Streak.count(back(2, 3), today))
    }

    @Test fun twoMissedDaysInARowEndStreak() {
        assertEquals(2, Streak.count(back(0, 1, 4, 5), today))
        assertEquals(0, Streak.count(back(3, 4), today))
    }

    @Test fun secondGapWithinAWeekEndsStreak() {
        // Luecken an Tag 2 und Tag 5: die zweite liegt nur 3 Tage dahinter
        assertEquals(4, Streak.count(back(0, 1, 3, 4, 6, 7), today))
    }

    @Test fun gapsAWeekApartAreBothBridged() {
        val done = (0..20).filter { it != 2 && it != 9 }.let { back(*it.toIntArray()) }
        assertEquals(19, Streak.count(done, today))
    }

    @Test fun weekStartsMonday() {
        val wed = 20261125
        val done = setOf(20261123, 20261125, 20261122)
        assertEquals(listOf(true, false, true, false, false, false, false), Streak.week(done, wed))
    }
}
