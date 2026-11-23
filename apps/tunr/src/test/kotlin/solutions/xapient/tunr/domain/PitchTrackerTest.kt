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
        q.push(Frame(hz(64), -45f))
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

    // erster Wert nach Stille: erst mit Bestaetigung aktiv, kein Einrasten (Haptik, Glanz) durch Einzelwerte
    @Test fun firstFrameAfterSilenceNeedsConfirmation() {
        val t = PitchTracker()
        val first = t.push(tone(69, 1f))
        assertFalse(first.active)
        assertFalse(first.heard)
        assertEquals(0, first.locks)
        // einzelner Ausreisser, dann Stille: nichts angezeigt
        t.push(silence)
        assertFalse(t.push(tone(69, 1f)).active)
        assertFalse(t.push(tone(57, 1f)).active)
        val ok = t.push(tone(57, 2f))
        assertTrue(ok.active)
        assertEquals(57, ok.target)
        assertEquals(2f, ok.cents, 0.01f)
        // nach Stille wieder von vorn
        repeat(PitchTracker.QUIET_FRAMES + 1) { t.push(silence) }
        val again = t.push(tone(64))
        assertFalse(again.active)
        assertEquals("alte Note bleibt gedimmt", 57, again.target)
        assertTrue(t.push(tone(64)).active)
    }

    @Test fun instrumentsPickStrings() {
        fun settle(i: Instrument, midi: Int, cents: Float): Reading {
            val t = PitchTracker(instrument = i)
            repeat(8) { t.push(tone(midi, cents)) }
            return t.reading
        }
        // Drop D: tiefe Saite auf D2; noch auf E2 gestimmt = 200 Cent zu hoch
        settle(Instrument.GUITAR_DROP_D, 38, 8f).let { assertEquals(0, it.string); assertEquals(38, it.target); assertEquals(8f, it.cents, 0.01f) }
        settle(Instrument.GUITAR_DROP_D, 40, 0f).let { assertEquals(0, it.string); assertEquals(200f, it.cents, 0.01f) }
        // Bass: E1 bis G2
        settle(Instrument.BASS, 28, -12f).let { assertEquals(0, it.string); assertEquals(-12f, it.cents, 0.01f) }
        settle(Instrument.BASS, 43, 3f).let { assertEquals(3, it.string); assertTrue(it.inTune) }
        // Ukulele rueckwaerts gestimmt: G4 ist Saite 1, C4 Saite 2
        settle(Instrument.UKULELE, 67, 0f).let { assertEquals(0, it.string); assertEquals(67, it.target) }
        settle(Instrument.UKULELE, 60, -20f).let { assertEquals(1, it.string); assertEquals(-20f, it.cents, 0.01f) }
        settle(Instrument.UKULELE, 69, 2f).let { assertEquals(3, it.string) }
        // Violine G3 D4 A4 E5
        settle(Instrument.VIOLIN, 55, -30f).let { assertEquals(0, it.string) }
        settle(Instrument.VIOLIN, 76, 3f).let { assertEquals(3, it.string); assertTrue(it.inTune) }
    }

    @Test fun stopKeepsLastNoteInactive() {
        val t = PitchTracker()
        repeat(5) { t.push(tone(52)) }
        t.stop()
        assertFalse(t.reading.active)
        assertEquals(52, t.reading.target)
        assertFalse(t.reading.inTune)
    }

    // Referenzton: Anzeige aus, eigener Ton zaehlt nicht, danach laeuft die Erkennung von selbst weiter
    @Test fun referenceToneHushesDetection() {
        val t = PitchTracker(instrument = Instrument.GUITAR)
        repeat(6) { t.push(tone(45, -20f)) }
        assertTrue(t.reading.active)
        t.hush(Pluck.HUSH_FRAMES)
        assertFalse(t.reading.active)
        val before = t.reading
        repeat(Pluck.HUSH_FRAMES) { assertEquals(before, t.push(tone(40))) }
        // zwei passende Werte nach dem Ausklingen: wieder aktiv, mit dem echten Ton
        t.push(tone(45, -20f))
        val r = t.push(tone(45, -20f))
        assertTrue(r.active)
        assertEquals(1, r.string)
        assertEquals(-20f, r.cents, 0.01f)
    }

    // Mikrofon aus (stop) beendet die Pause sofort; erneutes Anschlagen verlaengert sie
    @Test fun hushEndsOnStopAndRestarts() {
        val t = PitchTracker()
        t.hush(Pluck.HUSH_FRAMES)
        t.stop()
        repeat(2) { t.push(tone(57)) }
        assertTrue(t.reading.active)
        t.hush(10)
        repeat(8) { t.push(tone(57)) }
        t.hush(10)
        repeat(10) { assertFalse(t.push(tone(57)).active) }
        repeat(2) { t.push(tone(57)) }
        assertTrue(t.reading.active)
        // Lauflaenge passt zum Ton: ganzer Ton plus Nachlauf, in Analysen zu 25 ms
        assertTrue(Pluck.HUSH_FRAMES * 0.025f >= Pluck.SECONDS + Pluck.TAIL_S - 1e-3f)
    }
}
