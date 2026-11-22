package solutions.xapient.roundr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodecTest {
    @Test fun roundTrip() {
        val w = Workout("Beine", 5, 45, 15, 6, 2, 90)
        assertEquals(w, Codec.decode(Codec.encode(w)))
        assertEquals(Presets.default, Codec.decode(Codec.encode(Presets.default)))
    }

    @Test fun brokenDataIsDropped() {
        assertNull(Codec.decode(null))
        assertNull(Codec.decode(""))
        assertNull(Codec.decode("w1|x|1|2|3"))
        assertNull(Codec.decode("w9|x|1|2|3|4|5|6"))
        assertNull(Codec.decode("w1|x|1|zwei|3|4|5|6"))
        // Werte ausserhalb der Grenzen werden eingefangen
        assertEquals(99, Codec.decode("w1|x|0|20|10|5000|1|0")!!.rounds)
    }

    @Test fun listSkipsBadLinesPresetsAndDuplicates() {
        val good = Workout("A", workSec = 30)
        val s = listOf(Codec.encode(good), "kaputt", Codec.encode(Presets.default), Codec.encode(good.copy(workSec = 40)), Codec.encode(Workout(""))).joinToString("\n")
        assertEquals(listOf(good), Codec.decodeList(s))
        assertEquals(emptyList<Workout>(), Codec.decodeList(null))
    }

    @Test fun saveReplacesSameNameAndPutsFirst() {
        val a = Workout("A")
        val b = Workout("B")
        val list = Codec.save(listOf(a, b), Presets.default.copy(workSec = 30), " B ")!!
        assertEquals(listOf("B", "A"), list.map { it.name })
        assertEquals(30, list[0].workSec)
        assertNull(Codec.save(list, a, "  "))
    }

    @Test fun saveRefusesWhenFull() {
        val full = (1..Codec.MAX_SAVED).map { Workout("n$it") }
        assertNull(Codec.save(full, Workout(), "neu"))
        // gleichen Namen ueberschreiben geht trotzdem
        assertEquals(Codec.MAX_SAVED, Codec.save(full, Workout(workSec = 30), "n3")!!.size)
    }

    @Test fun runRoundTrip() {
        val r = Run(Workout("A", 5, 30, 10, 4), Session(12_345, 99_000), 42)
        val back = Codec.decodeRun(Codec.encodeRun(r))!!
        assertEquals(r.workout, back.workout)
        assertEquals(r.session, back.session)
        assertEquals(42, back.id)
        val paused = r.copy(session = Session(7_000, null))
        assertEquals(paused.session, Codec.decodeRun(Codec.encodeRun(paused))!!.session)
        assertNull(Codec.decodeRun("r1|x|0|0|w1"))
        assertNull(Codec.decodeRun("r1|1|-5|0|" + Codec.encode(Workout())))
        assertNull(Codec.decodeRun(null))
    }
}
