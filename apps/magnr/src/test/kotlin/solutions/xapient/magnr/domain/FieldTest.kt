package solutions.xapient.magnr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class FieldTest {
    private val ms = 1_000_000L

    @Test fun strengthBelowNoiseIsZero() {
        assertEquals(0f, Sensitivity.MEDIUM.strength(0.8f), 0f)
        assertEquals(0f, Sensitivity.MEDIUM.strength(-0.9f), 0f)
        assertEquals(0f, Sensitivity.MEDIUM.strength(Float.NaN), 0f)
    }

    @Test fun strengthIsSymmetricAndClamped() {
        val s = Sensitivity.MEDIUM
        assertEquals(s.strength(10f), s.strength(-10f), 0f)
        assertEquals((10f - 0.9f) / (40f - 0.9f), s.strength(10f), 1e-6f)
        assertEquals(1f, s.strength(500f), 0f)
    }

    @Test fun higherSensitivityGivesMoreStrength() {
        assertTrue(Sensitivity.HIGH.strength(5f) > Sensitivity.MEDIUM.strength(5f))
        assertTrue(Sensitivity.MEDIUM.strength(5f) > Sensitivity.LOW.strength(5f))
        assertEquals(1f, Sensitivity.HIGH.strength(12f), 0f)
        assertEquals(1f, Sensitivity.MEDIUM.strength(40f), 0f)
        assertEquals(1f, Sensitivity.LOW.strength(100f), 0f)
    }

    @Test fun levelsRiseImmediately() {
        assertEquals(Level.QUIET, Level.of(0.11f))
        assertEquals(Level.WEAK, Level.of(0.12f))
        assertEquals(Level.NEAR, Level.of(0.45f))
        assertEquals(Level.STRONG, Level.of(0.8f))
        assertEquals(Level.STRONG, Level.of(0.9f, Level.QUIET))
    }

    @Test fun levelsFallWithHysteresis() {
        assertEquals(Level.STRONG, Level.of(0.77f, Level.STRONG))
        assertEquals(Level.NEAR, Level.of(0.75f, Level.STRONG))
        assertEquals(Level.NEAR, Level.of(0.42f, Level.STRONG))
        assertEquals(Level.WEAK, Level.of(0.40f, Level.STRONG))
        assertEquals(Level.WEAK, Level.of(0.09f, Level.WEAK))
        assertEquals(Level.QUIET, Level.of(0.07f, Level.WEAK))
    }

    @Test fun smootherFollowsTimeConstant() {
        val s = Smoother(0.12f)
        assertEquals(40f, s.step(40f, 0f), 0f)
        // nach einer Zeitkonstante 63 % des Sprungs, egal in wie vielen Schritten
        val a = Smoother(0.12f).apply { step(0f, 0f); step(10f, 0.12f) }.value
        val b = Smoother(0.12f).apply { step(0f, 0f); repeat(6) { step(10f, 0.02f) } }.value
        assertEquals(10f * (1f - exp(-1f)), a, 1e-4f)
        assertEquals(a, b, 1e-4f)
    }

    @Test fun baselineSettlesToMeanAfterHalfSecond() {
        val b = Baseline()
        var t = 1_000 * ms
        for (i in 0..26) {
            b.update(if (i % 2 == 0) 47f else 49f, 0.02f, t, drift = true)
            if (i < 25) assertFalse("zu frueh bei $i", b.settled)
            t += 20 * ms
        }
        assertTrue(b.settled)
        assertEquals(48f, b.value, 0.1f)
    }

    @Test fun driftOnlyInsideQuietBand() {
        val b = Baseline().apply { set(48f) }
        // Fund mit +10 µT: Grundwert bleibt
        repeat(3000) { b.update(58f, 0.02f, it * 20 * ms, drift = true) }
        assertEquals(48f, b.value, 0f)
        // kleine Drift +1 µT: wird nach 60 s weitgehend ausgeglichen
        repeat(3000) { b.update(49f, 0.02f, it * 20 * ms, drift = true) }
        assertEquals(48f + 1f - exp(-2f), b.value, 0.02f)
    }

    @Test fun driftOffKeepsBaseline() {
        val b = Baseline().apply { set(48f) }
        repeat(3000) { b.update(49f, 0.02f, it * 20 * ms, drift = false) }
        assertEquals(48f, b.value, 0f)
    }

    @Test fun peakHoldsTwoSecondsThenFollows() {
        val p = PeakHold()
        p.update(5f, 0)
        p.update(-12f, 100 * ms)
        assertEquals(-12f, p.value, 0f)
        p.update(3f, 1_500 * ms)
        assertEquals(-12f, p.value, 0f)
        p.update(3f, 2_200 * ms)
        assertEquals(3f, p.value, 0f)
        p.update(1f, 2_300 * ms)
        assertEquals(1f, p.value, 0f)
    }

    @Test fun accuracyLowOnlyAfterGrace() {
        val a = AccuracyWatch()
        assertFalse(a.update(3, 0))
        assertFalse(a.update(1, 100 * ms))
        assertFalse(a.update(0, 900 * ms))
        assertTrue(a.update(1, 1_100 * ms))
        // gut sofort
        assertFalse(a.update(2, 1_200 * ms))
    }
}
