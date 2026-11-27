package solutions.xapient.dividr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.test.Test
import kotlin.random.Random

class CodecTest {
    private val a = Dev(0x7A3F_1C02)
    private val b = Dev(-0x1D2C_3B4A)

    // Toern: 6 Personen, n Eintraege mit typischen Titeln
    fun trip(n: Int, seed: Int = 1): Group {
        val rnd = Random(seed)
        val (g0, ids) = Fx.group(a, "EUR", "Anna", "Ben", "Clara", "David", "Eva", "Felix")
        var g = g0
        val titles = listOf("Proviant Konzum", "Marina Hvar", "Diesel", "Konoba Mate", "Eis", "Kaffee", "Taxi", "Bar", "Bäckerei", "Hafengebühr")
        repeat(n) { i ->
            val d = if (i % 3 == 0) b else a
            val forIds = if (rnd.nextInt(4) == 0) ids.shuffled(rnd).take(2 + rnd.nextInt(3)).toSet() else null
            val foreign = rnd.nextInt(10) == 0
            g = g.putEntry(
                Fx.expense(
                    d, ids.random(rnd), 100L + rnd.nextInt(30_000), forIds, titles.random(rnd), 20_400 + i / 8,
                    if (foreign) "HUF" else "", if (foreign) 2_530 else 0,
                ),
                d.device,
            )
        }
        return g
    }

    @Test fun roundTripAllFields() {
        var g = trip(30)
        val ids = g.activeMembers().map { it.id }
        val late = b.id()
        g = g.addMember(late, "Gina", b.device, fromStart = false)
            .rename(ids[1], "Benjamin", b.device)
            .mergeMembers(ids[5], ids[0], a.device)
            .putEntry(Fx.payment(b, ids[2], ids[3], 5000), b.device)
            .deleteEntry(g.entries.keys.first(), b.device)
            .setMeta("Segeln in den Kornaten ⛵", "EUR", b.device)
        g = g.copy(ids = g.ids + 42L)
        val back = WireCodec.decode(WireCodec.encode(g))
        assertEquals(g, back)
        assertEquals(WireCodec.encode(g).toList(), WireCodec.encode(back).toList())
    }

    @Test fun emptyGroupRoundTrip() {
        val g = newGroup(-5L, "", "JPY", a.device)
        assertEquals(g, WireCodec.decode(WireCodec.encode(g)))
    }

    @Test fun sizeBenchmark() {
        for (n in listOf(30, 60, 150, 300)) {
            val raw = WireCodec.encode(trip(n))
            val z = Frames.compress(raw)
            val parts = Frames.pack(raw).size
            println("dividr: $n Eintraege -> ${raw.size} B roh, ${z.size} B komprimiert, $parts Teil(e)")
            // Pflicht: realistischer Toern (6 Personen, 60 Ausgaben) = genau ein Code
            if (n <= 60) assertEquals(1, parts)
        }
    }

    @Test fun unknownFormatIsNewer() {
        val bytes = WireCodec.encode(trip(2))
        bytes[0] = 2
        try { WireCodec.decode(bytes); fail() } catch (_: NewerFormatException) { }
    }

    @Test fun limitsAreEnforced() {
        val g = trip(1)
        val many = (1..101).fold(g) { acc, i -> acc.addMember(Ids.of(9, i), "P$i", 9) }
        expectBroken(WireCodec.encode(many))
        // Betrag ueber 10^10 von Hand
        val e = g.entries.values.first()
        val huge = g.copy(entries = mapOf(e.id to e.copy(amount = Group.MAX_AMOUNT + 1)))
        expectBroken(WireCodec.encode(huge))
        val v = g.copy(entries = mapOf(e.id to e.copy(stamp = Stamp(WireCodec.MAX_VERSION + 1, a.device))))
        expectBroken(WireCodec.encode(v))
        val badCur = g.copy(meta = g.meta.copy(currency = "XX1"))
        expectBroken(WireCodec.encode(badCur))
    }

    @Test fun truncatedAndTrailingBytes() {
        val bytes = WireCodec.encode(trip(5))
        for (cut in 0 until bytes.size) expectBroken(bytes.copyOf(cut))
        expectBroken(bytes + byteArrayOf(0))
    }

    @Test fun longStringsAreCut() {
        val g = trip(1)
        val m = g.members.values.first()
        val long = g.copy(members = g.members + (m.id to m.copy(name = "x".repeat(200))))
        assertEquals(Group.MAX_NAME, WireCodec.decode(WireCodec.encode(long)).members.getValue(m.id).name.length)
    }

    @Test fun fuzzNeverCrashes() {
        val rnd = Random(42)
        val good = WireCodec.encode(trip(10))
        repeat(10_000) { i ->
            val bytes = if (i % 2 == 0) {
                ByteArray(rnd.nextInt(200)) { rnd.nextInt(256).toByte() }.also { if (it.isNotEmpty()) it[0] = 1 }
            } else {
                good.copyOf().also { repeat(1 + rnd.nextInt(4)) { _ -> it[rnd.nextInt(it.size)] = rnd.nextInt(256).toByte() } }
            }
            try {
                WireCodec.decode(bytes)
            } catch (_: CodecException) {
            } catch (_: NewerFormatException) {
            }
        }
    }

    private fun expectBroken(bytes: ByteArray) {
        try {
            WireCodec.decode(bytes)
            fail("decoded")
        } catch (_: CodecException) {
        }
    }
}
