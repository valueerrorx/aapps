package solutions.xapient.thinkr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalTest {
    private val day = 20261123

    private fun r(id: GameId = GameId.ECHO, score: Int = 1000, level: Int = 4, relaxed: Boolean = false, d: Int = day) =
        RoundResult(d, id, score, level, 80, 60_000, relaxed)

    @Test fun recordUpdatesProgress() {
        val j = Journal().record(r(score = 1200, level = 5), listOf(GameId.ECHO))
        assertEquals(GameProgress(5, 1200, 1), j.progress(GameId.ECHO, false))
        val k = j.record(r(score = 800, level = 3), listOf(GameId.ECHO))
        // Stufe folgt der letzten Runde, Bestwert bleibt
        assertEquals(GameProgress(3, 1200, 2), k.progress(GameId.ECHO, false))
    }

    @Test fun relaxedHasOwnProgress() {
        val j = Journal().record(r(score = 2000, level = 7, relaxed = true), emptyList())
        assertEquals(GameProgress(), j.progress(GameId.ECHO, false))
        assertEquals(GameProgress(7, 2000, 1), j.progress(GameId.ECHO, true))
    }

    @Test fun dailyCompleteOnlyWhenAllPlayed() {
        val daily = listOf(GameId.ECHO, GameId.STARS)
        val a = Journal().record(r(GameId.ECHO), daily)
        assertFalse(day in a.dailyDone)
        assertEquals(setOf(GameId.ECHO), a.doneToday(day, daily))
        // Spiel ausserhalb des Tagestrainings zaehlt nicht
        val b = a.record(r(GameId.INK), daily)
        assertFalse(day in b.dailyDone)
        val c = b.record(r(GameId.STARS, relaxed = true), daily)
        assertTrue(day in c.dailyDone)
    }

    @Test fun yesterdaysRoundsDoNotCountToday() {
        val daily = listOf(GameId.ECHO)
        val j = Journal().record(r(d = day - 1), daily)
        assertEquals(emptySet<GameId>(), j.doneToday(day, daily))
    }

    @Test fun historyIsCapped() {
        var j = Journal()
        repeat(Journal.KEEP_ROUNDS + 20) { j = j.record(r(score = it), emptyList()) }
        assertEquals(Journal.KEEP_ROUNDS, j.history.size)
        assertEquals(20, j.history.first().score)
    }

    @Test fun oldDaysAreDropped() {
        val old = 20250101
        val j = Journal().record(r(d = old), listOf(GameId.ECHO))
        assertTrue(old in j.dailyDone)
        val k = j.record(r(d = day), listOf(GameId.ECHO))
        assertEquals(listOf(day), k.history.map { it.day })
        assertEquals(setOf(day), k.dailyDone)
    }

    @Test fun resetLevelKeepsBest() {
        val j = Journal()
            .record(r(score = 900, level = 8), emptyList())
            .record(r(score = 700, level = 6, relaxed = true), emptyList())
            .record(r(GameId.STARS, level = 5), emptyList())
            .resetLevel(GameId.ECHO)
        assertEquals(GameProgress(1, 900, 1), j.progress(GameId.ECHO, false))
        assertEquals(GameProgress(1, 700, 1), j.progress(GameId.ECHO, true))
        assertEquals(5, j.progress(GameId.STARS, false).level)
    }

    @Test fun keysAreStableAndUnique() {
        assertEquals(GameId.entries.size, GameId.entries.map { it.key }.toSet().size)
        GameId.entries.forEach { assertEquals(it, GameId.of(it.key)) }
        assertEquals(null, GameId.of("nope"))
    }
}
