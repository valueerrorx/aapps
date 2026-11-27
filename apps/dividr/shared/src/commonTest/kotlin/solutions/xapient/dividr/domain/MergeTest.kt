package solutions.xapient.dividr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.random.Random

class MergeTest {
    private val a = Dev(1)
    private val b = Dev(2)

    private fun base(): Triple<Group, Long, Long> {
        val (g, ids) = Fx.group(a, "EUR", "Anna", "Ben")
        return Triple(g, ids[0], ids[1])
    }

    @Test fun mergeWithItselfIsIdentity() {
        val (g, anna, _) = base()
        val g2 = g.putEntry(Fx.expense(a, anna, 1000), a.device)
        assertEquals(g2, Merge.merge(g2, g2))
    }

    @Test fun higherVersionWins() {
        val (g, anna, _) = base()
        val e = Fx.expense(a, anna, 1000)
        val g0 = g.putEntry(e, a.device)
        val onA = g0.putEntry(g0.entries.getValue(e.id).copy(amount = 1200), a.device)
        val onB = g0
        assertEquals(1200, Merge.merge(onB, onA).entries.getValue(e.id).amount)
        assertEquals(1200, Merge.merge(onA, onB).entries.getValue(e.id).amount)
    }

    @Test fun sameVersionLargerDeviceWins() {
        val (g, anna, _) = base()
        val e = Fx.expense(a, anna, 1000)
        val g0 = g.putEntry(e, a.device)
        val onA = g0.putEntry(g0.entries.getValue(e.id).copy(amount = 1100), a.device)
        val onB = g0.putEntry(g0.entries.getValue(e.id).copy(amount = 2200), b.device)
        assertEquals(2200, Merge.merge(onA, onB).entries.getValue(e.id).amount)
        assertEquals(Merge.merge(onA, onB), Merge.merge(onB, onA))
    }

    @Test fun editBeatsConcurrentDelete() {
        val (g, anna, _) = base()
        val e = Fx.expense(a, anna, 1000)
        val g0 = g.putEntry(e, a.device)
        val deletedOnA = g0.deleteEntry(e.id, a.device)
        val editedOnB = g0.putEntry(g0.entries.getValue(e.id).copy(amount = 900), b.device)
        val m = Merge.merge(deletedOnA, editedOnB)
        assertFalse(m.entries.getValue(e.id).deleted)
        assertEquals(900, m.entries.getValue(e.id).amount)
        // wer die Aenderung gesehen hat und dann loescht, gewinnt
        val again = m.deleteEntry(e.id, a.device)
        assertTrue(Merge.merge(again, editedOnB).entries.getValue(e.id).deleted)
    }

    @Test fun tombstoneDoesNotComeBackFromOldPhone() {
        val (g, anna, _) = base()
        val e = Fx.expense(a, anna, 1000)
        val old = g.putEntry(e, a.device)
        val deleted = old.deleteEntry(e.id, a.device)
        val m = Merge.merge(deleted, old)
        assertTrue(m.entries.getValue(e.id).deleted)
        assertEquals(m, Merge.merge(old, deleted))
        assertEquals("", m.entries.getValue(e.id).title)
    }

    @Test fun renameIsLww() {
        val (g, anna, _) = base()
        val r1 = g.rename(anna, "Anne", a.device)
        val r2 = r1.rename(anna, "Annika", b.device)
        assertEquals("Annika", Merge.merge(r1, r2).members.getValue(anna).name)
        assertEquals("Annika", Merge.merge(g, r2).members.getValue(anna).name)
    }

    @Test fun groupMetaIsLww() {
        val (g, _, _) = base()
        val onA = g.setMeta("Segeln", "EUR", a.device)
        val onB = g.setMeta("Kroatien", "EUR", b.device)
        assertEquals("Kroatien", Merge.merge(onA, onB).name)
        assertEquals("Kroatien", Merge.merge(onB, onA).name)
    }

    @Test fun aliasResolvesAndCycleTakesSmallest() {
        val (g, anna, ben) = base()
        val c = a.id()
        val g1 = g.addMember(c, "Anna", a.device).mergeMembers(c, anna, a.device)
        assertEquals(anna, g1.resolve(c))
        assertEquals(listOf(anna, ben), g1.activeMembers().map { it.id })
        // Zyklus von Hand (z. B. nach gleichzeitigem Ruecksetzen): kleinste ID gewinnt
        val cyc = g1.copy(members = g1.members + (anna to g1.members.getValue(anna).copy(alias = c)))
        assertEquals(minOf(anna, c), cyc.resolve(c))
        assertEquals(minOf(anna, c), cyc.resolve(anna))
        // zurueck: Alias entfernen
        assertEquals(c, g1.unmerge(c, a.device).resolve(c))
    }

    @Test fun mergeDirectionIsAlwaysLargerIntoSmaller() {
        val (g, anna, ben) = base()
        assertEquals(anna, g.mergeMembers(anna, ben, a.device).resolve(ben))
        assertEquals(anna, g.mergeMembers(ben, anna, a.device).resolve(ben))
    }

    @Test fun guestTakesOverPhone() {
        // Anna legt Gruppe mit Gast Ben an; Ben scannt und waehlt „Ich bin Ben": keine neue Person
        val (g, anna, ben) = base()
        val withEntry = g.putEntry(Fx.expense(a, anna, 3000), a.device)
        val onBen = withEntry // Ben uebernimmt die Gruppe 1:1
        val benAdds = onBen.putEntry(Fx.expense(b, ben, 600), b.device)
        val back = ImportSummary.of(withEntry, benAdds)
        assertTrue(back.newPeople.isEmpty())
        assertEquals(1, back.added.size)
        assertEquals(mapOf(anna to 1200L, ben to -1200L), back.merged.balances())
    }

    @Test fun duplicateNamesAreDistinguished() {
        val (g, anna, _) = base()
        val anna2 = b.id()
        val g1 = g.addMember(anna2, "anna ", b.device)
        assertEquals("Anna", g1.displayName(anna))
        assertEquals("anna · 2", g1.displayName(anna2))
    }

    @Test fun removeOnlyWithoutReferences() {
        val (g, anna, ben) = base()
        val g1 = g.putEntry(Fx.expense(a, anna, 1000, setOf(ben)), a.device)
        assertEquals(1, g1.references(ben))
        assertEquals(g1, g1.removeMember(ben, a.device))
        val c = a.id()
        val g2 = g1.addMember(c, "Clara", a.device).removeMember(c, a.device)
        assertTrue(g2.members.getValue(c).removed)
        assertEquals(listOf(anna, ben), g2.activeMembers().map { it.id })
    }

    @Test fun adoptUnitesGroupIds() {
        val (mine, _, _) = base()
        val other = newGroup(99L, "Kornaten", "EUR", b.device).addMember(b.id(), "Ben", b.device)
        val adopted = mine.adopt(other)
        assertTrue(adopted.sameGroup(mine) && adopted.sameGroup(other))
        val m = ImportSummary.of(mine, adopted)
        assertEquals(setOf(Fx.GID, 99L), m.merged.ids)
        assertNotNull(m.newPeople.single().sameAs)
    }

    // Eigenschaftstest: Geraete aendern zufaellig und tauschen per Gossip; am Ende alle gleich
    @Test fun gossipConverges() {
        repeat(20) { seed ->
            val rnd = Random(seed)
            val devs = List(3 + rnd.nextInt(3)) { Dev(100 + it) }
            val (start, _) = Fx.group(devs[0], "EUR", "A", "B")
            val states = MutableList(devs.size) { start }
            repeat(150) {
                val i = rnd.nextInt(devs.size)
                val d = devs[i]
                var g = states[i]
                val people = g.activeMembers().map { it.id }
                val live = g.entries.values.filter { !it.deleted }
                when (rnd.nextInt(8)) {
                    0, 1 -> g = g.putEntry(Fx.expense(d, people.random(rnd), 1L + rnd.nextInt(10_000)), d.device)
                    2 -> live.randomOrNull(rnd)?.let { g = g.putEntry(it.copy(amount = 1L + rnd.nextInt(10_000)), d.device) }
                    3 -> live.randomOrNull(rnd)?.let { g = g.deleteEntry(it.id, d.device) }
                    4 -> g = g.addMember(d.id(), "P${rnd.nextInt(9)}", d.device, rnd.nextBoolean())
                    5 -> g = g.rename(people.random(rnd), "N${rnd.nextInt(9)}", d.device)
                    else -> {
                        val j = rnd.nextInt(devs.size)
                        g = Merge.merge(g, states[j])
                        states[j] = Merge.merge(states[j], g)
                    }
                }
                states[i] = g
            }
            // Kette hin und zurueck genuegt
            for (i in 1 until states.size) states[i] = Merge.merge(states[i], states[i - 1])
            for (i in states.size - 2 downTo 0) states[i] = Merge.merge(states[i], states[i + 1])
            states.forEach { assertEquals(states[0], it, "seed $seed") }
            assertEquals(0L, states[0].balances().values.sum())
            // Gesetze auf Zwischenstaenden
            val (x, y, z) = Triple(states[0], start, states.last())
            assertEquals(Merge.merge(x, y), Merge.merge(y, x))
            assertEquals(Merge.merge(x, Merge.merge(y, z)), Merge.merge(Merge.merge(x, y), z))
        }
    }

    @Test fun lawsOnDivergentStates() {
        repeat(30) { seed ->
            val rnd = Random(seed)
            val devs = List(3) { Dev(10 + it) }
            val (start, ids) = Fx.group(devs[0], "EUR", "A", "B")
            val s = devs.map { d ->
                var g = start
                repeat(10) {
                    g = if (rnd.nextBoolean() || g.entries.isEmpty()) g.putEntry(Fx.expense(d, ids.random(rnd), 1L + rnd.nextInt(500)), d.device)
                    else g.deleteEntry(g.entries.keys.random(rnd), d.device)
                }
                g
            }
            val (x, y, z) = s
            assertEquals(Merge.merge(x, y), Merge.merge(y, x))
            assertEquals(Merge.merge(x, Merge.merge(y, z)), Merge.merge(Merge.merge(x, y), z))
            assertEquals(x, Merge.merge(x, x))
        }
    }
}
