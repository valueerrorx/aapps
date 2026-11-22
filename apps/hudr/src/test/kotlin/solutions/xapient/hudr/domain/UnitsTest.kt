package solutions.xapient.hudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitsTest {
    @Test fun conversions() {
        assertEquals(100f, SpeedUnit.KMH.fromMps(27.7778f), 0.01f)
        assertEquals(60f, SpeedUnit.MPH.fromMps(26.8224f), 0.01f)
        assertEquals(10f, SpeedUnit.KN.fromMps(5.14444f), 0.01f)
        SpeedUnit.entries.forEach { assertEquals(12.3f, it.toMps(it.fromMps(12.3f)), 1e-4f) }
    }

    @Test fun distances() {
        assertEquals(1.0, SpeedUnit.KMH.distance(1000.0), 1e-6)
        assertEquals(1.0, SpeedUnit.MPH.distance(1609.344), 1e-6)
        assertEquals(1.0, SpeedUnit.KN.distance(1852.0), 1e-6)
    }

    @Test fun defaultByCountry() {
        assertEquals(SpeedUnit.MPH, SpeedUnit.defaultFor("US"))
        assertEquals(SpeedUnit.MPH, SpeedUnit.defaultFor("gb"))
        assertEquals(SpeedUnit.KMH, SpeedUnit.defaultFor("AT"))
        assertEquals(SpeedUnit.KMH, SpeedUnit.defaultFor("CA"))
        assertEquals(SpeedUnit.KMH, SpeedUnit.defaultFor(""))
        assertEquals(SpeedUnit.KMH, SpeedUnit.defaultFor(null))
    }

    @Test fun formatting() {
        assertEquals("–", formatSpeed(Float.NaN))
        assertEquals("0", formatSpeed(-0.4f))
        assertEquals("88", formatSpeed(87.5f))
        assertEquals("12,3", formatDistance(12.34, ','))
        assertEquals("0.0", formatDistance(0.0))
        assertEquals("99.9", formatDistance(99.94))
        assertEquals("100", formatDistance(99.96))
        assertEquals("1234", formatDistance(1234.4))
    }

    @Test fun limitSteps() {
        assertEquals(55, Limit.step(50, 1))
        assertEquals(10, Limit.step(10, -1))
        assertEquals(300, Limit.step(300, 1))
        assertEquals(50, Limit.clamp(52))
        // 50 km/h ~ 31 mph -> 30; 30 mph ~ 48 km/h -> 50
        assertEquals(30, Limit.convert(50, SpeedUnit.KMH, SpeedUnit.MPH))
        assertEquals(50, Limit.convert(30, SpeedUnit.MPH, SpeedUnit.KMH))
        assertEquals(300, Limit.convert(300, SpeedUnit.MPH, SpeedUnit.KMH))
    }

    // Band: Zahlen je 10, wenn genug Platz, sonst je 20; Lage relativ zum Zeiger
    @Test fun tape() {
        assertEquals(10, Tape.labelStep(8f, 40f))
        assertEquals(20, Tape.labelStep(4f, 40f))
        assertEquals(0f, Tape.offset(50f, 50f, 6f), 0f)
        assertEquals(-60f, Tape.offset(40f, 50f, 6f), 1e-4f)
    }

    // Ansage nur bei neuem Zehner oder Warnzustand
    @Test fun announce() {
        fun r(v: Float, a: Alert = Alert.NONE, p: GpsPhase = GpsPhase.FIX) = Reading.SEARCHING.copy(phase = p, speed = v, alert = a)
        assertEquals(announceKey(r(51f)), announceKey(r(58.4f)))
        assertTrue(announceKey(r(58.4f)) != announceKey(r(60f)))
        assertTrue(announceKey(r(52f)) != announceKey(r(52f, Alert.OVER)))
        assertEquals("LOST", announceKey(r(Float.NaN, p = GpsPhase.LOST)))
        assertEquals("SEARCHING", announceKey(Reading.SEARCHING))
    }

    @Test fun haversine() {
        val a = Track.fix(0.0, 0.0)
        val b = Track.fix(1.0, 1000.0)
        assertEquals(1000.0, Geo.distance(a, b), 1.0)
        assertEquals(0.0, Geo.distance(a, a), 1e-9)
    }
}
