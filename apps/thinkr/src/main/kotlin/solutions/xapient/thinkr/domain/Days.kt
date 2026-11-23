package solutions.xapient.thinkr.domain

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate

// Tage als yyyymmdd (lokales Datum)
object Days {
    fun of(d: LocalDate): Int = d.year * 10_000 + d.monthValue * 100 + d.dayOfMonth

    fun date(day: Int): LocalDate = LocalDate.of(day / 10_000, day / 100 % 100, day % 100)

    fun add(day: Int, n: Int): Int = of(date(day).plusDays(n.toLong()))

    fun epoch(day: Int): Long = date(day).toEpochDay()

    // Montag der Woche
    fun monday(day: Int): Int = of(date(day).with(DayOfWeek.MONDAY))

    // 0 = Montag … 6 = Sonntag
    fun weekday(day: Int): Int = date(day).dayOfWeek.value - 1

    fun valid(day: Int): Boolean = day in 19_000_101..99_991_231 && try {
        date(day); true
    } catch (_: DateTimeException) {
        false
    }
}
