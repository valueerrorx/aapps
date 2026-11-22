package solutions.xapient.tunr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin

class PitchDetectorTest {
    private val d48 = PitchDetector(48_000)
    private val d44 = PitchDetector(44_100)

    private fun cents(a: Float, b: Double) = 1200 * log2(a / b)

    private fun midiHz(m: Int) = 440.0 * 2.0.pow((m - 69) / 12.0)

    private fun assertPitch(det: PitchDetector, x: FloatArray, hz: Double, tol: Double, what: String) {
        val p = det.detect(x)
        assertNotNull("$what: kein Ton", p)
        val c = cents(p!!.hz, hz)
        assertTrue("$what: %.3f Cent daneben (%.2f Hz)".format(c, p.hz), abs(c) < tol)
    }

    @Test fun fftMatchesDft() {
        val n = 64
        val fft = Fft(n)
        val x = Signals.noise(n, 1.0)
        val re = DoubleArray(n) { x[it].toDouble() }
        val im = DoubleArray(n)
        fft.transform(re, im)
        for (k in 0 until n) {
            var r = 0.0
            var i = 0.0
            for (j in 0 until n) {
                r += x[j] * cos(-2 * PI * k * j / n)
                i += x[j] * sin(-2 * PI * k * j / n)
            }
            assertEquals(r, re[k], 1e-9)
            assertEquals(i, im[k], 1e-9)
        }
        // vorwaerts + rueckwaerts = n * x
        fft.transform(re, im, inverse = true)
        for (j in 0 until n) assertEquals(x[j].toDouble(), re[j] / n, 1e-9)
    }

    // Sinus E1 (MIDI 28) bis C8 (MIDI 108): unter 1 Cent, bei beiden Abtastraten
    @Test fun sineAcrossRangeBelowOneCent() {
        for (det in listOf(d48, d44)) {
            for (m in 28..108 step 4) {
                val hz = midiHz(m)
                assertPitch(det, Signals.sine(hz, det.sampleRate, det.size), hz, 1.0, "Sinus MIDI $m @${det.sampleRate}")
            }
            assertPitch(det, Signals.sine(midiHz(108), det.sampleRate, det.size), midiHz(108), 1.0, "C8")
            assertPitch(det, Signals.sine(41.2034, det.sampleRate, det.size), 41.2034, 1.0, "E1")
        }
    }

    // Verstimmung wird auf unter 1 Cent genau gemessen
    @Test fun detunedSineCents() {
        for (offset in listOf(-37.0, -12.5, -3.0, 0.4, 4.9, 21.0, 49.0)) {
            val hz = 196.0 * 2.0.pow(offset / 1200)
            val p = d48.detect(Signals.sine(hz, 48_000, d48.size))!!
            assertEquals("Versatz $offset", offset, cents(p.hz, 196.0), 1.0)
        }
    }

    @Test fun sawtoothWithOvertones() {
        for (m in listOf(28, 40, 45, 57, 69, 76, 84)) {
            val hz = midiHz(m)
            assertPitch(d48, Signals.saw(hz, 48_000, d48.size), hz, 3.0, "Saegezahn MIDI $m")
        }
    }

    // Grundton schwach, ungerade Obertoene schwach: YIN allein laege eine Oktave zu hoch
    @Test fun weakFundamentalKeepsOctave() {
        val weak = listOf(0.1, 1.0, 0.15, 0.5, 0.1, 0.3)
        for (hz in listOf(41.2034, 55.0, 82.407, 110.0, 196.0)) {
            assertPitch(d48, Signals.harmonics(hz, 48_000, d48.size, weak), hz, 3.0, "schwacher Grundton $hz")
        }
        val typical = listOf(0.25, 1.0, 0.6, 0.4, 0.3)
        for (hz in listOf(41.2034, 98.0, 220.0, 440.0)) {
            assertPitch(d44, Signals.harmonics(hz, 44_100, d44.size, typical), hz, 3.0, "Obertoene $hz")
        }
    }

    // reiner Sinus und Obertonreihe: nie eine Oktave zu tief
    @Test fun noSubOctave() {
        for (m in 28..96 step 3) {
            val hz = midiHz(m)
            val p = d48.detect(Signals.harmonics(hz, 48_000, d48.size, listOf(1.0, 0.8, 0.6, 0.4)))!!
            assertTrue("MIDI $m: ${p.hz}", abs(cents(p.hz, hz)) < 50)
        }
    }

    @Test fun noiseAndSilenceGiveNothing() {
        assertNull(d48.detect(Signals.noise(d48.size, 0.3)))
        assertNull(d48.detect(Signals.noise(d48.size, 0.8, seed = 3)))
        assertNull(d48.detect(FloatArray(d48.size)))
    }

    @Test fun sineInNoise() {
        // Signal-Rausch-Abstand ~ 15 dB
        val x = Signals.add(Signals.sine(110.0, 48_000, d48.size, 0.4), Signals.noise(d48.size, 0.1))
        assertPitch(d48, x, 110.0, 3.0, "Sinus mit Rauschen")
        val y = Signals.add(Signals.saw(329.63, 48_000, d48.size), Signals.noise(d48.size, 0.05))
        assertPitch(d48, y, 329.63, 3.0, "Saegezahn mit Rauschen")
    }

    @Test fun levelInDbfs() {
        // Sinus mit Spitze 1,0 = -3,01 dBFS
        assertEquals(-3.01f, PitchDetector.rmsDb(Signals.sine(440.0, 48_000, 4800, 1.0)), 0.05f)
        assertEquals(-120f, PitchDetector.rmsDb(FloatArray(100)), 0f)
        assertEquals(-23.01f, PitchDetector.rmsDb(Signals.sine(440.0, 48_000, 4800, 0.1)), 0.05f)
    }
}
