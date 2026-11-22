package com.valueerrorx.scorr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameCodecTest {
    @Test fun roundTrip() {
        val g = Game(listOf(Player("Anna | Ben", 3), Player("", 5)))
            .withTarget(66).withRule(WinRule.FEWEST)
            .score(0, 12, 0).endRound().score(1, -4, 5000)
        val back = GameCodec.decode(GameCodec.encode(g))
        assertEquals(g.players, back.players)
        assertEquals(g.actions, back.actions)
        assertEquals(66, back.target)
        assertEquals(WinRule.FEWEST, back.rule)
        assertEquals(g.scores, back.scores)
    }

    @Test fun noTargetRoundTrips() {
        assertNull(GameCodec.decode(GameCodec.encode(Game.initial())).target)
    }

    @Test fun emptyOrForeignGivesNewGame() {
        assertEquals(Game.initial(), GameCodec.decode(null))
        assertEquals(Game.initial(), GameCodec.decode(""))
        assertEquals(Game.initial(), GameCodec.decode("hello\nP|0|x"))
        assertEquals(Game.initial(), GameCodec.decode("scorr1\nT|5"))
    }

    @Test fun brokenLinesAreSkipped() {
        val text = "scorr1\nT|abc\nR|NOPE\nP|x|bad\nP|2|Eva\nP|11|Max\nS|0|5\nS|7|3\nS|1|zz\nS|1|0\nE\nQ|1\n"
        val g = GameCodec.decode(text)
        assertEquals(listOf(Player("Eva", 2), Player("Max", 3)), g.players)
        assertEquals(listOf(Action.Score(0, 5), Action.EndRound), g.actions)
        assertNull(g.target)
        assertEquals(WinRule.MOST, g.rule)
    }

    @Test fun limitsAreEnforced() {
        val players = (0 until 12).joinToString("\n") { "P|$it|p$it" }
        val g = GameCodec.decode("scorr1\nT|99999999\n$players\nS|0|2000000000\n")
        assertEquals(Game.MAX_PLAYERS, g.players.size)
        assertEquals(Game.MAX, g.target)
        assertEquals(Game.MAX, g.scores[0])
    }

    @Test fun controlCharsInNamesDoNotBreakFormat() {
        val g = Game.initial().rename(0, "a\nS|0|99")
        val back = GameCodec.decode(GameCodec.encode(g))
        assertEquals(listOf(0, 0), back.scores)
        assertEquals("a S|0|99", back.players[0].name)
    }
}
