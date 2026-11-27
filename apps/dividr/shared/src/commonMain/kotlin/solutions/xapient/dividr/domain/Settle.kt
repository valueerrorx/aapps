package solutions.xapient.dividr.domain

data class Transfer(val from: Long, val to: Long, val amount: Long)

// Minimale Ueberweisungen: n - k, k = groesste Zahl disjunkter Null-Teilgruppen (exakt bis 14 Personen)
object Settle {
    const val EXACT_MAX = 14

    fun plan(balances: Map<Long, Long>): List<Transfer> {
        val open = balances.filterValues { it != 0L }.toList().sortedBy { it.first }
        if (open.isEmpty()) return emptyList()
        val groups = if (open.size <= EXACT_MAX) partition(open) else listOf(open)
        return groups.flatMap { greedy(it) }
    }

    // Teilmengen-DP: dp[mask] = max. Null-Gruppen; Rueckweg ergibt die Grenzen
    private fun partition(items: List<Pair<Long, Long>>): List<List<Pair<Long, Long>>> {
        val n = items.size
        val full = (1 shl n) - 1
        val sum = LongArray(1 shl n)
        val dp = IntArray(1 shl n)
        for (mask in 1..full) {
            val low = mask.countTrailingZeroBits()
            sum[mask] = sum[mask and (mask - 1)] + items[low].second
            var best = 0
            var m = mask
            while (m != 0) {
                val b = m and -m
                best = maxOf(best, dp[mask xor b])
                m = m xor b
            }
            dp[mask] = best + if (sum[mask] == 0L) 1 else 0
        }
        // Elemente in fester Reihenfolge abbauen; jede Null-Teilmenge schliesst eine Gruppe ab
        val groups = ArrayList<List<Pair<Long, Long>>>()
        var mask = full
        var prev = full
        while (mask != 0) {
            val bonus = if (sum[mask] == 0L) 1 else 0
            var i = 0
            while (i < n) {
                val b = 1 shl i
                if (mask and b != 0 && dp[mask xor b] + bonus == dp[mask]) break
                i++
            }
            mask = mask xor (1 shl i)
            if (sum[mask] == 0L) {
                val g = prev xor mask
                groups += (0 until n).filter { g and (1 shl it) != 0 }.map { items[it] }
                prev = mask
            }
        }
        return groups.sortedBy { g -> g.minOf { it.first } }
    }

    // Groesster Glaeubiger an groessten Schuldner, Gleichstand nach kleinerer ID
    private fun greedy(items: List<Pair<Long, Long>>): List<Transfer> {
        val bal = items.sortedBy { it.first }.toMap(LinkedHashMap())
        val out = ArrayList<Transfer>()
        while (true) {
            var cred: Long? = null
            var debt: Long? = null
            bal.forEach { (id, v) ->
                if (v > 0 && (cred == null || v > bal.getValue(cred!!))) cred = id
                if (v < 0 && (debt == null || v < bal.getValue(debt!!))) debt = id
            }
            val c = cred ?: break
            val d = debt ?: break
            val a = minOf(bal.getValue(c), -bal.getValue(d))
            out += Transfer(d, c, a)
            bal[c] = bal.getValue(c) - a
            bal[d] = bal.getValue(d) + a
        }
        return out
    }
}
