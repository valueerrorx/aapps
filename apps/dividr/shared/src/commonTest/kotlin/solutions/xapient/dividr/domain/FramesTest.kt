package solutions.xapient.dividr.domain

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.random.Random

class FramesTest {
    @Test fun base45RfcVectors() {
        assertEquals("BB8", Base45.encode("AB".encodeToByteArray()))
        assertEquals("%69 VD92EX0", Base45.encode("Hello!!".encodeToByteArray()))
        assertEquals("UJCLQE7W581", Base45.encode("base-45".encodeToByteArray()))
        assertEquals("ietf!", Base45.decode("QED8WEX0")!!.decodeToString())
        assertContentEquals(ByteArray(0), Base45.decode(""))
    }

    @Test fun base45RejectsGarbage() {
        assertNull(Base45.decode("GGW")) // 16 + 16*45 + 32*2025 > 65535
        assertNull(Base45.decode("abc"))
        assertNull(Base45.decode("A"))
        assertNull(Base45.decode("https://example.org"))
    }

    @Test fun base45RoundTripRandom() {
        val rnd = Random(1)
        repeat(200) {
            val b = ByteArray(rnd.nextInt(50)) { rnd.nextInt(256).toByte() }
            assertContentEquals(b, Base45.decode(Base45.encode(b)))
        }
    }

    private fun big(): Group = CodecTest().trip(150, seed = 5)

    // Notfall-Fallback erzwingen: kleine Teile
    private fun split(g: Group) = Frames.pack(WireCodec.encode(g), chunk = 300)

    @Test fun singlePart() {
        val g = CodecTest().trip(60)
        val parts = Frames.pack(g)
        assertEquals(1, parts.size)
        assertEquals(ScanResult.Done(g), FrameCollector().accept(parts[0]))
    }

    @Test fun multiPartShuffledWithDuplicates() {
        val g = big()
        val parts = split(g)
        assertTrue(parts.size > 2)
        val c = FrameCollector()
        val order = (parts + parts.take(2)).shuffled(Random(3)).toMutableList()
        var result: ScanResult? = null
        for (p in order) {
            result = c.accept(p)
            if (result is ScanResult.Done) break
            assertTrue(result is ScanResult.Progress)
        }
        assertEquals(ScanResult.Done(g), result)
    }

    @Test fun progressCountsDistinctParts() {
        val parts = split(big())
        val c = FrameCollector()
        c.accept(parts[1])
        val r = c.accept(parts[1])
        assertEquals(ScanResult.Progress(setOf(1), parts.size), r)
    }

    @Test fun crcChangeRestarts() {
        val old = split(big())
        val g2 = big().let { it.putEntry(Fx.expense(Dev(5), it.activeMembers()[0].id, 99), 5) }
        val new = split(g2)
        val c = FrameCollector()
        c.accept(old[0])
        c.accept(old[1])
        // Sender hat inzwischen etwas geaendert: Sammlung beginnt neu
        val r = c.accept(new[2])
        assertEquals(ScanResult.Progress(setOf(2), new.size), r)
        val last = new.indices.filter { it != 2 }.map { c.accept(new[it]) }.last()
        assertEquals(ScanResult.Done(g2), last)
    }

    @Test fun foreignCodes() {
        val c = FrameCollector()
        assertEquals(ScanResult.NotOurs, c.accept("https://example.org/x"))
        assertEquals(ScanResult.NotOurs, c.accept("HELLO WORLD"))
        assertEquals(ScanResult.NotOurs, c.accept(Base45.encode("XY123456789".encodeToByteArray())))
    }

    @Test fun newerFrameVersion() {
        val raw = Base45.decode(Frames.pack(CodecTest().trip(3))[0])!!
        raw[1] = 9
        assertEquals(ScanResult.Newer, FrameCollector().accept(Base45.encode(raw)))
    }

    @Test fun brokenPayload() {
        val raw = Base45.decode(Frames.pack(CodecTest().trip(3))[0])!!
        raw[raw.size - 3] = (raw[raw.size - 3].toInt() xor 0x55).toByte()
        assertEquals(ScanResult.Broken, FrameCollector().accept(Base45.encode(raw)))
        val bad = raw.copyOf().also { it[7] = 0 }
        assertEquals(ScanResult.Broken, FrameCollector().accept(Base45.encode(bad)))
    }

    @Test fun zipBombIsRejected() {
        val z = Frames.compress(ByteArray(Frames.MAX_INFLATED + 10))
        assertNull(Frames.inflate(z))
        assertNull(Frames.inflate(byteArrayOf(1, 2, 3, 4)))
    }

    @Test fun fuzzFramesNeverCrash() {
        val rnd = Random(9)
        val c = FrameCollector()
        repeat(5_000) {
            val raw = ByteArray(8 + rnd.nextInt(60)) { rnd.nextInt(256).toByte() }
            raw[0] = Frames.MAGIC.toByte(); raw[1] = 1
            c.accept(Base45.encode(raw))
        }
    }

    @Test fun tooBigForQr() {
        val rnd = Random(2)
        assertTrue(Frames.pack(ByteArray(Frames.CHUNK * 65) { rnd.nextInt(256).toByte() }).isEmpty())
    }
}
