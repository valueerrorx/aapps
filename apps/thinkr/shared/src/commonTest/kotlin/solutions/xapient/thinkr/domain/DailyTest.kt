package solutions.xapient.thinkr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class DailyTest {
    private val all = GameId.entries.toList()
    private val five = listOf(GameId.ECHO, GameId.SWITCHES, GameId.STARS, GameId.INK, GameId.SWARM)

    private fun days(from: Int, n: Int) = List(n) { Days.add(from, it) }

    @Test fun emptyRegistryNoTraining() {
        assertEquals(emptyList<GameId>(), Daily.pick(20261123, emptyList()))
    }

    @Test fun singleGameEveryDay() {
        days(20261101, 40).forEach { assertEquals(listOf(GameId.ECHO), Daily.pick(it, listOf(GameId.ECHO))) }
    }

    @Test fun deterministicAndDistinct() {
        for (games in listOf(five, all, all.take(2))) {
            days(20260101, 400).forEach { d ->
                val p = Daily.pick(d, games)
                assertEquals(p, Daily.pick(d, games))
                assertEquals(minOf(3, games.size), p.size)
                assertEquals(p.size, p.toSet().size)
                assertTrue(games.containsAll(p))
            }
        }
    }

    @Test fun nineGamesNeverTwoDaysInARow() {
        val ds = days(20260105, 3 * 365)
        ds.zipWithNext().forEach { (a, b) ->
            val overlap = Daily.pick(a, all).toSet() intersect Daily.pick(b, all).toSet()
            assertTrue(overlap.isEmpty(), "$a -> $b: $overlap")
        }
    }

    @Test fun nineGamesEachTwicePerWeek() {
        var monday = 20260105
        repeat(150) {
            val counts = days(monday, 7).flatMap { Daily.pick(it, all) }.groupingBy { it }.eachCount()
            all.forEach { g -> assertTrue((counts[g] ?: 0) >= 2, "$monday $g ${counts[g]}") }
            monday = Days.add(monday, 7)
        }
    }

    @Test fun fiveGamesEveryGameWeeklyAtMostOneRepeat() {
        var monday = 20260105
        repeat(100) {
            val week = days(monday, 7).map { Daily.pick(it, five) }
            assertEquals(five.toSet(), week.flatten().toSet())
            monday = Days.add(monday, 7)
        }
        days(20260105, 700).zipWithNext().forEach { (a, b) ->
            val overlap = Daily.pick(a, five).toSet() intersect Daily.pick(b, five).toSet()
            assertTrue(overlap.size <= 1, "$a -> $b")
        }
    }

    @Test fun datesBeforeAnchorStillWork() {
        assertEquals(3, Daily.pick(20200301, all).toSet().size)
    }
}
