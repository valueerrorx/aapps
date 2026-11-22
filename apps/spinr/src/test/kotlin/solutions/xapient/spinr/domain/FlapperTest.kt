package solutions.xapient.spinr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlapperTest {
    @Test
    fun approachingPegPushesInDirectionOfTravel() {
        val f = Flapper(zoneDeg = 6.0)
        // im Uhrzeigersinn kommen die Stifte von links
        assertEquals(0.0, f.contact(-7.0, 1, 45.0), 1e-9)
        assertEquals(0.5, f.contact(-3.0, 1, 45.0), 1e-9)
        assertEquals(1.0, f.contact(0.0, 1, 45.0), 1e-9)
        assertEquals(0.0, f.contact(0.5, 1, 45.0), 1e-9)
        // gegen den Uhrzeigersinn gespiegelt
        assertEquals(-0.5, f.contact(3.0, -1, 45.0), 1e-9)
        assertEquals(0.0, f.contact(-3.0, -1, 45.0), 1e-9)
    }

    @Test
    fun zoneShrinksWithNarrowSegments() {
        val f = Flapper(zoneDeg = 6.0)
        // 50 Segmente = 7,2 Grad, Zone hoechstens 45 %
        assertEquals(0.0, f.contact(-3.3, 1, 7.2), 1e-9)
        assertTrue(f.contact(-3.0, 1, 7.2) > 0)
    }

    @Test
    fun snapsBackAfterPegSlipsPast() {
        val f = Flapper(zoneDeg = 6.0, releaseSeconds = 0.04)
        f.update(-0.2, 1, 45.0, 1.0 / 60)
        assertTrue(f.value > 0.9)
        f.update(0.3, 1, 45.0, 1.0 / 120)
        assertTrue(f.value in 0.1..0.9)
        assertTrue(!f.isSettled)
        repeat(30) { f.update(0.3, 1, 45.0, 1.0 / 60) }
        assertEquals(0.0, f.value, 0.0)
        assertTrue(f.isSettled)
    }

    @Test
    fun restingAgainstPegStaysBent() {
        val f = Flapper(zoneDeg = 6.0)
        repeat(10) { f.update(-2.0, 1, 45.0, 1.0 / 60) }
        assertEquals(2.0 / 3.0, f.value, 1e-9)
        assertTrue(f.isSettled)
    }
}
