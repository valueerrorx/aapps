package com.valueerrorx.nordr.ui

import com.valueerrorx.nordr.domain.Regions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RegionNamesTest {
    @Test
    fun everyRegionHasItsOwnName() {
        val ids = Regions.all.map { regionNameRes(it.id) }
        ids.forEachIndexed { i, id -> assertNotEquals(Regions.all[i].id, 0, id) }
        assertEquals(ids.size, ids.toSet().size)
    }
}
