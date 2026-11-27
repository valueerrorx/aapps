package solutions.xapient.focusr.domain

// Restzeit als m:ss bzw. h:mm:ss, aufgerundet: 0:00 erst am Ende
object TimeText {
    fun seconds(ms: Long): Long = (ms.coerceAtLeast(0L) + 999L) / 1000L

    fun clock(ms: Long): String {
        val s = seconds(ms)
        val h = s / 3600
        val m = (s / 60) % 60
        val sec = s % 60
        return if (h > 0) "$h:${two(m)}:${two(sec)}" else "$m:${two(sec)}"
    }

    private fun two(v: Long): String = v.toString().padStart(2, '0')
}
