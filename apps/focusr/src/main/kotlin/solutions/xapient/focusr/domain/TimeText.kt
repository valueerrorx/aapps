package solutions.xapient.focusr.domain

import java.util.Locale

// Restzeit als m:ss bzw. h:mm:ss, aufgerundet: 0:00 erst am Ende
object TimeText {
    fun seconds(ms: Long): Long = (ms.coerceAtLeast(0L) + 999L) / 1000L

    fun clock(ms: Long): String {
        val s = seconds(ms)
        val h = s / 3600
        val m = (s / 60) % 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(Locale.ROOT, "%d:%02d", m, sec)
    }

    // Ganze Minuten und Rest-Sekunden fuer die Ansage
    fun parts(ms: Long): Pair<Long, Long> {
        val s = seconds(ms)
        return s / 60 to s % 60
    }
}
