package solutions.xapient.levlr.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    @Test fun oneDecimalWithSeparator() {
        assertEquals("3.4", formatValue(3.4f, '.'))
        assertEquals("3,4", formatValue(3.4f, ','))
        assertEquals("12.5", formatValue(12.46f, '.'))
        assertEquals("0.0", formatValue(0f, '.'))
    }

    @Test fun realMinusSign() {
        assertEquals("−1.3", formatValue(-1.3f, '.'))
        // gerundet null: ohne Minus
        assertEquals("0.0", formatValue(-0.04f, '.'))
    }
}
