package com.valueerrorx.spinr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelTest {
    @Test
    fun labelsSkipBlankEntries() {
        val w = Wheel("x", listOf(" Pizza ", "", "   ", "Sushi"))
        assertEquals(listOf("Pizza", "Sushi"), w.labels)
        assertTrue(w.canSpin)
        assertFalse(Wheel("x", listOf("a", " ")).canSpin)
    }

    @Test
    fun cleanRemovesControlCharsAndLimitsLength() {
        assertEquals("a b", Wheel.clean("a\nb"))
        assertEquals("ab", Wheel.clean("a\u001Eb\u001F"))
        assertEquals(Wheel.MAX_LENGTH, Wheel.clean("x".repeat(100)).length)
        assertEquals("Grüße 🎉", Wheel.clean("Grüße 🎉"))
        assertEquals("x".repeat(39), Wheel.clean("x".repeat(39) + "🎉"))
    }

    @Test
    fun codecRoundTrip() {
        val wheels = listOf(
            Wheel("Was essen wir?", listOf("Pizza", "Sushi", "")),
            Wheel("", listOf("Ja", "Nein")),
            Wheel("Zeichen ; , | \" '", listOf("a=b", "{x}", "ä ö ü ß")),
        )
        assertEquals(wheels, WheelCodec.decode(WheelCodec.encode(wheels)))
    }

    @Test
    fun codecStripsSeparatorsFromInput() {
        val evil = listOf(Wheel("a\u001Eb", listOf("c\u001Fd", "e")))
        val back = WheelCodec.decode(WheelCodec.encode(evil))
        assertEquals(listOf(Wheel("ab", listOf("cd", "e"))), back)
    }

    @Test
    fun codecToleratesGarbage() {
        assertEquals(emptyList<Wheel>(), WheelCodec.decode(null))
        assertEquals(emptyList<Wheel>(), WheelCodec.decode(""))
        assertEquals(emptyList<Wheel>(), WheelCodec.decode("nur ein name"))
        val many = Wheel("n", (1..80).map { "$it" })
        assertEquals(Wheel.MAX_ENTRIES, WheelCodec.decode(WheelCodec.encode(listOf(many))).single().entries.size)
    }

    @Test
    fun roundRemovesUntilTwoAreLeft() {
        var r = Round(4)
        assertEquals(listOf(0, 1, 2, 3), r.remaining)
        r = r.remove(2)
        assertEquals(listOf(0, 1, 3), r.remaining)
        r = r.remove(2) // schon draussen
        assertEquals(listOf(0, 1, 3), r.remaining)
        r = r.remove(0)
        assertEquals(listOf(1, 3), r.remaining)
        assertFalse(r.canRemove)
        assertEquals(r, r.remove(1))
        assertTrue(r.canSpin)
        assertEquals(Round(4), r.restore())
    }

    @Test
    fun neighbourColorsDiffer() {
        for (k in 3..8) {
            for (n in 2..Wheel.MAX_ENTRIES) {
                val colors = (0 until n).map { Palette.colorIndex(it, n, k) }
                for (i in 0 until n) {
                    assertTrue(colors[i] in 0 until k)
                    assertNotEquals("k=$k n=$n i=$i", colors[i], colors[(i + 1) % n])
                }
            }
        }
    }
}
