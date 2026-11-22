package solutions.xapient.loudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class SoundMeterTest {
    private val rate = 48_000
    private val meter = SoundMeter().apply { restart(rate) }
    private var phase = 0L

    // 1-kHz-Sinus mit RMS in 16-bit-Einheiten, in 20-ms-Bloecken
    private fun tone(rms: Double, seconds: Double, m: SoundMeter = meter, fs: Int = rate) {
        val block = ShortArray(fs / 50)
        var left = (seconds * fs).roundToInt()
        while (left > 0) {
            val n = minOf(left, block.size)
            for (i in 0 until n) {
                block[i] = (rms * sqrt(2.0) * sin(2 * PI * 1000.0 * phase++ / fs)).roundToInt().toShort()
            }
            m.feed(block, n)
            left -= n
        }
    }

    private fun rmsFor(db: Double) = 2500.0 * Math.pow(10.0, (db - 90.0) / 20)

    @Test fun referencePointIs90dB() {
        tone(2500.0, 2.0)
        val r = meter.reading()
        assertEquals(90.0, r.fast.toDouble(), 0.1)
        assertEquals(90.0, r.slow.toDouble(), 0.1)
        assertEquals(90.0, r.leq.toDouble(), 0.1)
    }

    @Test fun fullScaleSine() {
        tone(32767 / sqrt(2.0), 1.0)
        assertEquals(SoundMeter.REF_DB - 10 * log10(2.0), meter.reading().fast.toDouble(), 0.1)
    }

    @Test fun works44k() {
        val m = SoundMeter().apply { restart(44_100) }
        tone(rmsFor(70.0), 1.5, m, 44_100)
        assertEquals(70.0, m.reading().fast.toDouble(), 0.1)
    }

    @Test fun settlesBeforeCounting() {
        tone(2500.0, 0.28)
        assertTrue(meter.reading().fast.isNaN())
        assertFalse(meter.reading().live)
        tone(2500.0, 0.52)
        val r = meter.reading()
        assertTrue(r.live)
        assertEquals(0.5, r.seconds.toDouble(), 0.001)
        assertEquals(90.0, r.fast.toDouble(), 0.3)
    }

    // Zeitkonstanten: nach tau 63 % der Energie-Stufe; Abfall 34,7 bzw. 4,3 dB/s
    @Test fun fastAndSlowTimeConstants() {
        tone(rmsFor(50.0), 3.0)
        tone(rmsFor(90.0), 0.125)
        var r = meter.reading()
        assertEquals(90 + 10 * log10(1 - Math.exp(-1.0)), r.fast.toDouble(), 0.3)
        assertEquals(90 + 10 * log10(1 - Math.exp(-0.125)), r.slow.toDouble(), 0.4)
        tone(rmsFor(90.0), 5.0)
        tone(rmsFor(50.0), 0.5)
        r = meter.reading()
        assertEquals(90 - 34.7 * 0.5, r.fast.toDouble(), 0.5)
        assertEquals(90 - 4.34 * 0.5, r.slow.toDouble(), 0.3)
    }

    @Test fun leqIsEnergyMean() {
        tone(rmsFor(90.0), 1.3)
        tone(rmsFor(70.0), 1.0)
        val r = meter.reading()
        assertEquals(90 + 10 * log10((1 + 0.01) / 2), r.leq.toDouble(), 0.1)
        assertEquals(2.0, r.seconds.toDouble(), 0.001)
    }

    @Test fun minAndMax() {
        tone(rmsFor(90.0), 1.3)
        tone(rmsFor(70.0), 1.0)
        val r = meter.reading()
        assertEquals(90.0, r.maxFast.toDouble(), 0.2)
        assertEquals(70.0, r.minFast.toDouble(), 0.3)
        assertEquals(90.0, r.maxSlow.toDouble(), 0.3)
        assertEquals(90 + 10 * log10(0.01 + 0.99 * Math.exp(-1.0)), r.minSlow.toDouble(), 0.3)
        assertEquals(r.maxFast, r.max(TimeWeighting.FAST))
        assertEquals(r.minSlow, r.min(TimeWeighting.SLOW))
    }

    @Test fun resetWhileRunningKeepsLevel() {
        tone(2500.0, 1.0)
        meter.reset()
        var r = meter.reading()
        assertTrue(r.leq.isNaN())
        assertTrue(r.maxFast.isNaN())
        assertEquals(0, r.history.size)
        assertEquals(90.0, r.fast.toDouble(), 0.2)
        // Fast braucht von 90 auf 60 dB knapp eine Sekunde
        tone(rmsFor(60.0), 2.0)
        r = meter.reading()
        assertEquals(60.0, r.minFast.toDouble(), 0.3)
        assertTrue(r.maxFast < 90f)
    }

    @Test fun pauseIgnoresInputAndResetClearsLevel() {
        tone(2500.0, 1.0)
        meter.pause()
        val before = meter.reading()
        assertFalse(before.live)
        assertEquals(90.0, before.fast.toDouble(), 0.2)
        tone(rmsFor(40.0), 1.0)
        assertEquals(before.seconds, meter.reading().seconds)
        meter.reset()
        assertTrue(meter.reading().fast.isNaN())
    }

    @Test fun restartKeepsStatistics() {
        tone(2500.0, 1.3)
        meter.pause()
        meter.restart(rate)
        tone(2500.0, 1.3)
        val r = meter.reading()
        assertEquals(2.0, r.seconds.toDouble(), 0.001)
        assertEquals(90.0, r.leq.toDouble(), 0.1)
    }

    @Test fun historyHoldsTenthSecondsForOneMinute() {
        tone(rmsFor(80.0), 2.3)
        var h = meter.reading().history
        assertEquals(20, h.size)
        assertEquals(80.0, h.last().toDouble(), 0.2)
        tone(rmsFor(60.0), 70.0)
        h = meter.reading().history
        assertEquals(SoundMeter.HISTORY_SLICES, h.size)
        assertEquals(60.0, h.last().toDouble(), 0.2)
    }

    @Test fun saveAndRestore() {
        tone(rmsFor(90.0), 1.3)
        tone(rmsFor(70.0), 1.0)
        val a = meter.reading()
        val copy = SoundMeter().apply { restore(meter.save()) }
        val b = copy.reading()
        assertEquals(a.leq, b.leq, 0.001f)
        assertEquals(a.maxFast, b.maxFast, 0.001f)
        assertEquals(a.minSlow, b.minSlow, 0.001f)
        assertEquals(a.fast, b.fast, 0.001f)
        assertEquals(a.seconds, b.seconds, 0.001f)
        assertEquals(a.history.toList(), b.history.toList())
        assertFalse(b.live)
        // ohne Messung danach: Pegel leer, Statistik bleibt
        val empty = SoundMeter().apply { restore(SoundMeter().save()) }.reading()
        assertTrue(empty.fast.isNaN() && empty.leq.isNaN())
    }

    @Test fun silenceStaysFinite() {
        meter.feed(ShortArray(rate), rate)
        val r = meter.reading()
        assertTrue(r.fast.isFinite() && r.fast < 0f)
    }

    @Test fun historyRing() {
        val h = History(3)
        listOf(1f, 2f, 3f, 4f, 5f).forEach(h::push)
        assertEquals(listOf(3f, 4f, 5f), h.toArray().toList())
        h.clear()
        assertEquals(0, h.toArray().size)
    }
}
