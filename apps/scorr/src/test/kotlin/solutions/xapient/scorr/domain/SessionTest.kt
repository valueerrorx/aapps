package solutions.xapient.scorr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    private fun played() = Session(Game.initial().score(0, 3, 0).endRound().score(1, 2, 5000))

    @Test fun undoAfterNewGameBringsMovesBack() {
        val s = played().newGame()
        assertEquals(listOf(0, 0), s.game.scores)
        assertTrue(s.canUndo)
        val u = s.undo()
        assertEquals(listOf(3, 2), u.game.scores)
        assertNull(u.before)
        assertEquals(listOf(3, 0), u.undo().game.scores)
    }

    @Test fun restoreKeepsCurrentNamesAndTarget() {
        val s = played().newGame()
        val edited = s.edit(s.game.rename(0, "Eva").withTarget(50))
        val u = edited.undo()
        assertEquals("Eva", u.game.players[0].name)
        assertEquals(50, u.game.target)
        assertEquals(listOf(3, 2), u.game.scores)
    }

    @Test fun newGameOnEmptyGameKeepsEarlierState() {
        val s = played().newGame()
        assertSame(s, s.newGame())
        assertSame(s.before, s.newGame().before)
        val empty = Session(Game.initial())
        assertNull(empty.newGame().before)
        assertFalse(empty.newGame().canUndo)
    }

    @Test fun playingAfterNewGameDropsEarlierState() {
        val s = played().newGame()
        val p = s.play(s.game.score(0, 1, 0))
        assertNull(p.before)
        assertEquals(listOf(0, 0), p.undo().game.scores)
        assertFalse(p.undo().canUndo)
    }

    @Test fun undoWithoutHistoryDoesNothing() {
        val s = Session(Game.initial())
        assertSame(s, s.undo())
    }
}
