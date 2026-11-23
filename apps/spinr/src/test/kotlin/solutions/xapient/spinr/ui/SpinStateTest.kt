package solutions.xapient.spinr.ui

import solutions.xapient.spinr.domain.WheelMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

// Rad-Zustand wie in der UI: Frames bis zum Stillstand
@RunWith(RobolectricTestRunner::class)
class SpinStateTest {
    private fun labels(n: Int) = (1..n).map { "$it" }

    // laeuft Frames bis zum Ende; Rueckgabe: Stifte insgesamt
    private fun runOut(s: SpinState, random: Random): Int {
        var ticks = 0
        var frames = 0
        while (s.spinning) {
            ticks += s.step(1.0 / 60 + random.nextDouble(0.0, 0.008))
            check(++frames < 10_000)
        }
        return ticks
    }

    @Test
    fun spinsAreFairAndWinnerIsUnderThePointer() {
        val n = 7
        val random = Random(11)
        val s = SpinState(labels(n), random = Random(5))
        val spins = 3_000
        val counts = IntArray(n)
        val ends = DoubleArray(spins)
        repeat(spins) { k ->
            // abwechselnd Tippen und Schwung in beide Richtungen
            val started = if (k % 2 == 0) s.tap() else s.fling((if (random.nextBoolean()) 1 else -1) * random.nextDouble(600.0, 4000.0).toFloat())
            assertTrue(started)
            runOut(s, random)
            val w = s.winner!!
            assertEquals(WheelMath.segmentAt(s.angle.toDouble(), n), w)
            counts[w]++
            ends[k] = WheelMath.normalize(s.angle.toDouble()) / 360.0
        }
        val expected = spins.toDouble() / n
        val chi2 = counts.sumOf { (it - expected) * (it - expected) / expected }
        assertTrue("chi2=$chi2 ${counts.toList()}", chi2 < 22.46) // p = 0,001, 6 Freiheitsgrade
        // Kolmogorow-Smirnow: Endwinkel gleichverteilt
        ends.sort()
        var d = 0.0
        ends.forEachIndexed { i, x -> d = max(d, max(abs((i + 1.0) / spins - x), abs(x - i.toDouble() / spins))) }
        assertTrue("ks=$d", d < 1.95 / sqrt(spins.toDouble())) // p = 0,001
    }

    @Test
    fun ticksMatchBoundariesCrossed() {
        val n = 8
        val s = SpinState(labels(n), random = Random(3))
        s.pose(0.5f)
        s.tap()
        val start = 0.5
        val ticks = runOut(s, Random(1))
        val crossed = abs(WheelMath.tickIndex(s.angle.toDouble(), n) - WheelMath.tickIndex(start, n)).toInt()
        assertEquals(crossed, ticks)
        // von Hand: genau eine Grenze bei 45 Grad
        s.pose(40f)
        assertEquals(1, s.dragBy(10f))
        assertEquals(0, s.dragBy(10f))
    }

    @Test
    fun lockedWhileSpinning() {
        val s = SpinState(labels(5), random = Random(2))
        assertTrue(s.tap())
        assertFalse(s.canSpin)
        assertFalse(s.tap())
        assertFalse(s.fling(3000f))
        val a = s.angle
        assertEquals(0, s.dragBy(30f))
        assertEquals(a, s.angle)
        runOut(s, Random(2))
        assertTrue(s.canSpin)
    }

    @Test
    fun weakFlingKeepsTheWheelWhereItIs() {
        val s = SpinState(labels(5), random = Random(2))
        s.pose(12f)
        assertFalse(s.fling(200f))
        assertFalse(s.spinning)
        assertEquals(12f, s.angle)
    }

    @Test
    fun newLabelsAbortTheSpin() {
        val s = SpinState(labels(6), random = Random(4))
        s.tap()
        s.step(0.2)
        s.showLabels(labels(4))
        assertFalse(s.spinning)
        assertNull(s.winner)
        assertEquals(0f, s.speed)
        assertEquals(4, s.colors.size)
        assertTrue(s.canSpin)
    }

    @Test
    fun fewerThanTwoLabelsLetTheLoopSleep() {
        val s = SpinState(labels(4), random = Random(4))
        // Zunge schwingt noch nach
        s.pose(20f, flap = 0.8f)
        s.step(0.001)
        assertTrue(s.isMoving)
        s.showLabels(labels(1))
        s.step(1.0 / 60)
        assertFalse(s.isMoving)
        assertEquals(0f, s.flap)
        assertFalse(s.tap())
    }
}
