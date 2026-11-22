package com.valueerrorx.spinr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class SpinTest {
    @Test
    fun trajectoryStartsAndEndsWherePlanned() {
        val spin = Spin(30.0, 1, 1000.0)
        assertEquals(30.0, spin.angleAt(0.0), 1e-9)
        assertEquals(spin.end, spin.angleAt(spin.duration), 0.0)
        assertEquals(spin.end, spin.angleAt(spin.duration + 5), 0.0)
        assertEquals(spin.end, spin.angleAt(spin.duration - 1e-6), 1e-3)
        assertEquals(30.0 + spin.distance, spin.end, 0.0)
    }

    @Test
    fun angleIsMonotonicAndSlowsDown() {
        val spin = Spin(0.0, -1, 1500.0)
        var last = spin.angleAt(0.0)
        var lastStep = Double.MAX_VALUE
        var t = 0.0
        while (!spin.isDone(t)) {
            t += 1.0 / 60
            val a = spin.angleAt(t)
            val step = last - a
            assertTrue(step >= 0)
            assertTrue(step <= lastStep + 1e-9)
            last = a; lastStep = step
        }
        assertTrue(spin.velocityAt(0.0) < 0)
        assertEquals(0.0, spin.velocityAt(spin.duration), 1e-9)
    }

    @Test
    fun frameByFrameEndsOnPlannedSegment() {
        // so wie die UI: Zeit pro Frame aufsummieren, am Ende steht der Zeiger auf dem geplanten Segment
        val random = Random(3)
        repeat(200) {
            val spin = Spinner.tap(random.nextDouble(0.0, 360.0), random)
            var t = 0.0
            var angle = spin.start
            while (!spin.isDone(t)) {
                t += 1.0 / 60 + random.nextDouble(0.0, 0.01)
                angle = spin.angleAt(t)
            }
            assertEquals(WheelMath.segmentAt(spin.end, 9), WheelMath.segmentAt(angle, 9))
        }
    }

    @Test
    fun tapSpinsFourToFiveTurnsClockwise() {
        val random = Random(1)
        repeat(500) {
            val spin = Spinner.tap(0.0, random)
            assertEquals(1, spin.direction)
            assertTrue(spin.distance in 4 * 360.0..6 * 360.0)
        }
    }

    @Test
    fun weakFlingIsNoSpin() {
        assertNull(Spinner.fling(0.0, 40.0, Random(1)))
        assertNull(Spinner.fling(0.0, -149.0, Random(1)))
        assertNull(Spinner.fling(0.0, Double.NaN, Random(1)))
        assertNotNull(Spinner.fling(0.0, 151.0, Random(1)))
    }

    @Test
    fun flingKeepsDirectionAndStaysNearNaturalDistance() {
        val random = Random(2)
        val f = Friction()
        for (v in listOf(-2500.0, -900.0, 400.0, 1200.0, 1800.0, 2600.0, 9000.0)) {
            val natural = f.distance(abs(v)).coerceIn(Spinner.MIN_DISTANCE, Spinner.MAX_DISTANCE)
            repeat(50) {
                val spin = Spinner.fling(10.0, v, random)!!
                assertEquals(if (v > 0) 1 else -1, spin.direction)
                assertTrue(abs(spin.distance - natural) <= 180.0 + 1e-6)
            }
        }
    }

    @Test
    fun tapIsFair() = assertFair { random -> Spinner.tap(random.nextDouble(0.0, 360.0), random) }

    @Test
    fun flingIsFairEvenWithIdenticalGestures() =
        // gleiche Startlage und gleicher Schwung: das Ergebnis ist trotzdem gleichverteilt
        assertFair { random -> Spinner.fling(123.0, 1300.0, random)!! }

    @Test
    fun counterClockwiseFlingIsFair() = assertFair { random -> Spinner.fling(0.0, -700.0 - random.nextDouble(0.0, 2000.0), random)!! }

    // Chi-Quadrat ueber die Segmente unter dem Zeiger am Ende der Drehung
    private fun assertFair(plan: (Random) -> Spin) {
        for ((n, critical) in listOf(2 to 10.83, 7 to 22.46, 37 to 67.99)) {
            val random = Random(42 + n)
            val spins = 2_000 * n
            val counts = IntArray(n)
            repeat(spins) {
                val spin = plan(random)
                counts[WheelMath.segmentAt(spin.angleAt(spin.duration), n)]++
            }
            val expected = spins.toDouble() / n
            val chi2 = counts.sumOf { (it - expected) * (it - expected) / expected }
            // kritischer Wert fuer p = 0,001
            assertTrue("n=$n chi2=$chi2 counts=${counts.toList()}", chi2 < critical)
        }
    }
}
