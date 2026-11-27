package solutions.xapient.hudr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import solutions.xapient.hudr.domain.Track.SEC

class SpeedoTest {
    private val s = Speedo()
    private val kmh = SpeedUnit.KMH
    private var pos = 0.0

    // Position waechst mit der Geschwindigkeit (Start braucht echte Verschiebung)
    private fun drive(from: Int, to: Int, mps: Float, limit: Int? = null): Int {
        var beeps = 0
        for (i in from..to) {
            pos += mps
            if (s.onFix(Track.fix(i.toDouble(), meters = pos, speed = mps), kmh, limit)) beeps++
        }
        return beeps
    }

    @Test fun searchingUntilFirstFix() {
        val r = s.reading(0, kmh, null)
        assertEquals(GpsPhase.SEARCHING, r.phase)
        assertTrue(r.speed.isNaN())
        assertTrue(r.trend.isNaN())
    }

    @Test fun readingInUnit() {
        drive(0, 30, 25f)
        val r = s.reading(30 * SEC, kmh, null)
        assertEquals(GpsPhase.FIX, r.phase)
        assertEquals(90f, r.speed, 0.01f)
        assertEquals(90f, r.max, 0.01f)
        // Fahrt bestaetigt nach 2 s, Start als Trapez aus 0: 12,5 m + 28 x 25 m in 29 s
        assertEquals(0.7125, r.distance, 1e-6)
        assertEquals(88.45f, r.avg, 0.01f)
        assertTrue(r.moving)
        // gleichmaessig: Trend = Wert
        assertEquals(90f, r.trend, 0.01f)
        val mph = s.reading(30 * SEC, SpeedUnit.MPH, null)
        assertEquals(55.9f, mph.speed, 0.1f)
        assertEquals(0.44273, mph.distance, 1e-4)
    }

    // nach 5 s ohne Fix: verloren, keine Zahl, Fahrtwerte bleiben
    @Test fun staleFixIsLost() {
        drive(0, 10, 10f)
        assertEquals(GpsPhase.FIX, s.reading(15 * SEC, kmh, null).phase)
        val r = s.reading(16 * SEC, kmh, null)
        assertEquals(GpsPhase.LOST, r.phase)
        assertTrue(r.speed.isNaN())
        assertFalse(r.moving)
        assertEquals(36f, r.max, 0.01f)
    }

    // nur geaenderte Zahlen melden ein neues Bild
    @Test fun satellites() {
        assertTrue(s.onSatellites(9, 21))
        assertFalse(s.onSatellites(9, 21))
        val r = s.reading(0, kmh, null)
        assertEquals(9, r.used)
        assertEquals(21, r.visible)
        assertTrue(s.onSatellites(5, 3))
        assertEquals(5, s.reading(0, kmh, null).visible)
    }

    // Trend: 3 s voraus aus der geglaetteten Beschleunigung (1 m/s² = 10,8 km/h)
    @Test fun trendLeadsWhileAccelerating() {
        var v = 10f
        for (i in 0..30) {
            v += 1f
            pos += v
            s.onFix(Track.fix(i.toDouble(), meters = pos, speed = v), kmh, null)
        }
        val r = s.reading(30 * SEC, kmh, null)
        assertEquals(10.8f, r.trend - r.speed, 0.6f)
    }

    // kurze Spitze zaehlt nicht als Max (93,6 km/h); nur der geglaettete Auslauf bleibt knapp ueber 72
    @Test fun spikeNotMax() {
        drive(0, 10, 20f)
        drive(11, 11, 26f)
        drive(12, 20, 20f)
        assertEquals(72f, s.reading(20 * SEC, kmh, null).max, 1.5f)
    }

    @Test fun beepOnceWhenOver() {
        assertEquals(1, drive(0, 20, 16f, limit = 50))
        assertEquals(Alert.OVER, s.reading(20 * SEC, kmh, 50).alert)
        // ohne Schwelle keine Farbe
        assertEquals(Alert.NONE, s.reading(20 * SEC, kmh, null).alert)
        // Signal weg: neutral
        assertEquals(Alert.NONE, s.reading(30 * SEC, kmh, 50).alert)
    }

    @Test fun saveRestore() {
        drive(0, 20, 20f)
        val saved = s.save()
        val other = Speedo().apply { restore(saved) }
        val r = other.reading(0, kmh, null)
        assertEquals(GpsPhase.SEARCHING, r.phase)
        assertEquals(72f, r.max, 0.01f)
        assertEquals(s.reading(20 * SEC, kmh, null).distance, r.distance, 1e-9)
    }

    @Test fun resetKeepsGps() {
        drive(0, 10, 10f)
        s.reset()
        val r = s.reading(10 * SEC, kmh, null)
        assertEquals(GpsPhase.FIX, r.phase)
        assertTrue(r.max.isNaN())
        assertEquals(0.0, r.distance, 0.0)
        assertEquals(36f, r.speed, 0.01f)
    }

    @Test fun restartDropsFix() {
        drive(0, 10, 10f)
        s.onSatellites(8, 12)
        s.restart()
        val r = s.reading(10 * SEC, kmh, null)
        assertEquals(GpsPhase.SEARCHING, r.phase)
        assertEquals(0, r.used)
        assertEquals(36f, r.max, 0.01f)
    }
}
