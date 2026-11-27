package solutions.xapient.nordr.domain

import kotlin.test.Test
import kotlin.test.assertEquals

// Pruefwerte aus WMM2025_TestValues.txt (NOAA): Jahr, Hoehe km, Breite, Laenge, Deklination
class WmmTest {
    private val official = listOf(
        doubleArrayOf(2025.0, 28.0, 89.0, -121.0, -99.77),
        doubleArrayOf(2025.0, 65.0, 43.0, 93.0, 0.50),
        doubleArrayOf(2025.0, 51.0, -33.0, 109.0, -5.49),
        doubleArrayOf(2025.5, 50.0, -70.0, -133.0, 57.21),
        doubleArrayOf(2026.0, 82.0, -64.0, 87.0, -81.74),
        doubleArrayOf(2026.5, 12.0, -79.0, 115.0, -137.58),
        doubleArrayOf(2027.0, 54.0, 54.0, 178.0, 0.63),
        doubleArrayOf(2028.0, 86.0, -85.0, -79.0, 41.09),
        doubleArrayOf(2028.5, 55.0, 86.0, 70.0, 67.64),
        doubleArrayOf(2029.0, 41.0, 42.0, -19.0, -4.13),
        doubleArrayOf(2029.5, 33.0, 17.0, 5.0, 0.89),
    )

    @Test
    fun matchesNoaaTestValues() {
        for (v in official) {
            assertEquals(v[4], Wmm.declination(v[2], v[3], v[1], v[0]), 0.01, "bei ${v.toList()}")
        }
    }

    @Test
    fun viennaIsAboutFiveDegreesEast() {
        // 1. Juni 2026, Meereshoehe
        val d = Wmm.declination(48.21f, 16.37f, 1_780_272_000_000L)
        assertEquals(5.3f, d, 0.3f)
    }
}
