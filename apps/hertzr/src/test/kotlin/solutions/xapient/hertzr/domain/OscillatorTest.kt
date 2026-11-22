package solutions.xapient.hertzr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Aliasing per DFT gemessen: staerkste Nicht-Harmonische relativ zum Grundton.
// Grenzwerte aus echten Laeufen mit ~3 dB Reserve; "band" = nur bis 16 kHz (Hoerbereich).
class OscillatorTest {
    private val rate = Signal.RATE
    private val band = 16_000.0
    private val freqs = listOf(1_234.5, 3_100.0, 4_700.0)

    private fun tone(w: Wave, hz: Double, frames: Int = 48_000): DoubleArray =
        Signal.left(Signal.render(ToneRenderer(rate, Params(hz, w, 1f, Output.BOTH)), frames))

    private fun naive(w: Wave, hz: Double, frames: Int = 48_000): DoubleArray {
        var t = 0.0
        return DoubleArray(frames) {
            val v = Oscillator.naive(w, t)
            t += hz / rate
            if (t >= 1) t -= 1
            v
        }
    }

    // gibt (geglaettet voll, naiv voll, geglaettet Band, naiv Band) aus und prueft
    private fun check(w: Wave, fullLimit: Double, bandLimit: Double, gainDb: Double) {
        for (hz in freqs) {
            val bl = tone(w, hz)
            val nv = naive(w, hz)
            val blFull = Signal.worstAliasDb(bl, hz)
            val nvFull = Signal.worstAliasDb(nv, hz)
            val blBand = Signal.worstAliasDb(bl, hz, band)
            val nvBand = Signal.worstAliasDb(nv, hz, band)
            println("alias $w $hz: voll %.1f (naiv %.1f), bis 16 kHz %.1f (naiv %.1f) dB".format(blFull, nvFull, blBand, nvBand))
            assertTrue("$w $hz voll: $blFull", blFull < fullLimit)
            assertTrue("$w $hz Band: $blBand", blBand < bandLimit)
            assertTrue("$w $hz Band: $blBand vs naiv $nvBand", blBand < nvBand - gainDb)
        }
    }

    @Test fun sawAliasing() = check(Wave.SAW, SAW_FULL, SAW_BAND, 10.0)

    @Test fun squareAliasing() = check(Wave.SQUARE, SQUARE_FULL, SQUARE_BAND, 10.0)

    @Test fun triangleAliasing() = check(Wave.TRIANGLE, TRIANGLE_FULL, TRIANGLE_BAND, 6.0)

    // ab Rate/6 reiner Sinus: kein Aliasing mehr
    @Test fun highFrequenciesBecomeSine() {
        for (w in Wave.entries) {
            val hz = 9_000.0
            val db = Signal.worstAliasDb(tone(w, hz), hz)
            println("alias $w $hz: %.1f dB".format(db))
            assertTrue("$w $hz: $db", db < -85)
        }
        assertEquals(0.0, Oscillator.sineBlend(0.05), 0.0)
        assertEquals(1.0, Oscillator.sineBlend(1.0 / 6), 1e-12)
    }

    // Formen in Phase mit dem Sinus, Spitze hoechstens 1
    @Test fun shapesAlignedAndBounded() {
        for (w in Wave.entries) {
            if (w != Wave.SQUARE) assertEquals("$w bei 0", 0.0, Oscillator.naive(w, 0.0), 1e-9)
            assertTrue("$w positiv bei 1/4", Oscillator.naive(w, 0.25) > 0)
            assertTrue("$w negativ bei 3/4", Oscillator.naive(w, 0.75) < 0)
            for (hz in listOf(100.0, 2_000.0, 6_000.0)) {
                var t = 0.0
                val dt = hz / rate
                repeat(10_000) {
                    val v = Oscillator.sample(w, t, dt)
                    assertTrue("$w $hz Spitze $v", kotlin.math.abs(v) <= 1.0 + 1e-9)
                    t = (t + dt) % 1.0
                }
            }
        }
    }

    // Glaettung: Rest eines Sprungs springt am Sprung genau um 1, Knick-Rest ist stetig
    @Test fun blepContinuous() {
        assertEquals(0.5, Oscillator.blep(-1e-9), 1e-6)
        assertEquals(-0.5, Oscillator.blep(1e-9), 1e-6)
        assertEquals(0.0, Oscillator.blep(-1.0), 0.0)
        assertEquals(Oscillator.blamp(-1e-9), Oscillator.blamp(1e-9), 1e-6)
    }

    private companion object {
        // gemessen 22.11. (schlechtester der drei Toene): Saege voll -26,0 / Band -32,6,
        // Rechteck -30,3 / -32,6, Dreieck -49,4 / -49,5; naiv je 10-16 dB schlechter
        const val SAW_FULL = -23.0
        const val SAW_BAND = -29.5
        const val SQUARE_FULL = -27.0
        const val SQUARE_BAND = -29.5
        const val TRIANGLE_FULL = -46.0
        const val TRIANGLE_BAND = -46.0
    }
}
