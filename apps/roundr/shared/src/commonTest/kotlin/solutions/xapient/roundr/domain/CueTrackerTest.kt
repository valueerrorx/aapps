package solutions.xapient.roundr.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

// Vorbereitung 10, Arbeit 20, Pause 10, 2 Runden: Start 0, Arbeit 10 s, Pause 30 s, Arbeit 40 s, Fertig 60 s
class CueTrackerTest {
    private val points = Cues.points(Timeline(Workout(prepSec = 10, workSec = 20, restSec = 10, rounds = 2)))
    private val work = Cue.Start(Phase.WORK)

    @Test fun freshStartPlaysStartSignal() {
        val c = CueTracker(points, 0)
        assertEquals(Cue.Start(Phase.PREP), c.onTick(0))
        assertEquals(Cue.Count(3), c.onTick(7_000))
        assertNull(c.onTick(7_500))
    }

    // Prozess-Neustart mitten im Lauf: nichts nachholen, naechste Ansage kommt normal
    @Test fun processRestartDoesNotCatchUp() {
        val c = CueTracker(points, 27_500)
        assertNull(c.onTick(27_600))
        assertEquals(Cue.Count(2), c.onTick(28_000))
    }

    @Test fun skipPlaysStartOfTarget() {
        val c = CueTracker(points, 0)
        c.onTick(2_000)
        c.onSync(10_003, running = true, offset = 10_000)
        assertEquals(work, c.onTick(10_003))
    }

    @Test fun backWithinGraceGoesToPreviousStart() {
        val c = CueTracker(points, 0)
        c.onTick(11_000)
        c.onSync(5, running = true, offset = 0)
        assertEquals(Cue.Start(Phase.PREP), c.onTick(5))
    }

    @Test fun backAfterGraceReplaysCurrentStart() {
        val c = CueTracker(points, 0)
        c.onTick(15_000)
        c.onSync(10_004, running = true, offset = 10_000)
        assertEquals(work, c.onTick(10_004))
    }

    // Tick laeuft noch vor der Sync-Meldung des Sprungs: Signal geht trotzdem nicht verloren
    @Test fun backSurvivesTickBeforeSync() {
        val c = CueTracker(points, 0)
        c.onTick(12_000)
        assertNull(c.onTick(10_002))
        c.onSync(10_002, running = true, offset = 10_000)
        assertEquals(work, c.onTick(10_003))
    }

    @Test fun pauseBeforeOneStillCountsOne() {
        val c = CueTracker(points, 0)
        c.onTick(8_000)
        c.onSync(8_500, running = false, offset = 8_500)
        c.onSync(8_500, running = true, offset = 8_500)
        assertNull(c.onTick(8_500))
        assertEquals(Cue.Count(1), c.onTick(9_000))
        assertEquals(work, c.onTick(10_000))
    }

    @Test fun skipWhilePausedPlaysStartOnResume() {
        val c = CueTracker(points, 0)
        c.onTick(15_000)
        c.onSync(15_000, running = false, offset = 15_000)
        c.onSync(30_000, running = false, offset = 30_000)
        c.onSync(30_000, running = true, offset = 30_000)
        assertEquals(Cue.Start(Phase.REST), c.onTick(30_000))
    }

    @Test fun skipToEndPlaysDone() {
        val c = CueTracker(points, 0)
        c.onTick(45_000)
        c.onSync(60_000, running = true, offset = 60_000)
        assertEquals(Cue.Done, c.onTick(60_000))
        assertNull(c.onTick(60_500))
    }
}
