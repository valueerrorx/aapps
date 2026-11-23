package solutions.xapient.spinr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelMathTest {
    @Test
    fun segmentUnderPointer() {
        // ungedreht steht Segment 0 rechts vom Zeiger, also direkt darunter
        assertEquals(0, WheelMath.segmentAt(0.0, 4))
        assertEquals(0, WheelMath.segmentAt(-89.9, 4))
        assertEquals(1, WheelMath.segmentAt(-90.0, 4))
        // im Uhrzeigersinn drehen bringt das letzte Segment unter den Zeiger
        assertEquals(3, WheelMath.segmentAt(10.0, 4))
        assertEquals(3, WheelMath.segmentAt(370.0, 4))
        assertEquals(2, WheelMath.segmentAt(-200.0 - 3600.0, 4))
        assertEquals(0, WheelMath.segmentAt(0.0, 1))
    }

    @Test
    fun angleForCentersSegment() {
        for (n in listOf(2, 3, 8, 50)) {
            for (i in 0 until n) assertEquals(i, WheelMath.segmentAt(WheelMath.angleFor(i, n), n))
        }
    }

    @Test
    fun oneTickPerBoundary() {
        val n = 8
        var last = WheelMath.tickIndex(1.0, n)
        var ticks = 0
        var a = 1.0
        while (a < 721.0) {
            a += 0.7
            val idx = WheelMath.tickIndex(a, n)
            ticks += kotlin.math.abs((idx - last).toInt())
            last = idx
        }
        assertEquals(16, ticks)
    }

    @Test
    fun pegOffsetIsNearestBoundary() {
        assertEquals(0.0, WheelMath.pegOffset(0.0, 4), 1e-9)
        assertEquals(10.0, WheelMath.pegOffset(10.0, 4), 1e-9)
        assertEquals(-10.0, WheelMath.pegOffset(80.0, 4), 1e-9)
        assertEquals(-45.0, WheelMath.pegOffset(45.0, 4), 1e-9)
        for (a in listOf(-1000.3, -5.0, 3.3, 777.7)) {
            val off = WheelMath.pegOffset(a, 7)
            assertTrue(off >= -360.0 / 14 && off < 360.0 / 14)
        }
    }

    @Test
    fun normalizeWraps() {
        assertEquals(350.0, WheelMath.normalize(-10.0), 1e-9)
        assertEquals(10.0, WheelMath.normalize(730.0), 1e-9)
    }
}
