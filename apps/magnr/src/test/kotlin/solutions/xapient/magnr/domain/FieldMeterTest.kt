package solutions.xapient.magnr.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FieldMeterTest {
    private val ms = 1_000_000L
    private var t = 10_000 * ms

    // Feld mit Betrag m, schraeg im Raum (Lage egal, nur der Betrag zaehlt)
    private fun FieldMeter.feed(m: Float, seconds: Float, status: Int = 3, maxRange: Float = 2000f): Reading {
        val a = m / sqrt(3f)
        var r = reading
        repeat((seconds * 50).toInt()) {
            r = onSample(a, -a, a, maxRange, status, t)
            t += 20 * ms
        }
        return r
    }

    @Test fun settlesThenShowsDeviation() {
        val f = FieldMeter()
        val first = f.feed(48f, 0.2f)
        assertTrue(first.settling)
        assertEquals(Level.QUIET, first.level)
        f.feed(48f, 0.6f)
        assertEquals(48f, f.reading.baseline, 0.01f)
        val near = f.feed(68f, 1f)
        assertEquals(68f, near.total, 0.05f)
        assertEquals(20f, near.dev, 0.05f)
        assertEquals(Level.NEAR, near.level)
        assertEquals(20f, near.peak, 0.05f)
    }

    @Test fun orientationDoesNotMatter() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        // gleiches Feld, andere Lage
        repeat(50) { f.onSample(48f, 0f, 0f, 2000f, 3, t); t += 20 * ms }
        repeat(50) { f.onSample(0f, 0f, -48f, 2000f, 3, t); t += 20 * ms }
        assertEquals(0f, f.reading.dev, 0.01f)
        assertEquals(Level.QUIET, f.reading.level)
    }

    @Test fun negativeDeviationCountsToo() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        val r = f.feed(36f, 1f)
        assertEquals(-12f, r.dev, 0.05f)
        assertEquals(Sensitivity.MEDIUM.strength(12f), r.strength, 0.01f)
    }

    @Test fun tareZeroesCurrentField() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        f.feed(60f, 1f)
        f.tare()
        assertEquals(0f, f.reading.dev, 0f)
        assertEquals(Level.QUIET, f.reading.level)
        assertTrue(f.reading.peak.isNaN())
        val r = f.feed(60f, 0.5f)
        assertEquals(60f, r.baseline, 0.05f)
        assertEquals(0f, r.dev, 0.05f)
    }

    @Test fun tareWhilePausedSettlesAgain() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        f.pause(100f)
        f.tare()
        assertTrue(f.reading.baseline.isNaN())
        t += 5_000 * ms
        assertTrue(f.feed(55f, 0.2f).settling)
        assertEquals(55f, f.feed(55f, 0.5f).baseline, 0.01f)
    }

    @Test fun roseOnlyWhenLevelClimbs() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        var rises = 0
        repeat(50) { f.onSample(0f, 0f, 90f, 2000f, 3, t); t += 20 * ms; if (f.rose) rises++ }
        // ruhig -> schwach -> nah -> sehr nah, jede Stufe einmal
        assertEquals(3, rises)
        assertEquals(Level.STRONG, f.reading.level)
        repeat(50) { f.onSample(0f, 0f, 90f, 2000f, 3, t); t += 20 * ms; assertFalse(f.rose) }
    }

    @Test fun sensitivityChangesScale() {
        val f = FieldMeter(Sensitivity.LOW)
        f.feed(48f, 1f)
        assertEquals(Level.QUIET, f.feed(58f, 1f).level)
        f.changeSensitivity(Sensitivity.HIGH)
        // Stufe gleich aus der aktuellen Abweichung, kein falsches "gestiegen" beim naechsten Wert
        assertEquals(Level.STRONG, f.reading.level)
        f.onSample(0f, 0f, 58f, 2000f, 3, t)
        assertFalse(f.rose)
        assertEquals(Level.STRONG, f.reading.level)
    }

    @Test fun driftOnlyWhileQuiet() {
        // niedrig: +4 µT ist Ruhe und wird langsam Grundwert
        val low = FieldMeter(Sensitivity.LOW)
        low.feed(48f, 1f)
        low.feed(52f, 60f)
        assertTrue(low.reading.baseline > 51f)
        // hoch: +4 µT ist ein Fund, der Grundwert bleibt
        val high = FieldMeter(Sensitivity.HIGH)
        high.feed(48f, 1f)
        high.feed(52f, 60f)
        assertEquals(48f, high.reading.baseline, 0.01f)
    }

    @Test fun saturationFromAnyAxis() {
        val f = FieldMeter()
        f.feed(48f, 1f, maxRange = 1000f)
        assertFalse(f.reading.saturated)
        f.onSample(990f, 0f, 0f, 1000f, 3, t)
        assertTrue(f.reading.saturated)
        // Reichweite unbekannt: nie am Anschlag
        f.onSample(990f, 0f, 0f, 0f, 3, t + 20 * ms)
        assertFalse(f.reading.saturated)
    }

    @Test fun historyHasOneSlicePer100ms() {
        val f = FieldMeter()
        f.feed(48f, 0.5f)
        f.feed(48f, 2f)
        val n = f.reading.history.size
        assertTrue("$n", n in 19..21)
        f.feed(90f, 0.5f)
        assertEquals(1f, f.reading.history.last(), 0f)
    }

    @Test fun historyKeepsTwentySeconds() {
        val f = FieldMeter()
        f.feed(48f, 25f)
        assertEquals(History.SLICES, f.reading.history.size)
    }

    @Test fun pauseLeavesGapAndKeepsBaseline() {
        val f = FieldMeter()
        f.feed(48f, 2f)
        f.pause(100f)
        assertEquals(48f, f.reading.baseline, 0.01f)
        assertTrue(f.reading.total.isNaN())
        t += 5_000 * ms
        f.resume(105f)
        val r = f.feed(48f, 1f)
        // kurze Pause: kein neues Einschwingen, Grundwert sofort da
        assertFalse(r.settling)
        assertTrue(r.history.any { it.isNaN() })
    }

    @Test fun longPauseSettlesAgain() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        f.pause(100f)
        // zweites pause (Dispose nach Stop) verschiebt den Beginn nicht
        f.pause(150f)
        f.resume(161f)
        assertTrue(f.reading.baseline.isNaN())
        assertTrue(f.feed(55f, 0.2f).settling)
        assertEquals(55f, f.feed(55f, 0.5f).baseline, 0.01f)
    }

    @Test fun rebootCountsAsLongPause() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        f.pause(5_000f)
        f.resume(30f)
        assertTrue(f.reading.baseline.isNaN())
    }

    @Test fun lowAccuracyReported() {
        val f = FieldMeter()
        f.feed(48f, 0.5f, status = 3)
        assertFalse(f.feed(48f, 0.5f, status = 1).accuracyLow)
        assertTrue(f.feed(48f, 0.6f, status = 1).accuracyLow)
    }

    @Test fun saveAndRestore() {
        val f = FieldMeter()
        f.feed(48f, 1f)
        f.feed(60f, 3f)
        val history = f.reading.history
        // wie in der App: erst ON_STOP, dann Sichern
        f.pause(100f)
        val saved = f.save()
        val g = FieldMeter().apply { restore(saved) }
        assertEquals(48f, g.reading.baseline, 0.01f)
        assertArrayEquals(history, g.reading.history, 0f)
        // bald zurueck: weiter ohne Einschwingen, mit Luecke
        g.resume(130f)
        val r = g.feed(60f, 0.5f)
        assertEquals(12f, r.dev, 0.1f)
        assertTrue(r.history.any { it.isNaN() })
        // lange weg (Prozess-Tod ueber Nacht): neu einschwingen
        val h = FieldMeter().apply { restore(saved) }
        h.resume(4_000f)
        assertTrue(h.feed(60f, 0.2f).settling)
    }

    @Test fun restoreWithoutBaselineSettlesAgain() {
        val g = FieldMeter().apply { restore(floatArrayOf(Float.NaN, Float.NaN)) }
        assertTrue(g.feed(48f, 0.2f).settling)
    }
}
