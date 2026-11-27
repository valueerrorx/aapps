package solutions.xapient.thinkr

import androidx.compose.runtime.Composable

const val APP_NAME = "thinkr"

// Heute als yyyymmdd (lokale Zeitzone)
expect fun todayDay(): Int

// Datumszeile der Startseite nach Muster (CLDR), in Grossbuchstaben der Systemsprache
@Composable
expect fun formatDate(day: Int, pattern: String): String

// Wochentag als ein Buchstabe (M, D, M …)
@Composable
expect fun narrowWeekday(day: Int): String
