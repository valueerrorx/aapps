package solutions.xapient.tunr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTest {
    @Test fun namesInAllNotations() {
        assertEquals("A4", Notes.name(69, Notation.SHARP).toString())
        assertEquals("C4", Notes.name(60, Notation.SHARP).toString())
        assertEquals("E1", Notes.name(28, Notation.SHARP).toString())
        assertEquals("C8", Notes.name(108, Notation.SHARP).toString())
        assertEquals("F♯3", Notes.name(54, Notation.SHARP).toString())
        assertEquals("G♭3", Notes.name(54, Notation.FLAT).toString())
        assertEquals("B♭3", Notes.name(58, Notation.FLAT).toString())
        assertEquals("A♯3", Notes.name(58, Notation.SHARP).toString())
        // deutsch: H statt B, B statt B♭, sonst Kreuze
        assertEquals("B3", Notes.name(58, Notation.GERMAN).toString())
        assertEquals("H3", Notes.name(59, Notation.GERMAN).toString())
        assertEquals("C♯4", Notes.name(61, Notation.GERMAN).toString())
        assertEquals("B", Notes.name(59, Notation.SHARP).letter)
        assertEquals(Accidental.SHARP, Notes.name(61, Notation.SHARP).accidental)
    }

    @Test fun frequencies() {
        assertEquals(440f, Notes.hz(69, 440f), 1e-3f)
        assertEquals(41.2034f, Notes.hz(28, 440f), 1e-3f)
        assertEquals(4186.01f, Notes.hz(108, 440f), 0.01f)
        assertEquals(432f, Notes.hz(69, 432f), 1e-3f)
        assertEquals(0f, Notes.centsFromA4(440f, 440f), 1e-4f)
        assertEquals(-1200f, Notes.centsFromA4(220f, 440f), 1e-3f)
        // A4 = 442: ein 440-Hz-Ton ist ~7,85 Cent zu tief
        assertEquals(-7.85f, Notes.centsFromA4(440f, 442f), 0.01f)
    }

    @Test fun nearestString() {
        val g = Instrument.GUITAR
        assertEquals(0, g.nearest(Notes.centsFromA4(83f, 440f)))
        assertEquals(1, g.nearest(Notes.centsFromA4(108f, 440f)))
        assertEquals(5, g.nearest(Notes.centsFromA4(330f, 440f)))
        assertEquals(-1, Instrument.CHROMATIC.nearest(0f))
        // Drop D: tiefe Saite D2
        assertEquals(0, Instrument.GUITAR_DROP_D.nearest(Notes.centsFromA4(73.4f, 440f)))
        // Ukulele re-entrant: G4 ist die erste Saite
        assertEquals(0, Instrument.UKULELE.nearest(Notes.centsFromA4(392f, 440f)))
        assertEquals(1, Instrument.UKULELE.nearest(Notes.centsFromA4(261.6f, 440f)))
        assertEquals(listOf(28, 33, 38, 43), Instrument.BASS.strings)
    }

    @Test fun settingsAndMicAccess() {
        assertEquals(415, Settings(a4 = 300).sane().a4)
        assertEquals(466, Settings(a4 = 500).sane().a4)
        assertEquals(MicAccess.GRANTED, micAccess(granted = true, deniedWithoutDialog = true, showRationale = false))
        // nie gefragt oder Dialog nur weggetippt: weiter fragen
        assertEquals(MicAccess.ASK, micAccess(granted = false, deniedWithoutDialog = false, showRationale = false))
        // einmal abgelehnt: System zeigt den Dialog noch
        assertEquals(MicAccess.ASK, micAccess(granted = false, deniedWithoutDialog = true, showRationale = true))
        assertEquals(MicAccess.BLOCKED, micAccess(granted = false, deniedWithoutDialog = true, showRationale = false))
    }
}
