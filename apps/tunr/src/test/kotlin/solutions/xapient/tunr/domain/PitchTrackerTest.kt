package solutions.xapient.tunr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PitchTrackerTest {
    private fun hz(midi: Int, cents: Float = 0f) = (440.0 * 2.0.pow((midi - 69) / 12.0 + cents / 1200.0)).toFloat()
    private fun tone(midi: Int, cents: Float = 0f) = Frame(hz(midi, cents), -30f)
    private val silence = Frame(0f, -70f)

    @Test fun settlesOnNoteAndCents() {
        val t = PitchTracker()
        var r = Reading()
        repeat(10) { r = t.push(tone(69, 12f)) }
        assertTrue(r.active)
        assertEquals(69, r.target)
        assertEquals(12f, r.cents, 0.01f)
        assertEquals(hz(69, 12f), r.hz, 0.01f)
        assertFalse(r.inTune)
    }

    // einzelner Oktavsprung wird verworfen
    @Test fun singleOutlierIgnored() {
        val t = PitchTracker()
        repeat(6) { t.push(tone(45, -4f)) }
        val r = t.push(tone(57, -4f))
        assertEquals(45, r.target)
        assertEquals(-4f, r.cents, 0.01f)
        val back = t.push(tone(45, -4f))
        assertEquals(45, back.target)
    }

    // neuer Ton: nach zwei Werten sofort eingerastet, ohne Nachziehen
    @Test fun newNoteLocksFast() {
        val t = PitchTracker()
        repeat(6) { t.push(tone(45)) }
        t.push(tone(50, 20f))
        val r = t.push(tone(50, 20f))
        assertEquals(50, r.target)
        assertEquals(20f, r.cents, 0.01f)
    }

    // Median: kurzer Ausreisser innerhalb des Halbtons bewegt die Anzeige kaum
    @Test fun medianDampsJitter() {
        val t = PitchTracker()
        repeat(6) { t.push(tone(57, 0f)) }
        val r = t.push(tone(57, 40f))
        assertEquals(0f, r.cents, 0.5f)
    }

    @Test fun silenceWithHysteresis() {
        val t = PitchTracker()
        repeat(5) { t.push(tone(64)) }
        // kurze Luecken halten den Ton
        repeat(PitchTracker.QUIET_FRAMES - 1) { assertTrue(t.push(silence).active) }
        assertTrue(t.push(tone(64)).active)
        repeat(PitchTracker.QUIET_FRAMES) { t.push(silence) }
        val r = t.push(silence)
        assertFalse(r.active)
        assertTrue("letzte Note bleibt", r.heard)
        assertEquals(64, r.target)
        // Pegel-Tor: -53 dB oeffnet nicht, haelt aber offen
        val q = PitchTracker()
        assertFalse(q.push(Frame(hz(64), -53f)).active)
        assertTrue(q.push(Frame(hz(64), -45f)).active)
        assertTrue(q.push(Frame(hz(64), -53f)).active)
    }

    @Test fun inTuneHysteresisAndLockCount() {
        val t = PitchTracker()
        repeat(8) { t.push(tone(69, 10f)) }
        assertFalse(t.reading.inTune)
        repeat(12) { t.push(tone(69, 4f)) }
        assertTrue(t.reading.inTune)
        assertEquals(1, t.reading.locks)
        // 6 Cent: bleibt drin (raus erst ueber 7)
        repeat(12) { t.push(tone(69, 6f)) }
        assertTrue(t.reading.inTune)
        repeat(12) { t.push(tone(69, 9f)) }
        assertFalse(t.reading.inTune)
        repeat(12) { t.push(tone(69, 0f)) }
        assertEquals(2, t.reading.locks)
    }

    // chromatisch: Note wechselt erst 55 Cent neben der alten
    @Test fun chromaticNoteHold() {
        val t = PitchTracker()
        repeat(8) { t.push(tone(60, 40f)) }
        repeat(14) { t.push(tone(60, 52f)) }
        assertEquals(60, t.reading.target)
        assertEquals(52f, t.reading.cents, 0.5f)
    }

    @Test fun guitarPicksStringAndHolds() {
        val t = PitchTracker(instrument = Instrument.GUITAR)
        repeat(8) { t.push(tone(45, -23f)) }
        assertEquals(1, t.reading.string)
        assertEquals(45, t.reading.target)
        assertEquals(-23f, t.reading.cents, 0.01f)
        // weit verstimmte Saite: Cent ueber 50 relativ zur Saite
        val low = PitchTracker(instrument = Instrument.GUITAR)
        repeat(8) { low.push(tone(40, -180f)) }
        assertEquals(0, low.reading.string)
        assertEquals(-180f, low.reading.cents, 0.01f)
    }

    @Test fun lockedStringWins() {
        val t = PitchTracker(instrument = Instrument.GUITAR)
        t.lock(0)
        assertEquals(40, t.reading.target)
        repeat(8) { t.push(tone(45)) }
        assertEquals(0, t.reading.string)
        assertEquals(500f, t.reading.cents, 0.01f)
        t.lock(-1)
        assertEquals(1, t.reading.string)
        assertEquals(0f, t.reading.cents, 0.01f)
        // Instrumentwechsel loest die Sperre
        t.lock(2)
        t.configure(440, Instrument.BASS)
        assertEquals(-1, t.locked)
    }

    @Test fun a4ChangesCents() {
        val t = PitchTracker(a4 = 442)
        repeat(8) { t.push(Frame(440f, -30f)) }
        assertEquals(69, t.reading.target)
        assertEquals(-7.85f, t.reading.cents, 0.05f)
        t.configure(440, Instrument.CHROMATIC)
        assertFalse(t.reading.active)
        repeat(8) { t.push(Frame(440f, -30f)) }
        assertEquals(0f, t.reading.cents, 0.01f)
    }

    @Test fun stopKeepsLastNoteInactive() {
        val t = PitchTracker()
        repeat(5) { t.push(tone(52)) }
        t.stop()
        assertFalse(t.reading.active)
        assertEquals(52, t.reading.target)
        assertFalse(t.reading.inTune)
    }
}
