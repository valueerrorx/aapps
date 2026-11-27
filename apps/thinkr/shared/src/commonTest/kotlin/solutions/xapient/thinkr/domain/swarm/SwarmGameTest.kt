package solutions.xapient.thinkr.domain.swarm

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class SwarmGameTest {
    private val g = SwarmGame

    @Test fun formationsHaveCenterFirstAndNoOverlap() {
        for (f in Formation.entries) for (n in Formations.SIZES) {
            val s = Formations.spots(f, n)
            assertEquals(n, s.size, "$f $n")
            assertEquals(Spot(0f, 0f), s[0])
            for (i in s.indices) for (j in i + 1 until s.size) {
                assertTrue(hypot(s[i].x - s[j].x, s[i].y - s[j].y) >= 1f, "$f $n $i $j")
            }
        }
    }

    @Test fun answersSpreadEvenlyWithoutLongRuns() {
        for (seed in 1L..20L) {
            val d = SwarmDeck.deal(Random(seed), 400, 5, seed % 2 == 0L)
            val counts = d.groupingBy { it.answer }.eachCount()
            Dir.entries.forEach { assertTrue((counts[it] ?: 0) in 70..130, "$seed $it ${counts[it]}") }
            var run = 1
            d.zipWithNext().forEach { (a, b) ->
                run = if (a.answer == b.answer) run + 1 else 1
                assertTrue(run <= SwarmRules.MAX_RUN)
                assertTrue(a.formation != b.formation)
                assertTrue(hypot(a.x - b.x, a.y - b.y) >= 0.12f)
            }
        }
    }

    @Test fun incongruentShareGrowsWithLevel() {
        for (level in listOf(1, 6, 9, 16)) {
            val d = SwarmDeck.deal(Random(level.toLong()), 2000, level, false)
            val share = d.count { !it.congruent }.toDouble() / d.size
            assertTrue(abs(share - SwarmRules.incongruent(level)) < 0.05, "$level $share")
        }
        assertEquals(0.3, SwarmRules.incongruent(1), 1e-9)
        assertEquals(0.7, SwarmRules.incongruent(SwarmRules.MAX_LEVEL), 1e-9)
    }

    @Test fun sizeAndJitterGrow() {
        assertEquals(5, SwarmRules.size(1))
        assertEquals(7, SwarmRules.size(5))
        assertEquals(9, SwarmRules.size(12))
        assertTrue(SwarmRules.jitter(1) < SwarmRules.jitter(9))
        assertTrue(g.start(1, 12, false).flocks.all { it.size == 9 })
    }

    @Test fun hollowRuleOnlyFromLevelTen() {
        val early = g.start(3, 9, false)
        assertFalse(early.hollowRule)
        assertTrue(early.flocks.all { !it.hollow && it.drift == null })
        val late = g.start(3, 11, false)
        assertTrue(late.hollowRule)
        val hollow = late.flocks.count { it.hollow }.toDouble() / late.flocks.size
        assertTrue(abs(hollow - SwarmRules.HOLLOW_SHARE) < 0.08, "$hollow")
        assertTrue(late.flocks.all { it.drift != null })
        // die Regel zaehlt: oft zeigt der Mittlere anders, als der Schwarm fliegt
        assertTrue(late.flocks.count { it.drift != it.point } > late.flocks.size / 2)
    }

    @Test fun answerFollowsRule() {
        val filled = Flock(Dir.UP, Dir.LEFT, 5, Formation.LINE, 0.5f, 0.5f, hollow = false, drift = Dir.RIGHT)
        assertEquals(Dir.UP, filled.answer)
        assertEquals(Dir.RIGHT, filled.copy(hollow = true).answer)
        assertFalse(filled.congruent)
    }

    @Test fun rightSwipeScoresWrongResets() {
        var s = g.start(5, 2, false)
        repeat(4) { s = g.input(s, s.flock.answer, s.shownAt + 300) }
        assertEquals(4 * SwarmRules.POINTS, s.round.score)
        assertEquals(2, s.round.combo.mult)
        val wrong = Dir.entries.first { it != s.flock.answer }
        s = g.input(s, wrong, s.shownAt + 300)
        assertEquals(1, s.round.combo.mult)
        assertEquals(wrong, s.last!!.said)
        assertFalse(s.last!!.correct)
    }

    @Test fun deadlineExpiresAsMiss() {
        val s = g.start(5, 3, false)
        assertEquals(0, g.tick(s, s.dueAt - 1).index)
        val t = g.tick(s, s.dueAt)
        assertEquals(1, t.index)
        assertNull(t.last!!.said)
        assertEquals(s.dueAt + SwarmRules.GAP_MS, t.shownAt)
    }

    @Test fun inputDuringGapIgnored() {
        var s = g.start(5, 3, false)
        s = g.input(s, Dir.UP, 300)
        val t = g.input(s, Dir.UP, 350)
        assertEquals(s.index, t.index)
        assertEquals(s.round.answered, t.round.answered)
    }

    @Test fun levelAndWarmUp() {
        var s = g.start(8, 4, false)
        repeat(40) { s = g.input(s, s.flock.answer, s.shownAt + 250) }
        assertTrue(s.level > 4)
        assertTrue(s.level <= SwarmRules.HOLLOW)
        assertEquals(1, g.startLevel(2))
        assertEquals(SwarmRules.HOLLOW, g.startLevel(11))
        assertEquals(12, g.startLevel(14))
    }

    @Test fun roundEndsAfterSixtySecondsAndRelaxedIsSlower() {
        val s = g.tick(g.start(2, 1, false), 61_000)
        assertTrue(s.round.isOver)
        assertEquals(0, s.round.score)
        assertTrue(abs((g.start(2, 1, false).deadlineMs * 1.5).toLong() - g.start(2, 1, true).deadlineMs) <= 1)
    }

    @Test fun deterministicPerSeed() {
        assertEquals(g.start(77, 6, false).flocks, g.start(77, 6, false).flocks)
    }
}
