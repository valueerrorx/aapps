package solutions.xapient.dozr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeTest {
    private val g = ShakeDetector.EARTH

    // Proben mit 50 Hz; Stoesse = kurze Spitzen von 3 g
    private fun feed(d: ShakeDetector, peaksAt: List<Long>, untilMs: Long, peakG: Float = 3f): Int {
        var fired = 0
        var t = 0L
        while (t <= untilMs) {
            val peak = peaksAt.any { t in it..(it + 40) }
            if (d.onSample(0f, if (peak) peakG * g else g, 0f, t)) fired++
            t += 20
        }
        return fired
    }

    @Test fun restingPhoneNeverFires() {
        assertEquals(0, feed(ShakeDetector(), emptyList(), 10_000))
    }

    @Test fun threeKnocksInOneSecondFire() {
        assertEquals(1, feed(ShakeDetector(), listOf(0, 300, 600), 1_000))
    }

    @Test fun twoKnocksAreNotEnough() {
        assertEquals(0, feed(ShakeDetector(), listOf(0, 300), 2_000))
    }

    // Umdrehen im Bett: einzelne Stoesse weit auseinander
    @Test fun slowKnocksDoNotFire() {
        assertEquals(0, feed(ShakeDetector(), listOf(0, 800, 1_600, 2_400), 4_000))
    }

    @Test fun weakMovementDoesNotFire() {
        assertEquals(0, feed(ShakeDetector(), listOf(0, 200, 400, 600), 1_000, peakG = 1.8f))
    }

    // Dauerhaft hoher Wert ist ein Stoss, keine Serie
    @Test fun sustainedPushCountsOnce() {
        val d = ShakeDetector()
        var fired = false
        for (t in 0L..600L step 20) fired = fired or d.onSample(0f, 3f * g, 0f, t)
        assertFalse(fired)
    }

    // Nach dem Ausloesen 3 s Ruhe, dann wieder bereit
    @Test fun cooldownBetweenShakes() {
        val d = ShakeDetector()
        assertEquals(1, feed(d, listOf(0, 200, 400, 1_000, 1_200, 1_400), 2_000))
        val d2 = ShakeDetector()
        assertEquals(2, feed(d2, listOf(0, 200, 400, 3_500, 3_700, 3_900), 4_500))
    }

    @Test fun anyAxisCounts() {
        val d = ShakeDetector()
        var fired = false
        for (t in listOf(0L, 300L, 600L)) fired = fired or d.onSample(3f * g, 0f, 0f, t)
        assertTrue(fired)
    }
}
