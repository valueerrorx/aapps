package solutions.xapient.hudr.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTest {
    private val trip = Trip()

    @Test fun startsEmpty() {
        assertEquals(0.0, trip.meters, 0.0)
        assertTrue(trip.avgMps.isNaN())
        assertEquals(0f, trip.maxMps, 0f)
    }

    // 60 s mit 20 m/s = 1200 m, Ø 20, Max 20
    @Test fun constantSpeed() {
        for (i in 0..60) trip.add(Track.fix(i.toDouble()), 20f, true)
        assertEquals(1200.0, trip.meters, 1e-6)
        assertEquals(60.0, trip.movingSeconds, 1e-9)
        assertEquals(20f, trip.avgMps, 1e-4f)
        assertEquals(20f, trip.maxMps, 0f)
    }

    // Trapez: linear von 0 auf 10 m/s in 10 s = 50 m
    @Test fun trapezoid() {
        for (i in 0..10) trip.add(Track.fix(i.toDouble()), i.toFloat(), i > 0)
        assertEquals(50.0, trip.meters, 1e-6)
        assertEquals(10f, trip.maxMps, 0f)
    }

    // Stillstand zaehlt weder Strecke noch Zeit; Ø nur ueber Fahrzeit
    @Test fun standingDoesNotCount() {
        for (i in 0..10) trip.add(Track.fix(i.toDouble()), 10f, true)
        for (i in 11..100) trip.add(Track.fix(i.toDouble()), 0f, false)
        assertEquals(100.0, trip.meters, 1e-6)
        assertEquals(10f, trip.avgMps, 1e-4f)
    }

    // Tunnel: 20 s Luecke, Luftlinie mit guter Genauigkeit
    @Test fun gapBridgedByStraightLine() {
        trip.add(Track.fix(0.0, 0.0), 20f, true)
        trip.add(Track.fix(20.0, 400.0), 20f, true)
        assertEquals(400.0, trip.meters, 1.0)
        assertEquals(20.0, trip.movingSeconds, 1e-9)
    }

    @Test fun gapWithPoorAccuracyIgnored() {
        trip.add(Track.fix(0.0, 0.0, acc = 50f), 20f, true)
        trip.add(Track.fix(20.0, 400.0, acc = 50f), 20f, true)
        assertEquals(0.0, trip.meters, 0.0)
    }

    // Sprung ueber 360 km/h ist kein Weg
    @Test fun implausibleJumpIgnored() {
        trip.add(Track.fix(0.0, 0.0), 20f, true)
        trip.add(Track.fix(10.0, 5000.0), 20f, true)
        assertEquals(0.0, trip.meters, 0.0)
    }

    @Test fun maxOnlyWhileMoving() {
        trip.add(Track.fix(0.0), 40f, false)
        trip.add(Track.fix(1.0), 12f, true)
        assertEquals(12f, trip.maxMps, 0f)
    }

    @Test fun resetAndRestore() {
        for (i in 0..10) trip.add(Track.fix(i.toDouble()), 15f, true)
        val s = trip.save()
        val other = Trip().apply { restore(s) }
        assertArrayEquals(s, other.save(), 0.0)
        assertEquals(15f, other.avgMps, 1e-4f)
        // nach Wiederherstellen kein Segment zum alten Fix
        other.add(Track.fix(100.0), 15f, true)
        assertEquals(150.0, other.meters, 1e-6)
        trip.reset()
        assertEquals(0.0, trip.meters, 0.0)
        assertEquals(0f, trip.maxMps, 0f)
    }

    @Test fun badStateIgnored() {
        Trip().apply {
            restore(doubleArrayOf(Double.NaN, 1.0, 2.0))
            assertEquals(0.0, meters, 0.0)
            restore(doubleArrayOf(1.0))
            assertEquals(0.0, meters, 0.0)
        }
    }
}
