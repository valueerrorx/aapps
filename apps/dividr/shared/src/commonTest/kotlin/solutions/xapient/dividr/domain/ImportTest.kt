package solutions.xapient.dividr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class ImportTest {
    private val a = Dev(1)
    private val b = Dev(2)

    private fun shared(): Pair<Group, List<Long>> {
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        var g = g0
        repeat(12) { g = g.putEntry(Fx.expense(a, ids[0], 1000L + it, title = "E$it"), a.device) }
        return g to ids
    }

    @Test fun nothingNew() {
        val (g, _) = shared()
        assertTrue(ImportSummary.of(g, g).upToDate)
        // aelterer Stand des anderen bringt nichts
        val older = g.copy(entries = g.entries.entries.take(3).associate { it.toPair() })
        assertTrue(ImportSummary.of(g, older).upToDate)
    }

    @Test fun newPersonWithForAllCount() {
        val (g, _) = shared()
        val clara = b.id()
        val theirs = g.addMember(clara, "Clara", b.device).putEntry(Fx.expense(b, clara, 500, title = "Eis"), b.device)
        val s = ImportSummary.of(g, theirs)
        val p = s.newPeople.single()
        assertEquals("Clara", p.member.name)
        // 12 alte „alle"-Ausgaben + ihre eigene
        assertEquals(13, p.forAllFromStart)
        assertEquals(1, p.forAllFromNow)
        assertNull(p.sameAs)
        assertEquals(listOf("Eis"), s.added.map { it.title })
        assertFalse(s.upToDate)
    }

    @Test fun applyFromNowAndSameName() {
        val (g, ids) = shared()
        val anna2 = b.id()
        val theirs = g.addMember(anna2, "anna", b.device)
        val s = ImportSummary.of(g, theirs)
        assertEquals(ids[0], s.newPeople.single().sameAs?.id)
        // „dieselbe Anna": zusammenlegen, keine neue Person
        val same = s.apply(emptyMap(), setOf(anna2), a.device)
        assertEquals(ids[0], same.resolve(anna2))
        assertEquals(2, same.activeMembers().size)
        // getrennt lassen, aber „ab jetzt"
        val apart = s.apply(mapOf(anna2 to false), emptySet(), a.device)
        assertEquals(3, apart.activeMembers().size)
        assertEquals(0L, apart.balances()[anna2])
    }

    @Test fun foreignChangesAndDeletionsAreListed() {
        val (g, ids) = shared()
        val e = g.entries.values.first { it.title == "E3" }
        val d = g.entries.values.first { it.title == "E5" }
        val theirs = g.putEntry(e.copy(amount = 9999), b.device).deleteEntry(d.id, b.device)
            .putEntry(Fx.expense(b, ids[1], 700, title = "Diesel"), b.device)
        val s = ImportSummary.of(g, theirs)
        assertEquals(listOf("E3"), s.changed.map { it.before.title })
        assertEquals(9999L, s.changed.single().after.amount)
        assertEquals(listOf("E5"), s.deleted.map { it.title })
        assertEquals(listOf("Diesel"), s.added.map { it.title })
    }

    @Test fun ownNewerEditIsKept() {
        val (g, _) = shared()
        val e = g.entries.values.first()
        val mine = g.putEntry(e.copy(amount = 1), a.device).putEntry(e.copy(amount = 2), a.device)
        val theirs = g.putEntry(e.copy(amount = 3), b.device)
        val s = ImportSummary.of(mine, theirs)
        assertTrue(s.upToDate)
        assertEquals(2L, s.merged.entries.getValue(e.id).amount)
    }

    @Test fun joinAsNewPersonRoundTrip() {
        val (g, _) = shared()
        // Clara scannt, tritt als neue Person bei; Anna scannt danach Clara
        val clara = b.id()
        val onClara = g.addMember(clara, "Clara", b.device)
        val back = ImportSummary.of(g, onClara)
        assertEquals(12, back.newPeople.single().forAllFromStart)
        assertEquals(0, back.newPeople.single().forAllFromNow)
        val anna = back.apply(emptyMap(), emptySet(), a.device)
        assertEquals(onClara, anna)
        assertTrue(ImportSummary.of(onClara, anna).upToDate)
    }
}
