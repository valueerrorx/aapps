package solutions.xapient.hudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.hudr.domain.Track.SEC

class SpeedoTest {
    private val s = Speedo()
    private val kmh = SpeedUnit.KMH

    private fun drive(from: Int, to: Int, mps: Float, limit: Int? = null): Int {
        var beeps = 0
        for (i in from..to) if (s.onFix(Track.fix(i.toDouble(), speed = mps), kmh, limit)) beeps++
        return beeps
    }

    @Test fun searchingUntilFirstFix() {
        val r = s.reading(0, kmh, null)
        assertEquals(GpsPhase.SEARCHING, r.phase)
        assertTrue(r.speed.isNaN())
        assertEquals(40, r.scale)
    }

    @Test fun readingInUnit() {
        drive(0, 30, 25f)
        val r = s.reading(30 * SEC, kmh, null)
        assertEquals(GpsPhase.FIX, r.phase)
        assertEquals(90f, r.speed, 0.01f)
        assertEquals(90f, r.max, 0.01f)
        // erster Fahrt-Fix startet als Trapez aus 0: 12,5 m + 29 x 25 m in 30 s
        assertEquals(0.7375, r.distance, 1e-6)
        assertEquals(88.5f, r.avg, 0.01f)
        assertTrue(r.moving)
        val mph = s.reading(30 * SEC, SpeedUnit.MPH, null)
        assertEquals(55.9f, mph.speed, 0.1f)
        assertEquals(0.45826, mph.distance, 1e-4)
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

    @Test fun satellites() {
        s.onSatellites(9, 21)
        val r = s.reading(0, kmh, null)
        assertEquals(9, r.used)
        assertEquals(21, r.visible)
        s.onSatellites(5, 3)
        assertEquals(5, s.reading(0, kmh, null).visible)
    }

    // Skala waechst mit Max und Schwelle, schrumpft nicht, Reset faengt neu an
    @Test fun scaleGrowsOnly() {
        assertEquals(80, s.reading(0, kmh, 50).scale)
        drive(0, 10, 30f)
        assertEquals(160, s.reading(10 * SEC, kmh, null).scale)
        drive(11, 20, 5f)
        assertEquals(160, s.reading(20 * SEC, kmh, null).scale)
        s.reset()
        assertEquals(40, s.reading(20 * SEC, kmh, null).scale)
        // ueber der groessten Skala bleibt die groesste
        assertEquals(240, s.reading(20 * SEC, kmh, 300).scale)
        assertEquals(3, Speedo.fit(SpeedUnit.MPH, 170f))
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
        assertEquals(80, r.scale)
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
