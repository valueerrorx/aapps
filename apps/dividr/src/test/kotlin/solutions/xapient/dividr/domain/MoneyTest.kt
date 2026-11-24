package solutions.xapient.dividr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyTest {
    private fun norm(s: String) = s.replace(' ', ' ').replace(' ', ' ')

    @Test fun parseBothSeparators() {
        assertEquals(1250L, Money.parse("12,5", "EUR"))
        assertEquals(1250L, Money.parse("12.50", "EUR"))
        assertEquals(1200L, Money.parse("12", "EUR"))
        assertEquals(1200L, Money.parse("12.", "EUR"))
        assertEquals(50L, Money.parse(",5", "EUR"))
        assertEquals(4000L, Money.parse("4000", "JPY"))
        assertNull(Money.parse("12,345", "EUR"))
        assertNull(Money.parse("1,5", "JPY"))
        assertNull(Money.parse("1.2.3", "EUR"))
        assertNull(Money.parse("abc", "EUR"))
        assertNull(Money.parse("", "EUR"))
        assertNull(Money.parse("99999999999", "EUR"))
    }

    @Test fun rates() {
        assertEquals(2_530L, Money.parseRate("0,00253"))
        assertEquals(395_120_000L, Money.parseRate("395.12"))
        assertNull(Money.parseRate("0"))
        assertNull(Money.parseRate("x"))
        assertEquals("0,00253", Money.formatRate(2_530, Locale.GERMANY))
        assertEquals("395.12", Money.formatRate(395_120_000, Locale.US))
    }

    @Test fun formatting() {
        assertEquals("1.834,20 €", norm(Money.format(183_420, "EUR", Locale.GERMANY)))
        assertEquals("+42,10 €", norm(Money.format(4_210, "EUR", Locale.GERMANY, signed = true)))
        assertEquals("−18,00 €", norm(Money.format(-1_800, "EUR", Locale.GERMANY, signed = true)))
        assertEquals("€1,834.20", Money.format(183_420, "EUR", Locale.UK))
        assertTrue(norm(Money.format(4_000, "JPY", Locale.GERMANY)).startsWith("4.000"))
        assertEquals("1.834,20", Money.plain(183_420, "EUR", Locale.GERMANY))
    }

    @Test fun currencies() {
        assertTrue(Money.valid("EUR"))
        assertFalse(Money.valid("eur"))
        assertFalse(Money.valid("ABC"))
        assertEquals(0, Money.digits("JPY"))
        assertEquals(2, Money.digits("EUR"))
        assertEquals("CHF", Money.defaultCurrency(Locale("de", "CH")))
        assertEquals("EUR", Money.defaultCurrency(Locale("de")))
    }
}
