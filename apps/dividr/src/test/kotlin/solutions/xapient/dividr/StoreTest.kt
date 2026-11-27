package solutions.xapient.dividr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import solutions.xapient.dividr.domain.Entry
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Stamp
import solutions.xapient.dividr.domain.addMember
import solutions.xapient.dividr.domain.newGroup
import solutions.xapient.dividr.domain.putEntry
import solutions.xapient.dividr.domain.Ids
import java.io.File

@RunWith(RobolectricTestRunner::class)
class StoreTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    // kleine Gruppe mit n Ausgaben (Fixtures liegen im shared-Modul)
    private fun trip(n: Int): Group {
        val dev = 0x7A3F_1C02
        var seq = 0
        var g = newGroup(0x1234_5678_9ABCL, "Kornaten", "EUR", dev)
        val ids = listOf("Anna", "Ben", "Clara").map { name -> Ids.of(dev, ++seq).also { g = g.addMember(it, name, dev) } }
        repeat(n) { i ->
            g = g.putEntry(Entry(Ids.of(dev, ++seq), Stamp(1, dev), title = "Eis $i", amount = 100L + i, payer = ids[i % 3], forAll = true, forIds = emptySet(), day = 20_400), dev)
        }
        return g
    }

    @Test fun saveLoadAndBackup() {
        val store = Store(ctx)
        val g1 = trip(5)
        val g2 = trip(8)
        val key = store.keyFor(g1)
        store.save(key, g1)
        assertEquals(g1, store.load(key))
        store.save(key, g2)
        assertEquals(mapOf(key to g2), store.loadAll())
        // kaputte Hauptdatei: letzte gueltige Sicherung
        File(ctx.filesDir, "groups/$key.dvr").writeBytes(byteArrayOf(1, 2, 3))
        assertEquals(g1, store.load(key))
        store.delete(key)
        assertNull(store.load(key))
    }

    @Test fun deviceIsStableAndIdsGrow() {
        val a = Store(ctx)
        val b = Store(ctx)
        assertEquals(a.device, b.device)
        assertNotEquals(0, a.device)
        val i1 = a.newId()
        val i2 = b.newId()
        assertEquals(Ids.seq(i1) + 1, Ids.seq(i2))
        assertEquals(a.device, Ids.device(i1))
    }

    @Test fun localSettings() {
        val s = Store(ctx)
        s.setMe("k", 42L)
        assertEquals(42L, s.me("k"))
        s.setLastRate("HUF", 2530)
        assertEquals(2530L, s.lastRate("HUF"))
        assertNull(s.lastRate("JPY"))
    }
}
