package solutions.xapient.sleepr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class DspTest {

    @Test fun lowpass_passes_low_and_cuts_high() {
        val lp = Biquad(FSpec(FType.LOWPASS, 1000.0))
        assertEquals(1.0, lp.magSq(50.0), 0.01)
        assertEquals(0.5, lp.magSq(1000.0), 0.02) // -3 dB an der Grenzfrequenz
        assertTrue(lp.magSq(10000.0) < 0.001)
    }

    @Test fun peaking_boosts_by_gain() {
        val pk = Biquad(FSpec(FType.PEAKING, 500.0, 0.8, 10.0))
        assertEquals(10.0, 10 * kotlin.math.log10(pk.magSq(500.0)), 0.05)
    }

    @Test fun white_noise_stays_in_range() {
        val ch = NoiseChannel(42L)
        val v = DoubleArray(5)
        repeat(FS) { ch.next(v); assertTrue(abs(v[0]) <= 1.0) }
    }

    @Test fun k_brings_all_colors_to_same_rms() {
        val ch = NoiseChannel(7L)
        val v = DoubleArray(5)
        val sum = DoubleArray(5)
        repeat(FS) { ch.next(v) } // einschwingen
        val n = FS * 2
        repeat(n) { ch.next(v); for (i in 0..4) sum[i] += (v[i] * NoiseChannel.K[i]).let { it * it } }
        for (i in 0..4) assertEquals("Farbe $i", 0.12, sqrt(sum[i] / n), 0.02)
    }

    @Test fun same_seed_same_noise() {
        val a = NoiseChannel(99L); val b = NoiseChannel(99L)
        val va = DoubleArray(5); val vb = DoubleArray(5)
        repeat(1000) { a.next(va); b.next(vb); assertTrue(va.contentEquals(vb)) }
    }
}
