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

    // Trapez: linear von 0 auf 10 m/s in 10 s = 50 m; Max = was 2 s anlag
    @Test fun trapezoid() {
        for (i in 0..10) trip.add(Track.fix(i.toDouble()), i.toFloat(), i > 0)
        assertEquals(50.0, trip.meters, 1e-6)
        assertEquals(8f, trip.maxMps, 0f)
    }

    // Stillstand zaehlt nicht; das letzte Stueck bis 0 schon (Trapez nach unten)
    @Test fun standingDoesNotCount() {
        for (i in 0..10) trip.add(Track.fix(i.toDouble()), 10f, true)
        for (i in 11..100) trip.add(Track.fix(i.toDouble()), 0f, false)
        assertEquals(105.0, trip.meters, 1e-6)
        assertEquals(11.0, trip.movingSeconds, 1e-9)
    }

    // Bremsen bis zum Halt: 0,5 x 10 m/s x 10 s
    @Test fun brakingToStopCounted() {
        for (i in 0..10) trip.add(Track.fix(i.toDouble()), (10 - i).toFloat(), i < 10)
        assertEquals(50.0, trip.meters, 1e-6)
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
        for (i in 1..3) trip.add(Track.fix(i.toDouble()), 12f, true)
        assertEquals(12f, trip.maxMps, 0f)
    }

    // Spitze unter 2 s erreicht Max nicht
    @Test fun spikeDoesNotReachMax() {
        for (i in 0..5) trip.add(Track.fix(i.toDouble()), 20f, true)
        trip.add(Track.fix(6.0), 35f, true)
        for (i in 7..10) trip.add(Track.fix(i.toDouble()), 20f, true)
        assertEquals(20f, trip.maxMps, 0f)
        for (i in 11..13) trip.add(Track.fix(i.toDouble()), 30f, true)
        assertEquals(30f, trip.maxMps, 0f)
    }

    // ungenaue Geschwindigkeit zaehlt nicht fuer Max, Strecke schon
    @Test fun unreliableNotForMax() {
        for (i in 0..10) trip.add(Track.fix(i.toDouble()), 25f, true, reliable = false)
        assertEquals(0f, trip.maxMps, 0f)
        assertEquals(250.0, trip.meters, 1e-6)
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
