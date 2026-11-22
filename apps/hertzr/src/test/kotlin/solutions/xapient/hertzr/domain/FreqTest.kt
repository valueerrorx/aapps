package solutions.xapient.hertzr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FreqTest {
    @Test fun rangeAndNyquist() {
        assertEquals(22_000.0, Freq.maxFor(48_000), 0.0)
        assertEquals(21_609.0, Freq.maxFor(44_100), 0.0)
        assertEquals(1.0, Freq.clamp(0.2), 0.0)
        assertEquals(21_609.0, Freq.clamp(30_000.0, Freq.maxFor(44_100)), 0.0)
        assertEquals(440.0, Freq.clamp(Double.NaN), 0.0)
    }

    @Test fun logScaleRoundTrip() {
        assertEquals(0f, Freq.toPos(1.0), 0f)
        assertEquals(1f, Freq.toPos(22_000.0), 0f)
        for (hz in listOf(1.0, 20.0, 440.0, 1_000.0, 20_000.0)) assertEquals(hz, Freq.fromPos(Freq.toPos(hz)), hz * 1e-5)
        // jede Dekade gleich breit
        assertEquals(Freq.toPos(100.0) - Freq.toPos(10.0), Freq.toPos(10_000.0) - Freq.toPos(1_000.0), 1e-6f)
    }

    @Test fun quantizeMatchesDisplay() {
        assertEquals(55.1, Freq.quantize(55.123), 1e-12)
        assertEquals(437.0, Freq.quantize(437.4), 0.0)
        assertEquals(100.0, Freq.quantize(99.96), 0.0)
    }

    @Test fun fineStepsAcrossBoundaries() {
        assertEquals(440.0, Freq.nudge(439.0, 1), 0.0)
        assertEquals(100.0, Freq.nudge(99.9, 1), 0.0)
        assertEquals(99.9, Freq.nudge(100.0, -1), 1e-9)
        assertEquals(10_000.0, Freq.nudge(9_999.0, 1), 0.0)
        assertEquals(10_010.0, Freq.nudge(10_000.0, 1), 0.0)
        assertEquals(9_999.0, Freq.nudge(10_000.0, -1), 0.0)
        // von krummen Werten zum naechsten Rasterpunkt
        assertEquals(10_010.0, Freq.nudge(10_005.0, 1), 0.0)
        assertEquals(10_000.0, Freq.nudge(10_005.0, -1), 0.0)
        assertEquals(1.0, Freq.nudge(1.0, -1), 0.0)
        assertEquals(22_000.0, Freq.nudge(22_000.0, 1), 0.0)
    }

    @Test fun octavesAndRotation() {
        assertEquals(880.0, Freq.octave(440.0, true), 0.0)
        assertEquals(220.0, Freq.octave(440.0, false), 0.0)
        assertEquals(1.0, Freq.octave(1.5, false), 0.0)
        assertEquals(880.0, Freq.rotate(440.0, 360.0), 0.0)
        assertEquals(466.0, Freq.rotate(440.0, 30.0), 0.0)
        assertEquals(220.0, Freq.rotate(440.0, -360.0), 0.0)
    }

    @Test fun notes() {
        assertEquals(Note(9, 4, 0), Freq.note(440.0))
        assertEquals(Note(0, 4, 0), Freq.note(261.63))
        assertEquals(Note(9, 4, 39), Freq.note(450.0))
        assertEquals(Note(10, 4, -12), Freq.note(463.0))
        assertNull(Freq.note(5.0))
        assertEquals(0, Freq.semitone(440.0))
        assertEquals(12, Freq.semitone(880.0))
        assertEquals(-1, Freq.semitone(415.3))
    }

    @Test fun format() {
        assertEquals("440", Freq.format(440.0))
        assertEquals("55.0", Freq.format(55.0))
        assertEquals("1,5", Freq.format(1.5, ','))
        assertEquals("100", Freq.format(99.97))
        assertEquals("20000", Freq.format(20_000.0))
    }

    @Test fun shortWithUnit() {
        assertEquals("55.5 Hz", Freq.short(55.5))
        assertEquals("440 Hz", Freq.short(440.0))
        assertEquals("1,5 kHz", Freq.short(1_500.0, ','))
        assertEquals("1.001 kHz", Freq.short(1_001.0))
        assertEquals("12.345 kHz", Freq.short(12_345.0))
        assertEquals("20 kHz", Freq.short(20_000.0))
    }

    @Test fun parse() {
        assertEquals(440.0, Freq.parse("440")!!, 0.0)
        assertEquals(1_500.0, Freq.parse("1,5k")!!, 0.0)
        assertEquals(12_000.0, Freq.parse(" 12 kHz ")!!, 0.0)
        assertEquals(2_500.0, Freq.parse("2.5KHZ")!!, 0.0)
        assertEquals(50.5, Freq.parse("50.5 Hz")!!, 0.0)
        assertNull(Freq.parse(""))
        assertNull(Freq.parse("0"))
        assertNull(Freq.parse("abc"))
        assertNull(Freq.parse("1.2.3"))
        assertNull(Freq.parse("-5"))
        assertNull(Freq.parse("k"))
    }
}
