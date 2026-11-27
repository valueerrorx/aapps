package solutions.xapient.dividr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class BalanceTest {
    private val a = Dev(1)
    private val b = Dev(2)

    @Test fun restCentsGoToLowestIds() {
        assertEquals(mapOf(1L to 34L, 2L to 33L, 3L to 33L), shares(100, listOf(3, 1, 2)))
        assertEquals(mapOf(1L to 1L, 2L to 1L, 3L to 0L), shares(2, listOf(1, 2, 3)))
        assertEquals(emptyMap<Long, Long>(), shares(5, emptyList()))
    }

    @Test fun balancesSumToZeroWithRounding() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben", "Clara")
        val (anna, ben, clara) = ids
        val g = g0.putEntry(Fx.expense(a, anna, 1000), a.device)
            .putEntry(Fx.expense(a, ben, 1), a.device)
            .putEntry(Fx.expense(a, clara, 999, setOf(anna, ben)), a.device)
        val bal = g.balances()
        assertEquals(0L, bal.values.sum())
        // 1000/3 = 334 (Anna, kleinste ID) + 333 + 333; 1/3 -> Anna 1; 999/2 -> Anna 500, Ben 499
        assertEquals(1000L - 334 - 1 - 500, bal[anna])
        assertEquals(1L - 333 - 0 - 499, bal[ben])
        assertEquals(999L - 333 - 0, bal[clara])
    }

    @Test fun forAllIsDynamicAfterLateJoin() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        val g1 = g0.putEntry(Fx.expense(a, ids[0], 900), a.device)
        // Clara kommt spaeter, Standard: von Anfang an
        val clara = b.id()
        val g2 = g1.addMember(clara, "Clara", b.device)
        assertEquals(-300L, g2.balances()[clara])
        // „ab jetzt": zaehlt nur bei danach erstellten „alle"-Ausgaben
        val g3 = g2.setFromStart(clara, false, b.device)
        assertEquals(0L, g3.balances()[clara])
        val g4 = g3.putEntry(Fx.expense(a, ids[0], 300), a.device)
        assertEquals(-100L, g4.balances()[clara])
        // feste Auswahl gilt immer
        val g5 = g3.putEntry(Fx.expense(a, ids[0], 200, setOf(clara)), a.device)
        assertEquals(-200L, g5.balances()[clara])
    }

    @Test fun fromNowIgnoresEntriesTheJoinerAlreadyKnew() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        val old = Fx.expense(b, ids[1], 600)
        val g1 = g0.putEntry(old, b.device)
        // Dana tritt „ab jetzt" bei und kennt die Ausgabe von Ben schon
        val dana = Dev(3).id()
        val g2 = g1.addMember(dana, "Dana", 3, fromStart = false)
        assertTrue(covers(g2.members.getValue(dana).known, old.id))
        assertEquals(0L, g2.balances()[dana])
        // neue Ausgabe von Ben: hoehere Nummer = danach erstellt
        val later = g2.putEntry(Fx.expense(b, ids[1], 300), b.device)
        assertEquals(-100L, later.balances()[dana])
    }

    @Test fun emptyFixedSelectionFallsBackToAll() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        val e = Fx.expense(a, ids[0], 1000, setOf(999L))
        val g = g0.putEntry(e, a.device)
        assertTrue(g.isFallback(g.entries.getValue(e.id)))
        assertEquals(mapOf(ids[0] to 500L, ids[1] to -500L), g.balances())
    }

    @Test fun aliasCountsOnceInFixedSelection() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        val anna2 = a.id()
        val g1 = g0.addMember(anna2, "Anna", a.device)
            .putEntry(Fx.expense(a, ids[1], 1000, setOf(ids[0], anna2)), a.device)
            .mergeMembers(anna2, ids[0], a.device)
        assertEquals(mapOf(ids[0] to -1000L, ids[1] to 1000L), g1.balances())
    }

    @Test fun paymentsMoveBalance() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        val g = g0.putEntry(Fx.expense(a, ids[0], 1000), a.device)
            .putEntry(Fx.payment(a, ids[1], ids[0], 500), a.device)
        assertEquals(mapOf(ids[0] to 0L, ids[1] to 0L), g.balances())
        assertEquals(1000L, g.totalSpent())
    }

    @Test fun foreignCurrencyZeroAndTwoDigits() {
        // 10 000 JPY zu 0,0061 EUR = 61,00 EUR
        assertEquals(6100L, Money.convert(10_000, "JPY", 6_100, "EUR"))
        // 12,34 EUR zu 162,45 JPY = 2 004,633 -> 2 005 JPY
        assertEquals(2005L, Money.convert(1234, "EUR", 162_450_000, "JPY"))
        // 4 000,00 HUF zu 0,00253 EUR = 10,12 EUR
        assertEquals(1012L, Money.convert(400_000, "HUF", 2_530, "EUR"))
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        val g = g0.putEntry(Fx.expense(a, ids[0], 10_001, currency = "JPY", rate = 6_100), a.device)
        // 10 001 * 0,0061 = 61,0061 -> 6101 Cent; zuerst umrechnen, dann teilen (Rest-Cent an Anna)
        assertEquals(6101L, g.totalSpent())
        assertEquals(mapOf(ids[0] to 3050L, ids[1] to -3050L), g.balances())
    }

    @Test fun removedPersonIsNotInAll() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben", "Clara")
        val g = g0.removeMember(ids[2], a.device).putEntry(Fx.expense(a, ids[0], 1000), a.device)
        assertEquals(setOf(ids[0], ids[1]), g.balances().keys)
    }

    @Test fun visibleEntriesNewestDayFirst() {
        val (g0, ids) = Fx.group(a, "EUR", "Anna")
        val e1 = Fx.expense(a, ids[0], 1, day = 10)
        val e2 = Fx.expense(a, ids[0], 2, day = 12)
        val e3 = Fx.expense(a, ids[0], 3, day = 11)
        val g = g0.putEntry(e1, 1).putEntry(e2, 1).putEntry(e3, 1).deleteEntry(e3.id, 1)
        assertEquals(listOf(e2.id, e1.id), g.visibleEntries().map { it.id })
    }
}
