package solutions.xapient.pulsr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TapTempoTest {
    private fun taps(t: TapTempo, vararg ms: Long): Int? = ms.fold(null as Int?) { _, m -> t.tap(m) }

    @Test fun firstTapGivesNothing() = assertNull(TapTempo().tap(1_000))

    @Test fun twoTapsGiveTempo() = assertEquals(120, taps(TapTempo(), 0, 500))

    @Test fun meanOfIntervals() {
        // 480, 520, 500, 500 -> 500 ms = 120 BPM
        assertEquals(120, taps(TapTempo(), 0, 480, 1_000, 1_500, 2_000))
    }

    @Test fun outlierIgnored() {
        // ein verpatzter Tipp (900 ms) zaehlt nicht
        assertEquals(120, taps(TapTempo(), 0, 500, 1_000, 1_900, 2_400, 2_900))
    }

    @Test fun onlyRecentIntervalsCount() {
        // erst 100 BPM, dann langsam auf 120: die letzten 6 Abstaende bestimmen das Tempo
        val t = TapTempo()
        var at = 0L
        t.tap(at)
        repeat(6) { at += 600; t.tap(at) }
        var bpm: Int? = null
        repeat(6) { at += 500; bpm = t.tap(at) }
        assertEquals(120, bpm)
    }

    @Test fun resetAfterPause() {
        val t = TapTempo()
        taps(t, 0, 600, 1_200)
        // 4 s Pause: neue Messung, der erste Tipp danach liefert nichts
        assertNull(t.tap(5_200))
        assertEquals(150, t.tap(5_600))
    }

    @Test fun slowestTempoStillTappable() = assertEquals(20, taps(TapTempo(), 0, 3_000, 6_000))

    @Test fun clampsToRange() {
        assertEquals(Tempo.MAX, taps(TapTempo(), 0, 100, 200))
    }

    @Test fun suddenTempoChangeRestarts() {
        // zwei Abstaende, die beide weit vom Median liegen: neu ab dem letzten
        assertEquals(60, taps(TapTempo(), 0, 400, 1_400))
    }

    @Test fun timeGoingBackwardsRestarts() {
        val t = TapTempo()
        taps(t, 1_000, 1_500)
        assertNull(t.tap(900))
    }

    @Test fun explicitReset() {
        val t = TapTempo()
        t.tap(0)
        t.reset()
        assertNull(t.tap(500))
    }
}
