package solutions.xapient.nordr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionsTest {
    @Test
    fun idsAndTimeZonesAreUnique() {
        val ids = Regions.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val zones = Regions.all.flatMap { it.timeZones }
        assertEquals(zones.size, zones.toSet().size)
    }

    @Test
    fun coordinatesArePlausible() {
        Regions.all.forEach {
            assertTrue(it.id, it.lat in -90f..90f && it.lon in -180f..180f)
        }
    }

    @Test
    fun guessFromTimeZone() {
        assertEquals("vienna", Regions.forTimeZone("Europe/Vienna")?.id)
        assertEquals("zurich", Regions.forTimeZone("Europe/Zurich")?.id)
        assertNull(Regions.forTimeZone("Antarctica/Troll"))
    }

    @Test
    fun lookupById() {
        assertEquals("tokyo", Regions.byId("tokyo")?.id)
        assertNull(Regions.byId(null))
        assertNull(Regions.byId("atlantis"))
    }
}
