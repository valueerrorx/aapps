package solutions.xapient.hertzr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelSweepTest {
    @Test fun dbToAmplitude() {
        assertEquals(1f, Level.amp(0), 1e-6f)
        assertEquals(0.5012f, Level.amp(-6), 1e-4f)
        assertEquals(0.001f, Level.amp(-60), 1e-6f)
    }

    // nach Neustart nie lauter als -12 dB
    @Test fun restoredLevelIsCapped() {
        assertEquals(-12, Level.restored(0))
        assertEquals(-30, Level.restored(-30))
        assertEquals(-60, Level.restored(-99))
    }

    @Test fun warnings() {
        assertEquals(Warning.NONE, Level.warning(-24, false))
        assertEquals(Warning.HEADPHONES, Level.warning(-24, true))
        assertEquals(Warning.NONE, Level.warning(-30, true))
        assertEquals(Warning.NONE, Level.warning(-10, false))
        assertEquals(Warning.LOUD, Level.warning(-9, false))
        assertEquals(Warning.LOUD, Level.warning(-6, true))
    }

    @Test fun sliderPosition() {
        assertEquals(0f, Level.toPos(-60), 0f)
        assertEquals(1f, Level.toPos(0), 0f)
        assertEquals(-24, Level.fromPos(Level.toPos(-24)))
        assertEquals(0, Level.fromPos(2f))
    }

    @Test fun sweepCurve() {
        val s = SweepSpec(20.0, 20_000.0, 10)
        assertEquals(20.0, s.hzAt(0.0), 1e-9)
        assertEquals(632.46, s.hzAt(0.5), 0.01)
        assertEquals(20_000.0, s.hzAt(1.0), 1e-6)
        // gleiche Zeit je Oktave
        assertEquals(s.hzAt(0.3) / s.hzAt(0.2), s.hzAt(0.8) / s.hzAt(0.7), 1e-9)
    }

    @Test fun durations() {
        assertEquals(10, Sweep.nearestDuration(12))
        assertEquals(60, Sweep.nearestDuration(500))
        assertEquals(20, Sweep.stepDuration(10, 1))
        assertEquals(5, Sweep.stepDuration(5, -1))
        assertEquals(60, Sweep.stepDuration(60, 1))
    }

    @Test fun settingsSane() {
        val s = Settings(hz = 99_999.0, levelDb = 7, sweepFrom = 0.0, sweepSec = 3).sane(Freq.maxFor(44_100))
        assertEquals(21_609.0, s.hz, 0.0)
        assertEquals(0, s.levelDb)
        assertEquals(1.0, s.sweepFrom, 0.0)
        assertEquals(5, s.sweepSec)
    }

    @Test fun paramsFromSettings() {
        val tone = Settings(hz = 1_000.0, levelDb = -20).params()
        assertEquals(null, tone.sweep)
        assertEquals(0.1f, tone.amp, 1e-6f)
        val sw = Settings(mode = Mode.SWEEP, sweepFrom = 50.0, sweepTo = 5_000.0, sweepSec = 20).params()
        assertEquals(SweepSpec(50.0, 5_000.0, 20), sw.sweep)
    }
}
