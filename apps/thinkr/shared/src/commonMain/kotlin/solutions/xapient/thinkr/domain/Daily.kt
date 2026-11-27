package solutions.xapient.thinkr.domain

import kotlin.math.min
import kotlin.random.Random

// Tagestraining: deterministisch aus dem Datum, ohne Netz auf jedem Geraet gleich
object Daily {
    const val SIZE = 3

    // Montag, ab dem die Wochen verkettet werden (Tausch-Regel braucht die Vorwoche)
    const val ANCHOR = 20260105
    private const val MAX_WEEKS = 5_200L

    fun pick(day: Int, games: List<GameId>): List<GameId> {
        if (games.isEmpty()) return emptyList()
        val k = min(SIZE, games.size)
        val order = weekOrder(Days.monday(day), games, k)
        val w = Days.weekday(day)
        return List(k) { order[(w * k + it) % order.size] }
    }

    // Reihenfolge der Woche: Tag w nimmt die Plaetze w*k … w*k+k-1; Montag meidet die Spiele vom Sonntag davor
    internal fun weekOrder(monday: Int, games: List<GameId>, k: Int): List<GameId> {
        val weeks = (Days.epoch(monday) - Days.epoch(ANCHOR)) / 7
        // vor dem Anker oder bei absurder Uhr: ohne Verkettung
        if (weeks <= 0 || weeks > MAX_WEEKS) return shuffled(monday, games)
        var order = shuffled(ANCHOR, games)
        var m = ANCHOR
        repeat(weeks.toInt()) {
            val sunday = List(k) { order[(6 * k + it) % order.size] }.toSet()
            m = Days.add(m, 7)
            order = avoid(shuffled(m, games), sunday, k)
        }
        return order
    }

    private fun shuffled(monday: Int, games: List<GameId>): List<GameId> =
        games.shuffled(Random(monday.toLong() * 1_000_003L + 17L))

    // Spiele aus `bad` von den ersten k Plaetzen nach hinten tauschen, soweit moeglich
    private fun avoid(order: List<GameId>, bad: Set<GameId>, k: Int): List<GameId> {
        val o = order.toMutableList()
        for (i in 0 until k) {
            if (o[i] !in bad) continue
            val j = (k until o.size).firstOrNull { o[it] !in bad } ?: break
            o[i] = o[j].also { o[j] = o[i] }
        }
        return o
    }
}

// Serie ohne Druck: Tage mit erledigtem Tagestraining, eine Luecke je 7 Tage wird still ueberbrueckt
object Streak {
    const val BRIDGE_DAYS = 7

    fun count(done: Set<Int>, today: Int): Int {
        val set = done.map { Days.epoch(it) }.toSet()
        val t = Days.epoch(today)
        // heute noch offen zaehlt nicht als Luecke
        var d = if (t in set) t else t - 1
        var n = 0
        var lastGap: Long? = null
        while (true) {
            if (d in set) {
                n++
                d--
                continue
            }
            val bridge = (d - 1) in set && (lastGap == null || lastGap - d >= BRIDGE_DAYS)
            if (!bridge) return n
            lastGap = d
            d--
        }
    }

    // Montag … Sonntag der Woche von today: erledigt ja/nein
    fun week(done: Set<Int>, today: Int): List<Boolean> {
        val mon = Days.monday(today)
        return List(7) { Days.add(mon, it) in done }
    }
}
