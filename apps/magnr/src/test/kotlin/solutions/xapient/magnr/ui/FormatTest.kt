package solutions.xapient.magnr.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun oneDecimal() {
        assertEquals("48.2", formatUt(48.24f))
        assertEquals("48,3", formatUt(48.25f, ','))
        assertEquals("0.6", formatUt(0.6f))
    }

    @Test fun signs() {
        assertEquals("+21.3", formatUt(21.3f, signed = true))
        assertEquals("−4.4", formatUt(-4.4f, signed = true))
        assertEquals("−4.4", formatUt(-4.4f))
        assertEquals("±0.0", formatUt(0.02f, signed = true))
        assertEquals("±0.0", formatUt(-0.04f, signed = true))
        assertEquals("0.0", formatUt(-0.01f))
    }

    @Test fun bigValuesWithoutDecimals() {
        assertEquals("4912", formatUt(4912.3f))
        assertEquals("+1000", formatUt(999.97f, signed = true))
        assertEquals("999.9", formatUt(999.94f))
    }

    @Test fun missingValue() {
        assertEquals("–", formatUt(Float.NaN))
    }

    @Test fun scaleLabels() {
        assertEquals("10", formatScale(10f))
        assertEquals("25", formatScale(25f))
        assertEquals("3", formatScale(3f))
        assertEquals("2,5", formatScale(2.5f, ','))
    }
}
