package solutions.xapient.nordr.ui

import solutions.xapient.nordr.domain.Angles
import kotlin.test.assertEquals
import kotlin.test.Test

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

    @Test
    fun declinationOneDecimalWithSeparator() {
        assertEquals("5,4°", formatDeclination(5.38f, ','))
        assertEquals("12.0°", formatDeclination(-11.96f, '.'))
        assertEquals("0.0°", formatDeclination(0.02f, '.'))
    }
}
