package solutions.xapient.pulsr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class BeatClockTest {
    // Renderer schreibt den Stand je Block in die Uhr, wie im Audio-Thread
    private fun played(s: Settings, frames: Int, block: Int = 256, rate: Int = 1_024, capacity: Int = 256): Pair<BeatClock, Renderer> {
        val clock = BeatClock(rate, capacity)
        val r = Renderer(rate, s, clock)
        val buf = FloatArray(block)
        var at = 0
        while (at < frames) {
            r.render(buf, block)
            at += block
        }
        return clock to r
    }

    @Test fun nothingBeforeAudio() {
        assertNull(BeatClock(48_000).at(100))
        val (clock, _) = played(Settings(), 256)
        assertNull(clock.at(-1))
    }

    @Test fun beatAndPhaseFromFrame() {
        // 60 BPM bei 1024 Hz: ein Schlag = 1024 Frames
        val (clock, _) = played(Settings(bpm = 60, beats = 3), 8_192)
        val p = clock.at(1_024 * 4 + 256)!!
        assertEquals(1, p.beat)
        assertEquals(0.25f, p.phase, 1e-4f)
        assertEquals(250f, p.sinceBeatMs, 0.5f)
        assertEquals(1_000f, p.beatMs, 0.5f)
        assertFalse(p.accent)
        val down = clock.at(1_024 * 6 + 10)!!
        assertEquals(0, down.beat)
        assertTrue(down.accent)
        assertEquals((0 + 10f / 1_024) / 3, down.barPhase, 1e-4f)
    }

    // Uhr wird zwischen den Bloecken weitergerechnet, auch ueber eine Schlaggrenze
    @Test fun extrapolatesInsideBlockAcrossBeat() {
        val (clock, _) = played(Settings(bpm = 60, beats = 4), 4_096, block = 1_536)
        val p = clock.at(2_048 + 100)!!
        assertEquals(2, p.beat)
    }

    @Test fun subIndexAndSince() {
        val (clock, _) = played(Settings(bpm = 60, sub = Subdivision.SIXTEENTH), 2_048)
        val p = clock.at(256 * 2 + 64)!!
        assertEquals(2, p.subIndex)
        assertEquals(62.5f, p.sinceSubMs, 0.5f)
    }

    // Tempowechsel: Zustand vor dem Wechsel bleibt fuer schon gespielte Frames gueltig
    @Test fun tempoChangeKeepsOldFrames() {
        val clock = BeatClock(1_024)
        val r = Renderer(1_024, Settings(bpm = 60), clock)
        val buf = FloatArray(512)
        r.render(buf, 512)
        r.apply(Settings(bpm = 120))
        r.render(buf, 512)
        r.render(buf, 512)
        // Frame 256: noch altes Tempo, Viertel des Schlags
        assertEquals(0.25f, clock.at(256)!!.phase, 1e-4f)
        // ab 512 doppelt so schnell: halber Schlag Rest in 256 Frames -> Schlag 1 bei 768
        assertEquals(1, clock.at(800)!!.beat)
        assertEquals(0, clock.at(760)!!.beat)
    }

    // Eigenschaft: am Klick-Frame zeigt die Uhr genau diesen Schlag ab 0, ein Frame davor noch den alten.
    // Zufaellige Wechsel von Tempo, Schlaegen und Unterteilung mitten im Schlag, krumme Raten und Bloecke.
    @Test fun clockFlipsExactlyOnClickFrames() {
        val rnd = kotlin.random.Random(42)
        for (rate in listOf(44_100, 48_000, 96_000)) {
            val clicks = ArrayList<Pair<Long, Int>>()
            val clock = BeatClock(rate, capacity = 64)
            var s = Settings(bpm = 97, beats = 4, sub = Subdivision.EIGHTH)
            val r = Renderer(rate, s, clock, leadIn = rate / 15) { f, kind, beat -> if (kind != Kind.SUB) clicks += f to beat }
            val buf = FloatArray(2_048)
            val frameMs = 1_000f / rate
            var checked = 0
            var prevBeat = -1
            repeat(6_000) {
                if (rnd.nextInt(6) == 0) {
                    s = s.copy(
                        bpm = rnd.nextInt(Tempo.MIN, Tempo.MAX + 1),
                        beats = rnd.nextInt(Beats.MIN, Beats.MAX + 1),
                        sub = Subdivision.entries.random(rnd),
                    )
                    r.apply(s)
                }
                r.render(buf, rnd.nextInt(32, 2_049))
                // Uhr sofort pruefen, solange der Block noch im Ringpuffer liegt
                for ((f, beat) in clicks) {
                    val p = clock.at(f)!!
                    assertEquals(beat, p.beat, "Frame $f")
                    assertTrue(p.sinceBeatMs < frameMs + 1e-3f, "seit Schlag ${p.sinceBeatMs} ms")
                    val before = clock.at(f - 1)
                    if (before != null) {
                        // noch im alten Schlag, kurz vor seinem Ende
                        assertTrue((1f - before.phase) * before.beatMs < frameMs * 1.01f + 1e-3f, "vor $f: ${before.phase}")
                        if (prevBeat >= 0 && before.beats == p.beats && p.beats > 1) assertTrue(before.beat != beat)
                    }
                    prevBeat = beat
                    checked++
                }
                clicks.clear()
            }
            assertTrue(checked > 150, "nur $checked Klicks")
        }
    }

    @Test fun oldFramesFallBackToOldestKnown() {
        val (clock, _) = played(Settings(bpm = 60), 1_024 * 10, block = 128, capacity = 8)
        // aelter als der Ringpuffer: kein Absturz, sinnvoller Wert
        val p = clock.at(0)
        assertTrue(p == null || p.beat in 0..3)
    }

    @Test fun clearForgets() {
        val (clock, _) = played(Settings(), 1_024)
        clock.clear()
        assertNull(clock.at(10))
    }

    @Test fun presentedFrameFromTimestamp() {
        assertEquals(48_000L + 24_000L, BeatClock.presented(48_000, 1_000_000_000L, 1_500_000_000L, 48_000))
        assertEquals(1_000L, BeatClock.presented(1_000, 5L, 5L, 48_000))
    }
}
