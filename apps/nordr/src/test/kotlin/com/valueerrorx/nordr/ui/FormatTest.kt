package com.valueerrorx.nordr.ui

import com.valueerrorx.nordr.domain.Angles
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun deviationIsWholeMagnitude() {
        assertEquals("12°", formatDegrees(-12.4f))
        assertEquals("18°", formatDegrees(17.6f))
        assertEquals("180°", formatDegrees(179.7f))
    }

    @Test
    fun courseNeverShows360() {
        // Kurs laeuft ueber Angles.whole, nicht ueber formatDegrees
        assertEquals(0, Angles.whole(359.7f))
        assertEquals(359, Angles.whole(359.4f))
    }
}
