package solutions.xapient.nordr.ui

import solutions.xapient.nordr.domain.Regions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RegionNamesTest {
    @Test
    fun everyRegionHasItsOwnName() {
        val names = Regions.all.map { regionNameRes(it.id) }
        names.forEachIndexed { i, res -> assertNotNull(res, Regions.all[i].id) }
        assertEquals(names.size, names.toSet().size)
    }
}
