package com.valueerrorx.scorr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardTest {
    @Test fun portraitGrid() {
        assertEquals(Grid(1, 1), Board.grid(1, false))
        assertEquals(Grid(1, 2), Board.grid(2, false))
        assertEquals(Grid(1, 3), Board.grid(3, false))
        assertEquals(Grid(2, 2), Board.grid(4, false))
        assertEquals(Grid(2, 3), Board.grid(5, false))
        assertEquals(Grid(2, 3), Board.grid(6, false))
        assertEquals(Grid(2, 4), Board.grid(8, false))
    }

    @Test fun landscapeGrid() {
        assertEquals(Grid(2, 1), Board.grid(2, true))
        assertEquals(Grid(4, 1), Board.grid(4, true))
        assertEquals(Grid(3, 2), Board.grid(5, true))
        assertEquals(Grid(4, 2), Board.grid(7, true))
        assertEquals(Grid(4, 2), Board.grid(8, true))
    }

    @Test fun gridAlwaysFitsPlayers() {
        for (n in 1..8) for (l in listOf(true, false)) {
            val g = Board.grid(n, l)
            assert(g.cols * g.rows >= n)
            assert(g.cols * g.rows - n < g.cols)
        }
        assertEquals(Grid(1, 1), Board.grid(0, false))
    }

    @Test fun stepsFromDrag() {
        assertEquals(0, Board.steps(-23f, 24f))
        assertEquals(1, Board.steps(-24f, 24f))
        assertEquals(7, Board.steps(-170f, 24f))
        assertEquals(-2, Board.steps(50f, 24f))
        assertEquals(0, Board.steps(50f, 0f))
    }

    @Test fun formatUsesRealMinus() {
        assertEquals("0", Board.format(0))
        assertEquals("42", Board.format(42))
        assertEquals("−42", Board.format(-42))
        assertEquals("−999999", Board.format(-Game.MAX))
    }

    @Test fun formatDeltaAlwaysSigned() {
        assertEquals("+3", Board.formatDelta(3))
        assertEquals("−7", Board.formatDelta(-7))
        assertEquals("0", Board.formatDelta(0))
    }

    @Test fun parseAmount() {
        assertNull(Board.parseAmount(""))
        assertNull(Board.parseAmount("-"))
        assertEquals(0, Board.parseAmount("000"))
        assertEquals(37, Board.parseAmount("037"))
        assertEquals(12, Board.parseAmount("1.2"))
        assertEquals(Game.MAX, Board.parseAmount("12345678"))
    }
}
