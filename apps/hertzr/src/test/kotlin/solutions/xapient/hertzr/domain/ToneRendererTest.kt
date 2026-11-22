package solutions.xapient.hertzr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

class ToneRendererTest {
    private val rate = Signal.RATE
    private val fade = rate * ToneRenderer.FADE_MS / 1_000

    private fun sine(hz: Double, amp: Float = 1f, out: Output = Output.BOTH) = ToneRenderer(rate, Params(hz, Wave.SINE, amp, out))

    // gemessene Frequenz = eingestellte, ueber den ganzen Bereich
    @Test fun frequencyAccuracy() {
        for (hz in listOf(1.0, 20.0, 55.0, 440.0, 1_000.0, 12_345.0, 20_000.0, 21_600.0)) {
            val secs = if (hz < 10) 5 else 1
            val x = Signal.left(Signal.render(sine(hz), rate * secs))
            val measured = Signal.zeroCrossHz(x, from = fade)
            assertEquals("$hz Hz", hz, measured, hz * 1e-5)
        }
    }

    // 10 Minuten durchgehend: Phase stimmt noch mit dem idealen Sinus
    @Test fun noPhaseDriftOverTenMinutes() {
        val hz = 997.3
        val r = sine(hz)
        val block = 240
        val buf = FloatArray(block * 2)
        val frames = rate.toLong() * 600
        var n = 0L
        while (n < frames) {
            r.render(buf, block)
            n += block
        }
        r.render(buf, block)
        var worst = 0.0
        for (i in 0 until block) {
            val ideal = sin(2 * PI * hz * (n + i) / rate)
            worst = maxOf(worst, abs(buf[2 * i] - ideal))
        }
        assertTrue("Abweichung $worst", worst < 1e-4)
    }

    // Frequenzsprung 100 Hz -> 5 kHz: kein Signalsprung, nur die Steilheit des neuen Tons
    @Test fun frequencyChangeIsPhaseContinuous() {
        val r = sine(100.0)
        val a = Signal.render(r, 4_800)
        r.apply(Params(5_000.0, Wave.SINE, 1f, Output.BOTH))
        val b = Signal.render(r, 4_800)
        val x = Signal.left(a + b)
        val bound = 2 * PI * 5_000 / rate
        assertTrue("Sprung ${Signal.maxStep(x)} > $bound", Signal.maxStep(x) <= bound * 1.01)
        // nach dem Gleiten genau auf dem Ziel
        assertEquals(5_000.0, Signal.zeroCrossHz(Signal.left(b), from = 4_000), 1.0)
    }

    // Wellenwechsel im Lauf: Ueberblendung, kein groesserer Sprung als die Formen selbst haben
    @Test fun waveChangeCrossfades() {
        val r = ToneRenderer(rate, Params(200.0, Wave.SINE, 1f, Output.BOTH))
        val a = Signal.render(r, 4_800)
        r.apply(Params(200.0, Wave.TRIANGLE, 1f, Output.BOTH))
        val b = Signal.render(r, 4_800)
        val steady = maxOf(Signal.maxStep(Signal.left(a).copyOfRange(fade, 4_800)), Signal.maxStep(Signal.left(b).copyOfRange(2_400, 4_800)))
        val across = Signal.maxStep(Signal.left(a + b).copyOfRange(fade, 9_600))
        assertTrue("Wechsel $across > $steady", across <= steady * 1.05)
    }

    @Test fun outputChangeGlides() {
        val r = sine(300.0)
        val a = Signal.render(r, 4_800)
        r.apply(Params(300.0, Wave.SINE, 1f, Output.LEFT))
        val b = Signal.render(r, 4_800)
        val right = Signal.right(a + b)
        val steady = 2 * PI * 300 / rate
        assertTrue(Signal.maxStep(right) <= steady * 1.01)
        // nach gut 80 ms rechts still
        assertTrue(Signal.right(b).drop(4_000).all { abs(it) < 1e-3 })
    }

    @Test fun channels() {
        val left = Signal.render(sine(500.0, out = Output.LEFT), 4_800)
        assertTrue(Signal.right(left).all { it == 0.0 })
        assertTrue(Signal.left(left).any { abs(it) > 0.5 })
        val inv = Signal.render(sine(500.0, out = Output.INVERT), 4_800)
        val l = Signal.left(inv)
        val rr = Signal.right(inv)
        for (i in l.indices) assertEquals(-l[i], rr[i], 1e-6)
    }

    // Pegel: Sinus mit Spitze 10^(dB/20), RMS 3 dB darunter
    @Test fun levelInDb() {
        for (db in listOf(0, -6, -24, -60)) {
            val x = Signal.left(Signal.render(sine(1_000.0, Level.amp(db)), rate))
            assertEquals("$db dB", db + Signal.sqrtHalfDb(), Signal.rmsDb(x, rate / 2, rate), 0.05)
        }
    }

    // Start aus der Stille, voll nach der Einblendzeit; Stopp blendet aus und meldet fertig
    @Test fun fadesInAndOut() {
        val r = sine(1_000.0)
        val x = Signal.left(Signal.render(r, fade * 2))
        assertEquals(0.0, x[0], 1e-9)
        assertTrue(x.take(fade / 4).all { abs(it) < 0.3 })
        assertTrue(x.drop(fade).any { abs(it) > 0.99 })
        r.release()
        val y = Signal.left(Signal.render(r, fade + 240))
        assertTrue(r.done)
        assertTrue(y.drop(fade).all { it == 0.0 })
        val joined = x + y
        assertTrue(Signal.maxStep(joined) <= 2 * PI * 1_000 / rate * 1.01)
    }

    // Sweep: gemessene Frequenz folgt der Exponentialkurve, am Ende Ausblenden und fertig
    @Test fun sweepFollowsLogCurve() {
        val spec = SweepSpec(20.0, 20_000.0, 5)
        val r = ToneRenderer(rate, Params(440.0, Wave.SINE, 1f, Output.BOTH, spec))
        val x = Signal.left(Signal.render(r, rate * 5))
        for (t in listOf(1.0, 2.0, 2.5, 3.0, 4.0)) {
            val c = (t * rate).toInt()
            val w = rate / 20
            val measured = Signal.zeroCrossHz(x, c - w, c + w)
            val expected = 20.0 * 1_000.0.pow(t / 5)
            assertEquals("t=$t", expected, measured, expected * 0.02)
        }
        assertEquals(1f, assertNotNullF(r.progress), 1e-3f)
        assertFalse(r.done)
        Signal.render(r, fade + 480)
        assertTrue(r.done)
    }

    @Test fun sweepDownwards() {
        val r = ToneRenderer(rate, Params(440.0, Wave.SINE, 1f, Output.BOTH, SweepSpec(8_000.0, 80.0, 5)))
        val x = Signal.left(Signal.render(r, rate * 5))
        val mid = Signal.zeroCrossHz(x, rate * 5 / 2 - 2_400, rate * 5 / 2 + 2_400)
        assertEquals(800.0, mid, 16.0)
        assertEquals(80.0, r.currentHz, 0.01)
    }

    private fun assertNotNullF(v: Float?): Float {
        assertNotNull(v)
        return v!!
    }

    private operator fun FloatArray.plus(o: FloatArray): FloatArray = FloatArray(size + o.size) { if (it < size) this[it] else o[it - size] }
    private operator fun DoubleArray.plus(o: DoubleArray): DoubleArray = DoubleArray(size + o.size) { if (it < size) this[it] else o[it - size] }
}
