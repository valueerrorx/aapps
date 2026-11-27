package solutions.xapient.thinkr.domain

// Tage als yyyymmdd (lokales Datum); proleptisch gregorianisch, ohne java.time
object Days {
    fun of(year: Int, month: Int, day: Int): Int = year * 10_000 + month * 100 + day

    fun add(day: Int, n: Int): Int = fromEpoch(epoch(day) + n)

    // Tage seit 1970-01-01 (wie LocalDate.toEpochDay)
    fun epoch(day: Int): Long {
        val y0 = day / 10_000
        val m = day / 100 % 100
        val d = day % 100
        require(valid(day)) { "kein Datum: $day" }
        val y = (if (m <= 2) y0 - 1 else y0).toLong()
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    fun fromEpoch(e: Long): Int {
        val z = e + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val y = (yoe + era * 400 + if (m <= 2) 1 else 0).toInt()
        return of(y, m, d)
    }

    // Montag der Woche
    fun monday(day: Int): Int = add(day, -weekday(day))

    // 0 = Montag … 6 = Sonntag (1970-01-01 war ein Donnerstag)
    fun weekday(day: Int): Int = (epoch(day) + 3).mod(7L).toInt()

    fun valid(day: Int): Boolean {
        if (day !in 19_000_101..99_991_231) return false
        val y = day / 10_000
        val m = day / 100 % 100
        val d = day % 100
        return m in 1..12 && d in 1..length(y, m)
    }

    private fun length(y: Int, m: Int): Int = when (m) {
        2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}
