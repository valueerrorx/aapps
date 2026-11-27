package solutions.xapient.thinkr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.*
import solutions.xapient.thinkr.domain.Days

// Lokaler Tag: Sekunden seit 1970 plus Zonenversatz, ganze Tage abgerundet
actual fun todayDay(): Int {
    val now = NSDate()
    val offset = NSTimeZone.localTimeZone.secondsFromGMTForDate(now)
    val secs = now.timeIntervalSince1970.toLong() + offset
    return Days.fromEpoch(secs.floorDiv(86_400L))
}

// Mittag UTC des Tages; Formatierung ebenfalls in UTC, damit kein Tageswechsel durch die Zone entsteht
private fun noon(day: Int): NSDate = NSDate.dateWithTimeIntervalSince1970((Days.epoch(day) * 86_400L + 43_200L).toDouble())

private fun formatter(pattern: String): NSDateFormatter = NSDateFormatter().apply {
    locale = NSLocale.currentLocale
    timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    dateFormat = pattern
}

@Composable
actual fun formatDate(day: Int, pattern: String): String =
    remember(day, pattern) { formatter(pattern).stringFromDate(noon(day)).uppercase() }

@Composable
actual fun narrowWeekday(day: Int): String = remember(day) { formatter("EEEEE").stringFromDate(noon(day)) }
