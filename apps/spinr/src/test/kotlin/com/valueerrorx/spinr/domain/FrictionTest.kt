package com.valueerrorx.spinr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionTest {
    private val f = Friction()

    @Test
    fun distanceAndStopTimeGrowWithSpeed() {
        var lastD = 0.0
        var lastT = 0.0
        for (w in listOf(10.0, 100.0, 500.0, 1000.0, 3000.0)) {
            val d = f.distance(w)
            val t = f.stopTime(w)
            assertTrue(d > lastD && t > lastT)
            lastD = d; lastT = t
        }
        assertEquals(0.0, f.distance(0.0), 0.0)
        assertEquals(0.0, f.distance(-5.0), 0.0)
    }

    @Test
    fun typicalSpinFeelsRight() {
        // fuenf Umdrehungen dauern gut fuenf Sekunden
        val w = f.velocityFor(5 * 360.0)
        assertTrue("w=$w", w in 1000.0..1400.0)
        assertTrue("t=${f.stopTime(w)}", f.stopTime(w) in 4.0..6.0)
    }

    @Test
    fun velocityForInvertsDistance() {
        for (d in listOf(1.0, 90.0, 900.0, 1800.0, 3600.0, 20_000.0)) {
            assertEquals(d, f.distance(f.velocityFor(d)), 1e-6)
        }
        assertEquals(0.0, f.velocityFor(0.0), 0.0)
    }

    @Test
    fun closedFormMatchesNumericIntegration() {
        val w0 = 1200.0
        var w = w0
        var s = 0.0
        val h = 1e-5
        var t = 0.0
        while (w > 0) {
            w -= (f.a + f.k * w) * h
            s += w.coerceAtLeast(0.0) * h
            t += h
        }
        assertEquals(f.distance(w0), s, 0.05)
        assertEquals(f.stopTime(w0), t, 1e-3)
        assertEquals(f.distance(w0), f.travelled(w0, 99.0), 1e-9)
    }

    @Test
    fun velocityFallsToZeroAtStop() {
        val w0 = 800.0
        val stop = f.stopTime(w0)
        assertEquals(w0, f.velocity(w0, 0.0), 1e-9)
        assertTrue(f.velocity(w0, stop * 0.5) < w0)
        assertEquals(0.0, f.velocity(w0, stop), 1e-6)
        assertEquals(0.0, f.velocity(w0, stop + 1), 0.0)
    }
}
