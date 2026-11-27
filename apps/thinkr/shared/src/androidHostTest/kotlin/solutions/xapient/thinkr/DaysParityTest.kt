package solutions.xapient.thinkr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import solutions.xapient.thinkr.domain.Days
import java.time.LocalDate

// Eigene Datumsrechnung (fuer iOS) gleich wie java.time, das die App vorher nutzte
class DaysParityTest {
    private fun of(d: LocalDate) = d.year * 10_000 + d.monthValue * 100 + d.dayOfMonth

    @Test fun matchesJavaTime() {
        var d = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2200, 12, 31)
        while (!d.isAfter(end)) {
            val day = of(d)
            assertEquals(day.toString(), d.toEpochDay(), Days.epoch(day))
            assertEquals(day, Days.fromEpoch(d.toEpochDay()))
            assertEquals(day.toString(), d.dayOfWeek.value - 1, Days.weekday(day))
            assertEquals(of(d.with(java.time.DayOfWeek.MONDAY)), Days.monday(day))
            assertEquals(of(d.plusDays(37)), Days.add(day, 37))
            d = d.plusDays(1)
        }
    }

    @Test fun rejectsNonDates() {
        for (day in listOf(20260230, 20261301, 20260000, 20250229, 19000229, 18991231, 0, -1)) assertFalse("$day", Days.valid(day))
        assertEquals(true, Days.valid(20240229))
        assertEquals(true, Days.valid(20000229))
    }
}
