package solutions.xapient.loudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoudnessTest {
    @Test fun zones() {
        assertEquals(Zone.QUIET, Zone.of(10f))
        assertEquals(Zone.QUIET, Zone.of(54.9f))
        assertEquals(Zone.MODERATE, Zone.of(55f))
        assertEquals(Zone.LOUD, Zone.of(70f))
        assertEquals(Zone.VERY_LOUD, Zone.of(85f))
        assertEquals(Zone.VERY_LOUD, Zone.of(140f))
    }

    @Test fun examples() {
        assertNull(Example.of(Float.NaN))
        assertEquals(Example.SILENCE, Example.of(-5f))
        assertEquals(Example.WHISPER, Example.of(30f))
        assertEquals(Example.CONVERSATION, Example.of(63f))
        assertEquals(Example.TRAFFIC, Example.of(80f))
        assertEquals(Example.SIREN, Example.of(130f))
        // aufsteigend, je etwa 10 dB
        Example.entries.zipWithNext().drop(1).forEach { (a, b) -> assertEquals(10f, b.from - a.from, 0f) }
    }

    @Test fun scale() {
        assertEquals(0f, Scale.fraction(0f), 0f)
        assertEquals(0.5f, Scale.fraction(70f), 1e-6f)
        assertEquals(1f, Scale.fraction(150f), 0f)
        assertEquals(0f, Scale.fraction(Float.NaN), 0f)
    }

    @Test fun calibrationSteps() {
        assertEquals(0.5f, Calibration.step(0f, +1), 0f)
        assertEquals(-0.5f, Calibration.step(0f, -1), 0f)
        assertEquals(20f, Calibration.step(20f, +1), 0f)
        assertEquals(-20f, Calibration.step(-20f, -1), 0f)
        // krumme Werte aus alten Daten rasten ein
        assertEquals(1.5f, Calibration.clamp(1.4f), 0f)
        assertEquals(0f, Calibration.clamp(Float.NaN), 0f)
        assertEquals(20f, Calibration.clamp(99f), 0f)
    }

    @Test fun calibrationApplies() {
        assertEquals(62.5f, Calibration.apply(60f, 2.5f), 0f)
        assertTrue(Calibration.apply(Float.NaN, 2f).isNaN())
    }

    @Test fun labels() {
        assertEquals("0", Calibration.label(0f))
        assertEquals("+2.5", Calibration.label(2.5f))
        assertEquals("−3", Calibration.label(-3f))
        assertEquals("−0,5", Calibration.label(-0.5f, ','))
        assertEquals("62", formatDb(62.4f))
        assertEquals("—", formatDb(Float.NaN))
        assertEquals("0", formatDb(-7f))
    }
}
