package solutions.xapient.nordr.ui

import solutions.xapient.nordr.domain.Accuracy
import solutions.xapient.nordr.domain.CompassReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Hinweis-Logik ohne Android: ausblenden, Vorrang, Wiederkehr
class CompassStateTest {
    private fun CompassState.accuracy(a: Accuracy) = show(CompassReading(valid = true, heading = 10f, accuracy = a))

    @Test
    fun lowAccuracyShowsCalibrationUntilDismissed() {
        val s = CompassState()
        s.accuracy(Accuracy.GOOD)
        assertNull(s.hint)
        s.accuracy(Accuracy.LOW)
        assertEquals(Hint.CALIBRATE, s.hint)
        s.dismissHint()
        assertNull(s.hint)
        // bleibt weg, solange es nicht besser wird
        s.accuracy(Accuracy.LOW)
        assertNull(s.hint)
    }

    @Test
    fun comesBackAfterImprovementThenWorsening() {
        val s = CompassState()
        s.accuracy(Accuracy.LOW)
        s.dismissHint()
        s.accuracy(Accuracy.GOOD)
        assertNull(s.hint)
        s.accuracy(Accuracy.LOW)
        assertEquals(Hint.CALIBRATE, s.hint)
    }

    @Test
    fun interferenceIsWorseThanDismissedLow() {
        val s = CompassState()
        s.accuracy(Accuracy.LOW)
        s.dismissHint()
        s.accuracy(Accuracy.INTERFERENCE)
        assertEquals(Hint.INTERFERENCE, s.hint)
    }

    @Test
    fun dismissedInterferenceStaysHiddenOnSmallImprovement() {
        val s = CompassState()
        s.accuracy(Accuracy.INTERFERENCE)
        s.dismissHint()
        s.accuracy(Accuracy.LOW)
        assertNull(s.hint)
        // Verbesserung auf LOW, dann wieder Stoerung = Verschlechterung
        s.accuracy(Accuracy.INTERFERENCE)
        assertEquals(Hint.INTERFERENCE, s.hint)
    }

    @Test
    fun interferenceHasPriorityOverManualHint() {
        val s = CompassState()
        s.accuracy(Accuracy.GOOD)
        s.showHint()
        assertEquals(Hint.CALIBRATE, s.hint)
        assertTrue(s.hintIsManual)
        s.accuracy(Accuracy.INTERFERENCE)
        assertEquals(Hint.INTERFERENCE, s.hint)
        assertFalse(s.hintIsManual)
    }

    @Test
    fun manualHintClosesWithDone() {
        val s = CompassState()
        s.accuracy(Accuracy.LOW)
        s.dismissHint()
        s.showHint()
        assertEquals(Hint.CALIBRATE, s.hint)
        s.dismissHint()
        assertNull(s.hint)
    }
}
