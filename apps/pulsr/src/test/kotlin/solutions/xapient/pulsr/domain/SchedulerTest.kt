package solutions.xapient.pulsr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToLong

class SchedulerTest {
    data class Ev(val frame: Long, val kind: Kind, val beat: Int, val sub: Int)

    // Bloecke beliebiger Groesse, Ereignisse mit absolutem Frame
    private fun run(s: Scheduler, frames: Long, block: Int = 480, between: ((Long) -> Unit)? = null): List<Ev> {
        val out = ArrayList<Ev>()
        var done = 0L
        while (done < frames) {
            between?.invoke(done)
            val n = minOf(block.toLong(), frames - done).toInt()
            val start = s.frame
            s.advance(n) { off, kind, beat, sub -> out += Ev(start + off, kind, beat, sub) }
            done += n
        }
        return out
    }

    // Klick liegt auf dem ersten Sample ab dem idealen Zeitpunkt (Rundung: hoechstens 1 Sample)
    private fun assertFrames(expected: List<Long>, ev: List<Ev>) {
        assertEquals("Anzahl", expected.size, ev.size)
        expected.zip(ev).forEach { (e, a) -> assertTrue("erwartet $e, war ${a.frame}", a.frame - e in 0..1) }
    }

    @Test fun firstClickOnFrameZeroAccented() {
        val ev = run(Scheduler(48_000, Settings(bpm = 120)), 10)
        assertEquals(listOf(Ev(0, Kind.ACCENT, 0, 0)), ev)
    }

    @Test fun beatSpacingAt120() {
        val ev = run(Scheduler(48_000, Settings(bpm = 120, beats = 4)), 48_000 * 3)
        assertFrames(listOf(0L, 24_000L, 48_000L, 72_000L, 96_000L, 120_000L), ev)
        assertEquals(listOf(Kind.ACCENT, Kind.BEAT, Kind.BEAT, Kind.BEAT, Kind.ACCENT, Kind.BEAT), ev.map { it.kind })
        assertEquals(listOf(0, 1, 2, 3, 0, 1), ev.map { it.beat })
    }

    @Test fun blockSizeDoesNotMatter() {
        val a = run(Scheduler(44_100, Settings(bpm = 137, sub = Subdivision.TRIPLET)), 44_100 * 20L, block = 64)
        val b = run(Scheduler(44_100, Settings(bpm = 137, sub = Subdivision.TRIPLET)), 44_100 * 20L, block = 1013)
        assertEquals(a.map { it.copy(frame = 0) }, b.map { it.copy(frame = 0) })
        a.zip(b).forEach { (x, y) -> assertTrue(abs(x.frame - y.frame) <= 1) }
    }

    @Test fun subdivisionsBetweenBeats() {
        val ev = run(Scheduler(48_000, Settings(bpm = 60, beats = 2, sub = Subdivision.SIXTEENTH)), 48_000)
        assertFrames(listOf(0L, 12_000L, 24_000L, 36_000L), ev)
        assertEquals(listOf(Kind.ACCENT, Kind.SUB, Kind.SUB, Kind.SUB), ev.map { it.kind })
        assertEquals(listOf(0, 1, 2, 3), ev.map { it.sub })
    }

    // Triolen bei 70 BPM: Abstand 13714,29 Frames, also nie ganzzahlig -> nie mehr als 1 Sample daneben
    @Test fun tripletsWithinOneSample() {
        val rate = 48_000
        val ev = run(Scheduler(rate, Settings(bpm = 70, sub = Subdivision.TRIPLET)), rate * 30L)
        ev.forEachIndexed { k, e ->
            val ideal = k * rate * 60.0 / 210
            assertTrue("Klick $k bei ${e.frame}, ideal $ideal", e.frame - ideal in -0.001..1.0)
        }
        assertEquals(105, ev.size)
    }

    // 10 Minuten bei krummem Tempo: kein Drift
    @Test fun noDriftOverTenMinutes() {
        val rate = 48_000
        val bpm = 133
        val ev = run(Scheduler(rate, Settings(bpm = bpm)), rate * 600L, block = 192)
        assertEquals(1_330, ev.size)
        ev.forEachIndexed { k, e ->
            val ideal = k * rate * 60.0 / bpm
            assertTrue("Drift bei $k: ${e.frame - ideal}", e.frame - ideal in -0.001..1.0)
        }
    }

    // Tempo verdoppelt in der Mitte eines Schlags: Rest des Schlags laeuft doppelt so schnell
    @Test fun tempoChangeMidBeatStretchesOnlyCurrentBeat() {
        val s = Scheduler(1_024, Settings(bpm = 60))
        val ev = run(s, 3_072, block = 128) { done -> if (done == 512L) s.apply(Settings(bpm = 120)) }
        assertEquals(listOf(0L, 768L, 1_280L, 1_792L, 2_304L, 2_816L), ev.map { it.frame })
    }

    @Test fun manyTempoChangesNeverStumble() {
        val s = Scheduler(48_000, Settings(bpm = 100))
        var bpm = 100
        val ev = run(s, 48_000 * 20L, block = 256) { if (it % 25_600 == 0L) s.apply(Settings(bpm = ++bpm)) }
        val gaps = ev.zipWithNext { a, b -> b.frame - a.frame }
        // Abstand liegt immer zwischen End- und Starttempo
        val min = (48_000 * 60.0 / bpm).roundToLong() - 1
        val max = (48_000 * 60.0 / 100).roundToLong() + 1
        assertTrue(gaps.all { it in min..max })
        assertTrue(bpm > 130)
    }

    @Test fun switchSubdivisionMidBeatSkipsPassedTicks() {
        val s = Scheduler(1_024, Settings(bpm = 60, sub = Subdivision.QUARTER))
        // bei 0,625 Schlag auf Sechzehntel: naechster Klick bei 0,75
        val ev = run(s, 1_152, block = 128) { if (it == 640L) s.apply(Settings(bpm = 60, sub = Subdivision.SIXTEENTH)) }
        assertEquals(listOf(0L to Kind.ACCENT, 768L to Kind.SUB, 1_024L to Kind.BEAT), ev.map { it.frame to it.kind })
    }

    @Test fun fewerBeatsLandsOnNextDownbeat() {
        val s = Scheduler(1_024, Settings(bpm = 60, beats = 6))
        // im fuenften Schlag (Index 4) auf 3/4: naechster Schlag ist die Eins
        val ev = run(s, 6_200, block = 128) { if (it == 4_608L) s.apply(Settings(bpm = 60, beats = 3)) }
        assertEquals(listOf(0, 1, 2, 3, 4, 0, 1), ev.map { it.beat })
        assertEquals(Kind.ACCENT, ev[5].kind)
    }

    @Test fun accentOff() {
        val ev = run(Scheduler(1_000, Settings(bpm = 60, accent = false)), 5_000)
        assertEquals(5, ev.size)
        assertTrue(ev.all { it.kind == Kind.BEAT })
    }

    @Test fun oneBeatBarAlwaysAccented() {
        val ev = run(Scheduler(1_024, Settings(bpm = 120, beats = 1)), 3_072)
        assertTrue(ev.all { it.kind == Kind.ACCENT && it.beat == 0 })
        assertEquals(6, ev.size)
    }

    @Test fun extremesClamped() {
        val fast = run(Scheduler(48_000, Settings(bpm = 999)), 48_000)
        assertEquals(5, fast.size)
        val slow = run(Scheduler(48_000, Settings(bpm = 1)), 48_000 * 4L)
        assertFrames(listOf(0L, 144_000L), slow)
    }
}
