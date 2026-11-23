package solutions.xapient.thinkr.domain.stars

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StarsGameTest {
    private val g = StarsGame

    // Spielzeit bis ins Abfragen des aktuellen Durchgangs
    private fun toRecall(s: StarsState): StarsState = g.tick(s, s.recallAt)

    private fun solve(s: StarsState, errors: Int = 0): StarsState {
        var x = toRecall(s)
        var t = x.round.now
        val empty = (0 until x.pass.side * x.pass.side).filter { it !in x.pass.stars }
        repeat(errors) { x = g.input(x, empty[it], ++t) }
        for (c in x.pass.stars) if (!x.pass.done) x = g.input(x, c, ++t)
        return x
    }

    @Test fun patternDistinctSortedAndInGrid() {
        val rng = Random(1)
        for (level in 1..StarsRules.MAX_LEVEL) {
            val side = StarsRules.side(level)
            repeat(50) {
                val p = Sky.pattern(rng, side, StarsRules.stars(level))
                assertEquals(StarsRules.stars(level), p.toSet().size)
                assertEquals(p.sorted(), p)
                assertTrue(p.all { it in 0 until side * side })
            }
        }
    }

    @Test fun gridGrowsWithStarsAndNeverOverfills() {
        var last = 0
        for (level in 1..StarsRules.MAX_LEVEL) {
            val side = StarsRules.side(level)
            assertTrue(side >= last)
            last = side
            // hoechstens ein Drittel der Punkte leuchtet: sonst raet man
            assertTrue("$level", StarsRules.stars(level) * 3 <= side * side)
        }
        assertEquals(4, StarsRules.side(1))
        assertEquals(7, StarsRules.side(StarsRules.MAX_LEVEL))
    }

    @Test fun linksFormSpanningTree() {
        val rng = Random(5)
        repeat(200) {
            val side = 4 + rng.nextInt(4)
            val n = 2 + rng.nextInt(side * side / 3)
            val cells = Sky.pattern(rng, side, n)
            val links = Sky.links(cells, side)
            assertEquals(n - 1, links.size)
            // zusammenhaengend: von der ersten Zelle aus alles erreichbar
            val seen = mutableSetOf(cells.first())
            links.forEach { (a, b) -> assertTrue(a in seen); seen += b }
            assertEquals(cells.toSet(), seen)
        }
    }

    @Test fun linksAreShortest() {
        // Reihe 0,1,2 und 3 weit weg: Baum verbindet Nachbarn
        val links = Sky.links(listOf(0, 1, 2, 15), 4)
        val total = links.sumOf { (a, b) -> Sky.dist(a, b, 4) }
        assertEquals(1.0 + 1.0 + Sky.dist(2, 15, 4), total, 1e-9)
    }

    @Test fun phasesFollowTime() {
        val s = g.start(3, 4, false)
        assertEquals(StarsPhase.LEAD, s.phase(0))
        assertEquals(StarsPhase.SHOW, s.phase(StarsRules.LEAD_MS))
        assertEquals(StarsPhase.RECALL, s.phase(StarsRules.LEAD_MS + StarsRules.showMs(4)))
        val relaxed = g.start(3, 4, true)
        assertEquals((StarsRules.showMs(4) * 1.5).toLong(), relaxed.recallAt - relaxed.showAt)
    }

    @Test fun tapsDuringShowDoNothing() {
        val s = g.tick(g.start(3, 1, false), StarsRules.LEAD_MS + 10)
        val t = g.input(s, s.pass.stars.first(), s.round.now)
        assertEquals(0, t.pass.taps.size)
        assertEquals(0, t.round.answered)
    }

    @Test fun perfectPassScoresAndLevelsUp() {
        val s = g.start(7, 3, false)
        val x = solve(s)
        assertTrue(x.pass.perfect)
        assertEquals(4, x.level)
        assertEquals(1, x.cheers)
        val n = StarsRules.stars(3)
        // 5 Sterne: Kette steigt nach 4 Treffern auf x2
        val taps = (0 until n).sumOf { StarsRules.POINTS * (1 + it / 4) }
        assertEquals(taps + StarsRules.PERFECT_BONUS * n * x.round.combo.mult, x.round.score)
        assertEquals(n, x.round.correct)
    }

    @Test fun oneErrorKeepsLevelTwoErrorsDropAndEndPass() {
        val one = solve(g.start(7, 5, false), errors = 1)
        assertTrue(one.pass.done)
        assertFalse(one.pass.perfect)
        assertEquals(5, one.level)
        assertEquals(0, one.cheers)

        val two = solve(g.start(7, 5, false), errors = 2)
        assertTrue(two.pass.done)
        assertEquals(0, two.pass.found)
        assertEquals(4, two.level)
        assertEquals(2, two.round.wrong)
    }

    @Test fun sameCellCountsOnce() {
        var x = toRecall(g.start(9, 2, false))
        val c = x.pass.stars.first()
        x = g.input(x, c, x.round.now + 1)
        x = g.input(x, c, x.round.now + 2)
        assertEquals(1, x.round.correct)
        assertEquals(1, x.pass.taps.size)
        // ausserhalb des Rasters
        x = g.input(x, 999, x.round.now + 3)
        assertEquals(1, x.pass.taps.size)
    }

    @Test fun nextPassAfterReveal() {
        val x = solve(g.start(7, 3, false))
        val end = x.pass.endAt
        assertEquals(0, g.tick(x, end + StarsRules.REVEAL_OK_MS - 1).pass.no)
        val n = g.tick(x, end + StarsRules.REVEAL_OK_MS)
        assertEquals(1, n.pass.no)
        assertEquals(4, n.pass.level)
        assertEquals(StarsRules.stars(4), n.pass.stars.size)
        assertEquals(end + StarsRules.REVEAL_OK_MS, n.pass.startAt)
        // Taps in der Aufloesung zaehlen nicht
        val r = g.input(x, 0, end + 10)
        assertEquals(x.round, r.round.copy(now = x.round.now))
    }

    @Test fun deterministicPerSeedAndPass() {
        assertEquals(g.newPass(4, 2, 6, 0).stars, g.newPass(4, 2, 6, 0).stars)
        assertTrue(g.newPass(4, 2, 6, 0).stars != g.newPass(4, 3, 6, 0).stars || g.newPass(4, 4, 6, 0).stars != g.newPass(4, 2, 6, 0).stars)
    }

    @Test fun perfectPlayerClimbsAndRoundEndsOnTime() {
        var x = g.start(1, 1, false)
        while (!x.round.isOver) {
            x = if (x.pass.done || x.phase() != StarsPhase.RECALL) g.tick(x, minOf(x.round.now + 50, x.round.durationMs)) else solve(x)
        }
        assertTrue(x.level >= 6)
        assertEquals(0, x.round.wrong)
        assertEquals(x.round.durationMs, x.round.now)
    }

    @Test fun warmUpAndLimits() {
        assertEquals(1, g.startLevel(1))
        assertEquals(4, g.startLevel(6))
        assertEquals(StarsRules.MAX_LEVEL - 2, g.startLevel(99))
        assertEquals(StarsRules.MAX_LEVEL, StarsRules.next(StarsRules.MAX_LEVEL, 0))
        assertEquals(1, StarsRules.next(1, 2))
    }
}
