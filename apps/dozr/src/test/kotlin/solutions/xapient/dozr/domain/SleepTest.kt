package solutions.xapient.dozr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.dozr.domain.Sleep.Companion.MINUTE

class SleepTest {
    private val t0 = 1_000_000L
    private val fade = 60_000L

    @Test fun idleShowsSetTime() {
        val s = Sleep(30)
        assertEquals(30 * MINUTE, s.remaining(t0))
        assertEquals(Phase.IDLE, s.phase(t0, fade))
    }

    @Test fun runningCountsDownOnMonotonicClock() {
        val s = Sleep(30).start(t0)
        assertEquals(30 * MINUTE, s.remaining(t0))
        assertEquals(20 * MINUTE, s.remaining(t0 + 10 * MINUTE))
        assertEquals(0L, s.remaining(t0 + 40 * MINUTE))
    }

    @Test fun phasesAlongTheRun() {
        val s = Sleep(30).start(t0)
        assertEquals(Phase.RUNNING, s.phase(t0 + 29 * MINUTE - 1, fade))
        assertEquals(Phase.FADING, s.phase(t0 + 29 * MINUTE, fade))
        assertEquals(Phase.ENDED, s.phase(t0 + 30 * MINUTE, fade))
        assertEquals(Phase.DONE, s.finish().phase(t0 + 31 * MINUTE, fade))
    }

    @Test fun fadeProgressRunsZeroToOne() {
        val s = Sleep(30).start(t0)
        val f = t0 + 29 * MINUTE
        assertEquals(0.0, s.fadeProgress(f - 5_000, fade), 1e-9)
        assertEquals(0.0, s.fadeProgress(f, fade), 1e-9)
        assertEquals(0.5, s.fadeProgress(f + 30_000, fade), 1e-9)
        assertEquals(1.0, s.fadeProgress(f + 90_000, fade), 1e-9)
    }

    // Ausblenden laenger als der Lauf: beginnt beim Start
    @Test fun fadeNeverStartsBeforeStart() {
        val s = Sleep(5).start(t0)
        assertEquals(t0, s.fadeStart(10 * MINUTE))
        assertEquals(Phase.FADING, s.phase(t0, 10 * MINUTE))
    }

    @Test fun extendAddsTenMinutes() {
        val s = Sleep(30).start(t0).extend(t0 + 5 * MINUTE)
        assertEquals(35 * MINUTE, s.remaining(t0 + 5 * MINUTE))
        assertEquals(40 * MINUTE, s.totalMs)
    }

    // Verlaengern beim Ausblenden springt zurueck in den Lauf
    @Test fun extendWhileFadingLeavesFade() {
        val s = Sleep(30).start(t0)
        val now = t0 + 29 * MINUTE + 30_000
        assertEquals(Phase.FADING, s.phase(now, fade))
        val e = s.extend(now)
        assertEquals(Phase.RUNNING, e.phase(now, fade))
        assertEquals(10 * MINUTE + 30_000, e.remaining(now))
    }

    // Nach dem Ende verlaengern (Dienst war kurz nicht dran): ab jetzt
    @Test fun extendAfterEndCountsFromNow() {
        val s = Sleep(30).start(t0)
        val now = t0 + 31 * MINUTE
        assertEquals(10 * MINUTE, s.extend(now).remaining(now))
    }

    @Test fun extendIsCappedAtMaximum() {
        val s = Sleep(115).start(t0).extend(t0)
        assertEquals(120 * MINUTE, s.remaining(t0))
        assertEquals(120 * MINUTE, s.extend(t0).remaining(t0))
    }

    @Test fun extendOnlyWhileRunning() {
        assertEquals(Sleep(30), Sleep(30).extend(t0))
    }

    @Test fun setIsClampedAndIgnoredWhileRunning() {
        assertEquals(5, Sleep(30).set(1).setMin)
        assertEquals(120, Sleep(30).set(500).setMin)
        val r = Sleep(30).start(t0)
        assertEquals(r, r.set(60))
        val d = r.finish().set(45)
        assertEquals(Mode.IDLE, d.mode)
        assertEquals(45, d.setMin)
    }

    @Test fun stopKeepsLastValue() {
        val s = Sleep(45).start(t0).stop()
        assertEquals(Mode.IDLE, s.mode)
        assertEquals(45, s.setMin)
        assertEquals(45, s.start(t0).setMin)
    }

    @Test fun overdueAfterTwoMinutes() {
        val s = Sleep(30).start(t0)
        assertFalse(s.overdue(t0 + 32 * MINUTE))
        assertTrue(s.overdue(t0 + 32 * MINUTE + 1))
        assertFalse(s.finish().overdue(t0 + 99 * MINUTE))
    }

    // Prozess-Tod: gleicher Start, Lauf geht weiter
    @Test fun restoredSameBootKeepsRun() {
        val s = Sleep(30).start(t0)
        assertEquals(s, Sleep.restored(s, sameBoot = true, now = t0 + MINUTE))
    }

    // Geraete-Neustart: Laufzeituhr neu, Lauf ist vorbei, Wert bleibt
    @Test fun restoredAfterRebootIsIdle() {
        val s = Sleep(45).start(t0)
        val r = Sleep.restored(s, sameBoot = false, now = 5_000L)
        assertEquals(Mode.IDLE, r.mode)
        assertEquals(45, r.setMin)
        assertEquals(Mode.IDLE, Sleep.restored(s.finish(), sameBoot = false, now = 5_000L).mode)
    }

    @Test fun restoredDoneSameBootStaysDone() {
        val s = Sleep(30).start(t0).finish()
        assertEquals(s, Sleep.restored(s, true, t0 + 40 * MINUTE))
    }

    @Test fun restoredBrokenDataIsSane() {
        assertEquals(Mode.IDLE, Sleep.restored(Sleep(30, Mode.RUNNING, 10, 5), true, 20).mode)
        assertEquals(Mode.IDLE, Sleep.restored(Sleep(30, Mode.RUNNING, 500, 900), true, 20).mode)
        assertEquals(120, Sleep.restored(Sleep(9999), true, 0).setMin)
        assertEquals(5, Sleep.restored(Sleep(-3), true, 0).setMin)
    }

    @Test fun fadeLengthParsing() {
        assertEquals(FadeLength.M3, FadeLength.of("M3"))
        assertEquals(FadeLength.DEFAULT, FadeLength.of("kaputt"))
        // fruehere 30-s-Wahl faellt auf den Standard zurueck
        assertEquals(FadeLength.DEFAULT, FadeLength.of("S30"))
        assertEquals(FadeLength.DEFAULT, FadeLength.of(null))
    }
}
