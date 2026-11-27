package solutions.xapient.scorr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.Test

class GameTest {
    private fun two() = Game(listOf(Player("A", 0), Player("B", 1)))
    private fun three() = Game(listOf(Player("A", 0), Player("B", 1), Player("C", 2)))

    @Test fun startsAtZeroInRoundOne() {
        val g = two()
        assertEquals(listOf(0, 0), g.scores)
        assertEquals(1, g.round)
        assertFalse(g.canUndo)
    }

    @Test fun separateMovesWhenSlow() {
        val g = two().score(0, 1, 0).score(0, 1, 5000)
        assertEquals(2, g.actions.size)
        assertEquals(listOf(2, 0), g.scores)
    }

    @Test fun quickTapsMergeIntoOneMove() {
        val g = two().score(0, 1, 0).score(0, 1, 500).score(0, 1, 1900)
        assertEquals(listOf(Action.Score(0, 3)), g.actions)
        assertEquals(3, g.burst(0, 2000))
    }

    @Test fun mergeWindowCountsFromLastTap() {
        val g = two().score(0, 1, 0).score(0, 1, 1500).score(0, 1, 3000)
        assertEquals(1, g.actions.size)
    }

    @Test fun otherPlayerBreaksTheMove() {
        val g = two().score(0, 1, 0).score(1, 1, 100).score(0, 1, 200)
        assertEquals(3, g.actions.size)
        assertNull(g.burst(1, 300))
        assertEquals(1, g.burst(0, 300))
    }

    @Test fun burstEndsAfterWindow() {
        val g = two().score(0, 4, 0)
        assertEquals(4, g.burst(0, 1999))
        assertNull(g.burst(0, 2000))
        assertNull(g.burst(1, 10))
    }

    @Test fun moveCancellingToZeroDisappears() {
        val g = two().score(1, 2, 0).score(0, 1, 10).score(0, -1, 20)
        assertEquals(listOf<Action>(Action.Score(1, 2)), g.actions)
        assertEquals(listOf(0, 2), g.scores)
    }

    @Test fun cancelledMoveDoesNotSwallowNextTap() {
        val g = two().score(0, 5, 0).score(0, 1, 5000).score(0, -1, 5100)
        assertEquals(listOf<Action>(Action.Score(0, 5)), g.actions)
        assertNull(g.burst(0, 5200))
        val next = g.score(0, 1, 5200)
        assertEquals(listOf(Action.Score(0, 5), Action.Score(0, 1)), next.actions)
        assertEquals(listOf(5, 0), next.undo().scores)
    }

    @Test fun undoWithoutRoundsTakesHiddenRoundEnds() {
        val g = two().score(0, 3, 0).endRound().score(1, 2, 5000).endRound().endRound()
        assertEquals(listOf(Action.Score(0, 3), Action.EndRound), g.undo().actions)
        assertEquals(listOf(3, 2), g.withRounds(true).undo().scores)
        assertEquals(emptyList<Action>(), two().endRound().undo().actions)
    }

    @Test fun targetZeroOrBelowMeansNone() {
        assertNull(two().withTarget(0).target)
        assertNull(two().withTarget(-5).target)
        assertEquals(Game.MAX, two().withTarget(Int.MAX_VALUE).target)
        assertFalse(two().withTarget(0).finished)
    }

    @Test fun enteredNumberIsItsOwnMove() {
        val g = two().score(0, 1, 0).enter(0, 37, 100)
        assertEquals(listOf(Action.Score(0, 1), Action.Score(0, 37)), g.actions)
        assertEquals(37, g.burst(0, 200))
        assertEquals(1, g.undo().scores[0])
    }

    @Test fun zeroAndUnknownPlayerAreIgnored() {
        val g = two()
        assertSame(g, g.score(0, 0, 0))
        assertSame(g, g.score(5, 1, 0))
        assertSame(g, g.score(-1, 1, 0))
    }

    @Test fun undoTakesBackWholeMove() {
        val g = two().score(0, 1, 0).score(0, 1, 100).score(1, 5, 5000)
        val u = g.undo()
        assertEquals(listOf(2, 0), u.scores)
        assertEquals(listOf(0, 0), u.undo().scores)
        assertFalse(u.undo().canUndo)
        assertEquals(listOf(0, 0), u.undo().undo().scores)
    }

    @Test fun undoBreaksMerging() {
        val g = two().score(0, 1, 0).score(1, 1, 10).undo().score(0, 1, 20)
        assertEquals(2, g.actions.size)
    }

    @Test fun undoReopensRound() {
        val g = two().withRounds(true).score(0, 3, 0).endRound()
        assertEquals(2, g.round)
        assertEquals(1, g.undo().round)
        assertEquals(listOf(3, 0), g.undo().scores)
    }

    @Test fun roundEndBreaksMerging() {
        val g = two().score(0, 1, 0).endRound().score(0, 1, 10)
        assertEquals(listOf(listOf(1, 0), listOf(1, 0)), g.roundTable())
    }

    @Test fun roundTableSumsPerRound() {
        val g = three()
            .score(0, 10, 0).score(1, 5, 5000).score(0, -2, 10000)
            .endRound()
            .score(2, 7, 20000)
            .endRound()
        assertEquals(
            listOf(listOf(8, 5, 0), listOf(0, 0, 7), listOf(0, 0, 0)),
            g.roundTable(),
        )
        assertEquals(3, g.round)
        assertEquals(listOf(8, 5, 7), g.scores)
    }

    @Test fun scoresAreClamped() {
        val g = two().score(0, Int.MAX_VALUE, 0).score(0, Int.MAX_VALUE, 5000)
        assertEquals(Game.MAX, g.scores[0])
        val n = two().score(1, Int.MIN_VALUE, 0)
        assertEquals(-Game.MAX, n.scores[1])
    }

    @Test fun leadersWithoutTarget() {
        assertTrue(two().leaders.isEmpty())
        val g = three().score(0, 3, 0).score(1, 3, 5000)
        assertEquals(setOf(0, 1), g.leaders)
        assertEquals(setOf(2), g.withRule(WinRule.FEWEST).leaders)
        assertTrue(g.winners.isEmpty())
        assertFalse(g.finished)
    }

    @Test fun singlePlayerHasNoLeader() {
        val g = Game(listOf(Player("", 0))).score(0, 5, 0)
        assertTrue(g.leaders.isEmpty())
    }

    @Test fun targetEndsGameMostWins() {
        val g = three().withTarget(10).score(0, 9, 0)
        assertFalse(g.finished)
        val f = g.score(1, 12, 5000)
        assertTrue(f.finished)
        assertEquals(setOf(1), f.winners)
    }

    @Test fun targetEndsGameFewestWins() {
        val g = three().withTarget(100).withRule(WinRule.FEWEST)
            .score(0, 100, 0).score(1, 20, 5000).score(2, 40, 10000)
        assertTrue(g.finished)
        assertEquals(setOf(1), g.winners)
    }

    @Test fun tieGivesSeveralWinners() {
        val g = two().withTarget(5).score(0, 5, 0).score(1, 5, 5000)
        assertEquals(setOf(0, 1), g.winners)
    }

    @Test fun undoReopensFinishedGame() {
        val g = two().withTarget(3).score(0, 3, 0)
        assertTrue(g.finished)
        assertFalse(g.undo().finished)
        assertFalse(g.withTarget(null).finished)
    }

    @Test fun newGameKeepsPlayersAndSettings() {
        val g = two().withTarget(50).withRule(WinRule.FEWEST).score(0, 9, 0).endRound().newGame()
        assertEquals(listOf(0, 0), g.scores)
        assertEquals(1, g.round)
        assertEquals(50, g.target)
        assertEquals(WinRule.FEWEST, g.rule)
        assertEquals(2, g.players.size)
    }

    @Test fun addPlayerTakesFreeColorUpToEight() {
        var g = Game(listOf(Player("", 0), Player("", 2)))
        g = g.addPlayer()
        assertEquals(1, g.players[2].color)
        repeat(10) { g = g.addPlayer() }
        assertEquals(Game.MAX_PLAYERS, g.players.size)
        assertEquals((0 until Game.COLORS).toSet(), g.players.map { it.color }.toSet())
        assertEquals(0, g.scores.last())
    }

    @Test fun removePlayerDropsHisMovesAndShifts() {
        val g = three().score(0, 1, 0).score(1, 2, 5000).score(2, 3, 10000).endRound()
        val r = g.removePlayer(1)
        assertEquals(listOf("A", "C"), r.players.map { it.name })
        assertEquals(listOf(1, 3), r.scores)
        assertEquals(2, r.round)
        assertTrue(g.hasActions(1))
        assertFalse(three().hasActions(1))
    }

    @Test fun lastPlayerStays() {
        val g = Game(listOf(Player("", 0)))
        assertSame(g, g.removePlayer(0))
        val t = two()
        assertSame(t, t.removePlayer(7))
    }

    @Test fun renameCleansAndLimits() {
        val g = two().rename(0, "Te\nam\u0007 " + "x".repeat(40))
        assertEquals("Te am " + "x".repeat(18), g.players[0].name)
        assertEquals(Game.MAX_NAME, g.players[0].name.length)
    }

    @Test fun cleanDoesNotCutEmojiInHalf() {
        val name = "a".repeat(Game.MAX_NAME - 1) + "😀"
        assertEquals("a".repeat(Game.MAX_NAME - 1), Game.clean(name))
    }

    @Test fun recolorWraps() {
        assertEquals(1, two().recolor(0, Game.COLORS + 1).players[0].color)
    }
}
