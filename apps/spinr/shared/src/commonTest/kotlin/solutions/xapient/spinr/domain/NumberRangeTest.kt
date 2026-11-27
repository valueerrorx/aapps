package solutions.xapient.spinr.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class NumberRangeTest {
    @Test
    fun ascending() {
        assertEquals((1..10).map { "$it" }, NumberRange.entries(1, 10))
        assertEquals(listOf("-1", "0", "1"), NumberRange.entries(-1, 1))
    }

    @Test
    fun limitsTwoToFifty() {
        assertEquals(listOf("7", "8"), NumberRange.entries(7, 8))
        assertEquals(50, NumberRange.entries(1, 50)!!.size)
        assertEquals(50, NumberRange.entries(0, 49)!!.size)
        assertNull(NumberRange.entries(1, 51))
        assertNull(NumberRange.entries(0, 50))
        assertNull(NumberRange.entries(1, 1000))
        // kein Ueberlauf an den Grenzen von Int
        assertNull(NumberRange.entries(Int.MIN_VALUE, Int.MAX_VALUE))
        assertEquals(2, NumberRange.entries(Int.MAX_VALUE - 1, Int.MAX_VALUE)!!.size)
    }

    @Test
    fun sameNumberIsTooFew() {
        assertNull(NumberRange.entries(5, 5))
        assertNull(NumberRange.parse("3", "3"))
    }

    @Test
    fun reversedCountsDown() {
        assertEquals(listOf("10", "9", "8", "7"), NumberRange.entries(10, 7))
        assertEquals(50, NumberRange.entries(50, 1)!!.size)
        assertNull(NumberRange.entries(51, 1))
    }

    @Test
    fun parseValidatesInput() {
        assertEquals(listOf("2", "3", "4"), NumberRange.parse(" 2", "4 "))
        assertNull(NumberRange.parse("", "4"))
        assertNull(NumberRange.parse("1", ""))
        assertNull(NumberRange.parse("a", "4"))
        assertNull(NumberRange.parse("-", "4"))
        assertEquals(listOf("-2", "-1"), NumberRange.parse("-2", "-1"))
    }

    @Test
    fun cleanInputKeepsDigitsAndLeadingMinus() {
        assertEquals("12", NumberRange.cleanInput("1a2"))
        assertEquals("-5", NumberRange.cleanInput("-5"))
        assertEquals("5", NumberRange.cleanInput("5-"))
        assertEquals("123456", NumberRange.cleanInput("12345678"))
        assertEquals("", NumberRange.cleanInput("x"))
    }
}
