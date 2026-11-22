package solutions.xapient.pulsr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MeterTest {
    @Test fun markingsCoverRangeWithoutGaps() {
        assertEquals("Grave", Tempo.marking(20))
        assertEquals("Largo", Tempo.marking(40))
        assertEquals("Adagio", Tempo.marking(72))
        assertEquals("Andante", Tempo.marking(96))
        assertEquals("Moderato", Tempo.marking(112))
        assertEquals("Allegro", Tempo.marking(120))
        assertEquals("Presto", Tempo.marking(180))
        assertEquals("Prestissimo", Tempo.marking(300))
        // jede Grenze wechselt genau einmal
        val changes = (Tempo.MIN..Tempo.MAX).map(Tempo::marking).zipWithNext().count { (a, b) -> a != b }
        assertEquals(9, changes)
    }

    @Test fun saneClamps() {
        val s = Settings(bpm = 5, beats = 40).sane()
        assertEquals(Tempo.MIN, s.bpm)
        assertEquals(Beats.MAX, s.beats)
        assertEquals(Settings(), Settings().sane())
    }
}
