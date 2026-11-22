package solutions.xapient.magnr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.magnr.domain.FieldMeter
import solutions.xapient.magnr.domain.Level

class DetectorStateTest {
    private val ms = 1_000_000L
    private var t = 5_000 * ms
    private val state = DetectorState(FieldMeter())

    private fun feed(z: Float, seconds: Float, status: Int = 3) {
        repeat((seconds * 50).toInt()) {
            state.listener.onField(0f, 0f, z, status, t)
            t += 20 * ms
        }
    }

    @Test fun digitsHoldQuarterSecond() {
        feed(48f, 1f)
        val before = state.shown
        // ein einzelner neuer Wert aendert die Ziffern noch nicht
        state.listener.onField(0f, 0f, 49f, 3, t)
        assertEquals(before.total, state.shown.total, 0f)
        t += 260 * ms
        state.listener.onField(0f, 0f, 49f, 3, t)
        assertTrue(state.shown.total > before.total)
    }

    @Test fun riseShowsImmediatelyAndNotifies() {
        val rises = mutableListOf<Level>()
        state.onRise = { rises += it }
        feed(48f, 1f)
        feed(90f, 0.2f)
        assertEquals(listOf(Level.WEAK, Level.NEAR, Level.STRONG), rises)
        assertEquals(Level.STRONG, state.shown.level)
    }

    @Test fun strengthFeedsBeeperAndStopsOnPause() {
        var s = -1f
        state.onStrength = { s = it }
        feed(48f, 1f)
        feed(90f, 0.5f)
        assertEquals(1f, s, 0f)
        state.pause(100_000L)
        assertEquals(0f, s, 0f)
    }

    @Test fun tareResetsDeviation() {
        feed(48f, 1f)
        feed(60f, 1f)
        val tick = state.tick
        state.tare()
        assertEquals(0f, state.shown.dev, 0f)
        assertTrue(state.tick > tick)
    }

    @Test fun calibrationCardFollowsAccuracy() {
        feed(48f, 1f)
        assertNull(state.hint)
        feed(48f, 1.2f, status = 1)
        assertEquals(Hint.CALIBRATE, state.hint)
        state.dismissHint()
        assertNull(state.hint)
        // bleibt weg, solange es schlecht bleibt
        feed(48f, 1f, status = 1)
        assertNull(state.hint)
        // besser, dann wieder schlecht: Karte kommt wieder
        feed(48f, 0.1f, status = 3)
        feed(48f, 1.2f, status = 0)
        assertEquals(Hint.CALIBRATE, state.hint)
    }

    @Test fun introBeforeCalibrationAndManualHelp() {
        state.introVisible = true
        feed(48f, 1.2f, status = 1)
        assertEquals(Hint.INTRO, state.hint)
        state.dismissHint()
        assertEquals(Hint.CALIBRATE, state.hint)
        state.dismissHint()
        assertNull(state.hint)
        state.showCalibration()
        assertEquals(Hint.CALIBRATE, state.hint)
        state.dismissHint()
        assertNull(state.hint)
    }
}
