package solutions.xapient.roundr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    @Test fun elapsedFollowsMonotonicClock() {
        val s = Session.start(1_000)
        assertEquals(0, s.elapsed(1_000))
        assertEquals(4_250, s.elapsed(5_250))
        // Uhr laeuft nie rueckwaerts in die Rechnung
        assertEquals(0, s.elapsed(500))
    }

    @Test fun pauseFreezesResumeContinues() {
        val p = Session.start(0).pause(7_000)
        assertFalse(p.running)
        assertEquals(7_000, p.elapsed(100_000))
        val r = p.resume(100_000)
        assertEquals(9_000, r.elapsed(102_000))
        assertSame(r, r.resume(200_000))
        assertSame(p, p.pause(300_000))
    }

    // Viele Pausen: kein Aufsummieren von Ticks, also kein Drift
    @Test fun manyPauseResumeCyclesDoNotDrift() {
        var s = Session.start(0)
        var now = 0L
        repeat(1000) {
            now += 333
            s = s.pause(now)
            now += 1_000
            s = s.resume(now)
        }
        now += 1
        assertEquals(1000 * 333L + 1, s.elapsed(now))
    }

    @Test fun seekKeepsRunState() {
        val running = Session.start(0).seek(5_000, 30_000)
        assertTrue(running.running)
        assertEquals(31_000, running.elapsed(6_000))
        val paused = Session(10_000, null).seek(99, 40_000)
        assertFalse(paused.running)
        assertEquals(40_000, paused.elapsed(1_000_000))
        assertEquals(0, Session(10_000, null).seek(0, -5).elapsed(0))
    }

    @Test fun rebootPausesAtLastOffset() {
        // laeuftSeit liegt nach „jetzt": Uhr wurde zurueckgesetzt
        val s = Session(12_000, 900_000).restored(3_000)
        assertFalse(s.running)
        assertEquals(12_000, s.elapsed(3_000))
        val ok = Session(12_000, 1_000)
        assertSame(ok, ok.restored(3_000))
    }

    @Test fun runFinished() {
        val w = Workout(prepSec = 0, workSec = 5, restSec = 0, rounds = 2)
        val r = Run(w, Session.start(0), 1)
        assertFalse(r.finished(9_999))
        assertTrue(r.finished(10_000))
    }
}
