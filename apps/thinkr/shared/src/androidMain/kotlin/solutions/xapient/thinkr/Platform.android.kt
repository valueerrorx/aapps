package solutions.xapient.thinkr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

actual fun todayDay(): Int = LocalDate.now().let { it.year * 10_000 + it.monthValue * 100 + it.dayOfMonth }

private fun date(day: Int): LocalDate = LocalDate.of(day / 10_000, day / 100 % 100, day % 100)

@Composable
actual fun formatDate(day: Int, pattern: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(day, locale, pattern) { date(day).format(DateTimeFormatter.ofPattern(pattern, locale)).uppercase(locale) }
}

@Composable
actual fun narrowWeekday(day: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return date(day).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
}
