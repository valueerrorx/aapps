package solutions.xapient.dividr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SettleTest {
    private fun apply(bal: Map<Long, Long>, plan: List<Transfer>): Map<Long, Long> {
        val out = bal.toMutableMap()
        plan.forEach { t ->
            assertTrue(t.amount > 0)
            out[t.from] = out.getValue(t.from) + t.amount
            out[t.to] = out.getValue(t.to) - t.amount
        }
        return out
    }

    private fun check(bal: Map<Long, Long>): List<Transfer> {
        val plan = Settle.plan(bal)
        assertTrue(apply(bal, plan).values.all { it == 0L })
        return plan
    }

    @Test fun emptyAndSettled() {
        assertEquals(emptyList<Transfer>(), Settle.plan(emptyMap()))
        assertEquals(emptyList<Transfer>(), Settle.plan(mapOf(1L to 0L, 2L to 0L)))
    }

    @Test fun simpleTwo() {
        assertEquals(listOf(Transfer(2, 1, 2340)), check(mapOf(1L to 2340L, 2L to -2340L)))
    }

    @Test fun twoZeroSubgroupsSaveTransfers() {
        // {1,2,3} und {4,5,6} gleichen sich je fuer sich aus: 6 - 2 = 4 Ueberweisungen
        val bal = mapOf(1L to 500L, 2L to -300L, 3L to -200L, 4L to 70L, 5L to 30L, 6L to -100L)
        assertEquals(4, check(bal).size)
    }

    @Test fun deterministicRegardlessOfMapOrder() {
        val bal = linkedMapOf(5L to -100L, 1L to 250L, 3L to -150L, 2L to 40L, 4L to -40L)
        val shuffled = LinkedHashMap<Long, Long>().apply { bal.entries.reversed().forEach { put(it.key, it.value) } }
        assertEquals(Settle.plan(bal), Settle.plan(shuffled))
    }

    // Unabhaengige Referenz: groesste Zerlegung in Null-Teilgruppen ueber alle Mengenpartitionen
    private fun bruteMin(values: List<Long>): Int {
        val nz = values.filter { it != 0L }
        var best = 0
        fun go(i: Int, sums: MutableList<Long>) {
            if (i == nz.size) {
                if (sums.all { it == 0L }) best = maxOf(best, sums.size)
                return
            }
            for (k in sums.indices) { sums[k] += nz[i]; go(i + 1, sums); sums[k] -= nz[i] }
            sums += nz[i]; go(i + 1, sums); sums.removeAt(sums.size - 1)
        }
        go(0, mutableListOf())
        return nz.size - best
    }

    @Test fun optimalAgainstBruteForce() {
        val rnd = Random(7)
        repeat(300) {
            val n = 2 + rnd.nextInt(7)
            val v = MutableList(n - 1) { (rnd.nextInt(9) - 4) * 100L }
            v += -v.sum()
            val bal = v.mapIndexed { i, x -> (i + 1).toLong() to x }.toMap()
            assertEquals("case $v", bruteMin(v), check(bal).size)
        }
    }

    @Test fun greedyAbove14() {
        val rnd = Random(3)
        val v = MutableList(19) { (rnd.nextInt(2001) - 1000).toLong() }
        v += -v.sum()
        val bal = v.mapIndexed { i, x -> (i + 1).toLong() to x }.toMap()
        val plan = check(bal)
        assertTrue(plan.size <= bal.values.count { it != 0L } - 1)
    }

    @Test fun exactAt14IsFast() {
        val rnd = Random(11)
        val v = MutableList(13) { (rnd.nextInt(41) - 20) * 50L }
        v += -v.sum()
        val t = System.nanoTime()
        check(v.mapIndexed { i, x -> (i + 1).toLong() to x }.toMap())
        assertTrue((System.nanoTime() - t) < 2_000_000_000L)
    }
}
