package solutions.xapient.loudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

class AWeightingTest {
    private val rates = listOf(44_100, 48_000)

    // Terzmitten nach IEC 61672-1 mit Klasse-1-Toleranz (plus, minus) in dB
    private val class1 = listOf(
        10.0 to (3.5 to 99.0), 20.0 to (2.5 to 2.5), 31.5 to (2.0 to 2.0), 63.0 to (1.5 to 1.5),
        125.0 to (1.5 to 1.5), 250.0 to (1.4 to 1.4), 500.0 to (1.4 to 1.4), 1000.0 to (1.1 to 1.1),
        2000.0 to (1.6 to 1.6), 4000.0 to (1.6 to 1.6), 8000.0 to (2.1 to 3.1), 10000.0 to (2.6 to 3.6),
        12500.0 to (3.0 to 6.0), 16000.0 to (3.5 to 17.0),
    )

    @Test fun referenceCurveKnownValues() {
        assertEquals(0.0, AWeighting.reference(1000.0), 0.01)
        assertEquals(-19.1, AWeighting.reference(100.0), 0.05)
        assertEquals(-50.5, AWeighting.reference(20.0), 0.15)
        assertEquals(1.2, AWeighting.reference(2500.0), 0.1)
        assertEquals(-2.5, AWeighting.reference(10000.0), 0.1)
    }

    @Test fun zeroDbAtOneKilohertz() = rates.forEach {
        assertEquals(0.0, AWeighting(it).gainDb(1000.0), 1e-9)
    }

    // bis 4 kHz praktisch exakt, darueber faellt die bilineare Transformation etwas ab
    @Test fun matchesReferenceUpTo4k() = rates.forEach { fs ->
        val a = AWeighting(fs)
        var f = 10.0
        while (f <= 4000.0) {
            val err = a.gainDb(f) - AWeighting.reference(f)
            assertTrue("$fs Hz, $f Hz: %.3f dB".format(err), kotlin.math.abs(err) <= 0.1)
            f *= 1.12
        }
    }

    @Test fun withinClass1Tolerance() = rates.forEach { fs ->
        val a = AWeighting(fs)
        class1.forEach { (f, tol) ->
            val err = a.gainDb(f) - AWeighting.reference(f)
            assertTrue("$fs Hz, $f Hz: %.2f dB".format(err), err <= tol.first && err >= -tol.second)
        }
    }

    // Zeitbereich: Sinus durch das Filter, Pegel am Ausgang
    private fun sineGainDb(fs: Int, f: Double): Double {
        val a = AWeighting(fs)
        var sIn = 0.0
        var sOut = 0.0
        val n = fs * 2
        for (i in 0 until n) {
            val x = sin(2 * PI * f * i / fs)
            val y = a.process(x)
            if (i >= fs) {
                sIn += x * x
                sOut += y * y
            }
        }
        return 10 * log10(sOut / sIn)
    }

    @Test fun sineThroughFilter() = rates.forEach { fs ->
        for (f in listOf(31.5, 100.0, 1000.0, 3150.0, 8000.0)) {
            assertEquals("$fs/$f", AWeighting(fs).gainDb(f), sineGainDb(fs, f), 0.05)
        }
        assertEquals(-19.1, sineGainDb(fs, 100.0), 0.1)
        assertEquals(0.0, sineGainDb(fs, 1000.0), 0.02)
    }

    @Test fun resetClearsState() {
        val a = AWeighting(48_000)
        repeat(1000) { a.process(1.0) }
        a.reset()
        assertEquals(0.0, a.process(0.0), 0.0)
        assertEquals(0.0, a.process(0.0), 0.0)
    }
}
