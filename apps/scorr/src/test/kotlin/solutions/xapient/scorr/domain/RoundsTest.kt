package solutions.xapient.scorr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundsTest {
    private fun three() = Game(listOf(Player("A", 0), Player("B", 1), Player("C", 2)), rounds = true)

    // Zuege im Abstand > Zusammenfass-Fenster
    private var t = 0L
    private fun Game.play(vararg moves: Pair<Int, Int>): Game {
        var g = this
        moves.forEach { (p, d) -> t += 10_000; g = g.score(p, d, t) }
        return g
    }

    @Test fun roundPointsStartAtZeroAndReset() {
        val g = three().play(0 to 5, 1 to 3)
        assertEquals(listOf(5, 3, 0), g.roundPoints)
        assertEquals(listOf(5, 3, 0), g.tileValues)
        val n = g.endRound()
        assertEquals(listOf(0, 0, 0), n.roundPoints)
        assertEquals(listOf(0, 0, 0), n.tileValues)
        assertEquals(listOf(5, 3, 0), n.scores)
        val m = n.play(1 to 4)
        assertEquals(listOf(0, 4, 0), m.roundPoints)
        assertEquals(listOf(5, 7, 0), m.scores)
    }

    @Test fun withoutRoundsTileShowsTotal() {
        val g = three().withRounds(false).play(0 to 5).endRound().play(0 to 2)
        assertEquals(listOf(7, 0, 0), g.tileValues)
        assertEquals(g.scores, g.standings)
    }

    @Test fun roundWinnerMostAndFewest() {
        val g = three().play(0 to 5, 1 to 3, 2 to 4).endRound()
        assertEquals(listOf(setOf(0)), g.roundWinners)
        assertEquals(listOf(1, 0, 0), g.wins)
        val f = g.withRule(WinRule.FEWEST)
        assertEquals(listOf(setOf(1)), f.roundWinners)
        assertEquals(listOf(0, 1, 0), f.wins)
    }

    @Test fun fewestWithZeroWins() {
        // Uno: wer nichts hat, gewinnt die Runde
        val g = three().withRule(WinRule.FEWEST).play(0 to 12, 1 to 7).endRound()
        assertEquals(listOf(setOf(2)), g.roundWinners)
    }

    @Test fun tieAtTopGivesAllTheWin() {
        val g = three().play(0 to 4, 1 to 4, 2 to 1).endRound()
        assertEquals(listOf(1, 1, 0), g.wins)
        val f = three().withRule(WinRule.FEWEST).play(0 to 2, 1 to 9, 2 to 2).endRound()
        assertEquals(listOf(1, 0, 1), f.wins)
    }

    @Test fun zeroRoundHasNoWinner() {
        val g = three().endRound().play(0 to 3, 0 to -3).endRound()
        assertEquals(listOf(emptySet<Int>(), emptySet()), g.roundWinners)
        assertEquals(listOf(0, 0, 0), g.wins)
        assertTrue(three().pendingRoundWinners.isEmpty())
        assertEquals(setOf(1), three().play(1 to 2).pendingRoundWinners)
    }

    @Test fun runningRoundDoesNotCountAsWin() {
        val g = three().play(0 to 9)
        assertEquals(listOf(0, 0, 0), g.wins)
        assertTrue(g.leaders.isEmpty())
        assertEquals(setOf(0), g.endRound().leaders)
    }

    @Test fun targetInWinsIsCheckedAtRoundEnd() {
        var g = three().withTarget(2).play(0 to 5).endRound()
        assertFalse(g.finished)
        g = g.play(0 to 3, 1 to 1)
        assertFalse(g.finished)
        g = g.endRound()
        assertTrue(g.finished)
        assertEquals(setOf(0), g.winners)
        // nach Spielende zeigt die Kachel die Siege
        assertEquals(listOf(2, 0, 0), g.tileValues)
    }

    @Test fun winsAlwaysCountMostEvenWithFewestRule() {
        val g = three().withRule(WinRule.FEWEST).withTarget(2)
            .play(0 to 1, 1 to 5, 2 to 5).endRound()
            .play(0 to 2, 1 to 6, 2 to 9).endRound()
        assertTrue(g.finished)
        assertEquals(setOf(0), g.winners)
    }

    @Test fun targetInPointsIsCheckedRightAway() {
        val g = three().withScoring(Scoring.POINTS).withTarget(10).play(0 to 6).endRound().play(0 to 4)
        assertTrue(g.finished)
        assertEquals(setOf(0), g.winners)
        assertEquals(listOf(10, 0, 0), g.tileValues)
    }

    @Test fun leaderFollowsScoring() {
        // B gewinnt zwei Runden knapp, A eine hoch
        val g = three()
            .play(0 to 30, 1 to 1).endRound()
            .play(0 to 1, 1 to 2).endRound()
            .play(0 to 1, 1 to 2).endRound()
        assertEquals(listOf(1, 2, 0), g.wins)
        assertEquals(setOf(1), g.leaders)
        assertEquals(setOf(0), g.withScoring(Scoring.POINTS).leaders)
    }

    @Test fun switchingScoringRecomputesAndKeepsEverything() {
        val g = three().withTarget(2)
            .play(0 to 30, 1 to 1).endRound()
            .play(0 to 1, 1 to 2).endRound()
            .play(0 to 1, 1 to 2).endRound()
        assertTrue(g.finished)
        assertEquals(setOf(1), g.winners)
        val p = g.withScoring(Scoring.POINTS)
        assertEquals(g.actions, p.actions)
        assertTrue(p.finished)
        assertEquals(setOf(0), p.winners)
        val back = p.withScoring(Scoring.WINS)
        assertEquals(g.wins, back.wins)
        assertEquals(setOf(1), back.winners)
        // ohne Runden gelten wieder nur Punkte
        assertEquals(g.scores, g.withRounds(false).standings)
    }

    @Test fun undoOverRoundEnd() {
        val g = three().withTarget(2)
            .play(0 to 5).endRound()
            .play(0 to 3, 1 to 1).endRound()
        assertTrue(g.finished)
        val u = g.undo()
        assertFalse(u.finished)
        assertEquals(listOf(3, 1, 0), u.roundPoints)
        assertEquals(listOf(1, 0, 0), u.wins)
        assertEquals(2, u.round)
        val u2 = u.undo().undo().undo()
        assertEquals(listOf(5, 0, 0), u2.roundPoints)
        assertEquals(listOf(0, 0, 0), u2.wins)
        assertEquals(1, u2.round)
    }

    @Test fun removingPlayerRecomputesWins() {
        val g = three().play(0 to 5, 1 to 3).endRound()
        val r = g.removePlayer(0)
        assertEquals(listOf(1, 0), r.wins)
    }
}
