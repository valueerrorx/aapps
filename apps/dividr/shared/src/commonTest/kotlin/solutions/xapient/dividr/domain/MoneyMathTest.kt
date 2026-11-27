package solutions.xapient.dividr.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Rechnen ohne BigDecimal: laeuft auf JVM und iOS gleich
class MoneyMathTest {
    @Test fun parse() {
        assertEquals(1250L, Money.parse("12,5", "EUR"))
        assertEquals(1250L, Money.parse("1 2.50", "EUR"))
        assertEquals(50L, Money.parse(",5", "EUR"))
        assertEquals(4000L, Money.parse("4000", "JPY"))
        assertEquals(Group.MAX_AMOUNT, Money.parse("100000000,00", "EUR"))
        assertNull(Money.parse("100000000,01", "EUR"))
        assertNull(Money.parse("99999999999999999999999", "EUR"))
    }

    @Test fun parseRate() {
        assertEquals(2_530L, Money.parseRate("0,00253"))
        assertEquals(1L, Money.parseRate("0.0000005"))
        assertNull(Money.parseRate("0.0000004"))
        assertEquals(1_500_000L, Money.parseRate("1.5"))
        assertEquals(1_000_000_000L, Money.parseRate("1e3"))
        assertEquals(2_000_000L, Money.parseRate("+2."))
        assertNull(Money.parseRate("-1"))
        assertNull(Money.parseRate("1000001"))
        assertNull(Money.parseRate(""))
        assertNull(Money.parseRate("."))
    }

    @Test fun convert() {
        // 12.000 HUF zu 0,00253 -> 30,36 EUR
        assertEquals(3_036L, Money.convert(1_200_000, "HUF", 2_530, "EUR"))
        // 4.000 JPY zu 0,0061 EUR -> 24,40 EUR
        assertEquals(2_440L, Money.convert(4_000, "JPY", 6_100, "EUR"))
        // kaufmaennisch: 0,5 Cent rundet auf
        assertEquals(1L, Money.convert(1, "EUR", 500_000, "EUR"))
        assertEquals(0L, Money.convert(1, "EUR", 499_999, "EUR"))
        // grosse Werte ohne Ueberlauf
        assertEquals(10_000_000_000_000_000L, Money.convert(Group.MAX_AMOUNT, "EUR", MAX_RATE, "EUR"))
    }

    @Test fun mulDiv() {
        assertEquals(3L, Money.mulDivHalfUp(5, 1, 2))
        assertEquals(2L, Money.mulDivHalfUp(7, 1, 4))
        assertEquals(999_999_999_999_999_999L / 1_000, Money.mulDivHalfUp(999_999_999_999_999_999L, 1, 1_000) - 1)
    }
}
