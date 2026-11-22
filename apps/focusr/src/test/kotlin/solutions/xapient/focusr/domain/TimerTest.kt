package solutions.xapient.focusr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTest {
    private val min = Timer.MINUTE
    private val t0 = 1_000_000_000L

    @Test fun defaultIsIdle25() {
        val t = Timer()
        assertEquals(Phase.IDLE, t.phase(t0))
        assertEquals(25 * min, t.remaining(t0))
        assertNull(t.alarmAt(t0))
    }

    @Test fun startRunsAndCountsDown() {
        val t = Timer(10 * min).start(t0)
        assertEquals(Phase.RUNNING, t.phase(t0))
        assertEquals(t0 + 10 * min, t.endAt)
        assertEquals(10 * min - 1234, t.remaining(t0 + 1234))
        assertEquals(t0 + 10 * min, t.alarmAt(t0 + 5))
    }

    @Test fun doneAtEndAndRemainingNeverNegative() {
        val t = Timer(min).start(t0)
        assertEquals(Phase.RUNNING, t.phase(t0 + min - 1))
        assertEquals(Phase.DONE, t.phase(t0 + min))
        assertEquals(0L, t.remaining(t0 + 5 * min))
        assertNull(t.alarmAt(t0 + min))
    }

    @Test fun pauseKeepsRemainingAndResumeContinues() {
        val p = Timer(10 * min).start(t0).pause(t0 + 3 * min)
        assertEquals(Phase.PAUSED, p.phase(t0 + 99 * min))
        assertEquals(7 * min, p.remaining(t0 + 99 * min))
        assertNull(p.alarmAt(t0))
        val r = p.start(t0 + 50 * min)
        assertEquals(t0 + 57 * min, r.endAt)
        assertEquals(10 * min, r.setMs)
    }

    @Test fun pauseOnlyWhileRunning() {
        val idle = Timer(5 * min)
        assertSame(idle, idle.pause(t0))
        val done = Timer(min).start(t0)
        assertSame(done, done.pause(t0 + 2 * min))
    }

    @Test fun startWhileRunningDoesNothing() {
        val r = Timer(5 * min).start(t0)
        assertSame(r, r.start(t0 + 1000))
    }

    @Test fun againRestartsWithSetTime() {
        val done = Timer(4 * min).start(t0)
        val again = done.start(t0 + 10 * min)
        assertEquals(Phase.RUNNING, again.phase(t0 + 10 * min))
        assertEquals(t0 + 14 * min, again.endAt)
    }

    @Test fun zeroCannotStart() {
        val z = Timer(0)
        assertFalse(z.canStart(t0))
        assertSame(z, z.start(t0))
        assertTrue(Timer(0, Mode.PAUSED, 0, 1000).canStart(t0))
    }

    @Test fun resetGoesIdleKeepingSetTime() {
        val r = Timer(8 * min).start(t0).pause(t0 + min).reset()
        assertEquals(Timer(8 * min), r)
    }

    @Test fun setWhileIdle() {
        assertEquals(Timer(40 * min), Timer().set(40 * min, t0))
    }

    @Test fun setWhileRunningKeepsRunningWithNewRemaining() {
        val r = Timer(10 * min).start(t0).set(30 * min, t0 + min)
        assertEquals(Phase.RUNNING, r.phase(t0 + min))
        assertEquals(30 * min, r.remaining(t0 + min))
        assertEquals(30 * min, r.setMs)
    }

    @Test fun setWhilePausedStaysPaused() {
        val p = Timer(10 * min).start(t0).pause(t0 + min).set(3 * min, t0 + 2 * min)
        assertEquals(Phase.PAUSED, p.phase(t0))
        assertEquals(3 * min, p.remaining(t0))
    }

    @Test fun setAfterDoneIsIdle() {
        val s = Timer(min).start(t0).set(12 * min, t0 + 5 * min)
        assertEquals(Timer(12 * min), s)
    }

    @Test fun setZeroAlwaysIdle() {
        assertEquals(Timer(0), Timer(10 * min).start(t0).set(0, t0 + 1))
        assertEquals(Timer(0), Timer(10 * min).start(t0).pause(t0 + 1).set(0, t0 + 2))
    }

    @Test fun setClampsToMax() {
        assertEquals(Timer.MAX_MS, Timer().set(999 * min, t0).setMs)
        assertEquals(0L, Timer().set(-5, t0).setMs)
    }

    @Test fun limitToScaleOnlyTouchesSetTime() {
        assertEquals(5 * min, Timer(25 * min).limitTo(Scale.MIN5).setMs)
        assertEquals(25 * min, Timer(25 * min).limitTo(Scale.MIN60).setMs)
        val running = Timer(25 * min).start(t0).limitTo(Scale.MIN5)
        assertEquals(t0 + 25 * min, running.endAt)
    }

    @Test fun sanitizedKeepsValidStates() {
        val run = Timer(10 * min).start(t0)
        assertEquals(run, run.sanitized(t0))
        val p = run.pause(t0 + min)
        assertEquals(p, p.sanitized(t0))
        // laengst abgelaufen bleibt "Zeit um"
        assertEquals(run, run.sanitized(t0 + 1000 * min))
    }

    @Test fun sanitizedRepairsBrokenData() {
        assertEquals(Timer(Timer.MAX_MS), Timer(Long.MAX_VALUE).sanitized(t0))
        assertEquals(Timer(0), Timer(-7).sanitized(t0))
        assertEquals(Timer(5 * min), Timer(5 * min, Mode.PAUSED, 0, -3).sanitized(t0))
        assertEquals(Timer(5 * min), Timer(5 * min, Mode.PAUSED, 0, Timer.MAX_MS + 1).sanitized(t0))
        // Lauf ohne brauchbares Ende: nicht still verwerfen, sondern "Zeit um"
        assertEquals(Phase.DONE, Timer(5 * min, Mode.RUNNING, 0, 0).sanitized(t0).phase(t0))
        // Ende unmoeglich weit weg: Lauf bleibt, begrenzt auf das Maximum
        assertEquals(Timer(5 * min, Mode.RUNNING, t0 + Timer.MAX_MS, 0), Timer(5 * min, Mode.RUNNING, t0 + 500 * min, 0).sanitized(t0))
        // Idle mit Restdaten wird bereinigt
        assertEquals(Timer(5 * min), Timer(5 * min, Mode.IDLE, 77, 88).sanitized(t0))
    }

    private val wall = 1_700_000_000_000L

    // Uhrzeit verstellt (Zeitzone, Sommerzeit, Netzzeit): Lauf haengt nur an der Laufzeituhr
    @Test fun wallClockJumpChangesNothing() {
        val run = Timer(10 * min).start(t0)
        val endWall = wall + 10 * min
        assertEquals(run, run.restored(t0 + min, wall + min + 60 * min, endWall, sameBoot = true))
        assertEquals(run, run.restored(t0 + min, wall + min - 60 * min, endWall, sameBoot = true))
        assertEquals(Phase.RUNNING, run.restored(t0 + min, wall + 30 * min, endWall, sameBoot = true).phase(t0 + min))
    }

    // Nach einem Neustart beginnt die Laufzeituhr bei 0: Ende ueber die Wanduhr umrechnen
    @Test fun afterRebootWallClockRebasesEnd() {
        val run = Timer(10 * min).start(t0)
        val endWall = wall + 10 * min
        val boot = 5_000L
        val r = run.restored(boot, wall + 4 * min, endWall, sameBoot = false)
        assertEquals(Phase.RUNNING, r.phase(boot))
        assertEquals(6 * min, r.remaining(boot))
        // Ende waehrend des Neustarts verpasst: Zeit um
        val late = run.restored(boot, wall + 30 * min, endWall, sameBoot = false)
        assertEquals(Phase.DONE, late.phase(boot))
        assertEquals(Mode.RUNNING, late.mode)
        // Neustart nicht erkannt, aber Ende unmoeglich weit weg: ebenfalls umrechnen
        assertEquals(6 * min, run.restored(boot, wall + 4 * min, endWall, sameBoot = true).remaining(boot))
        // Pause braucht keine Uhr
        val p = run.pause(t0 + min)
        assertEquals(p, p.restored(boot, wall, 0, sameBoot = false))
    }
}
