package solutions.xapient.nordr.domain

import kotlin.test.assertEquals
import kotlin.test.Test

class AnglesTest {
    @Test
    fun normalizeWrapsIntoRange() {
        assertEquals(350f, Angles.normalize(-10f), 1e-4f)
        assertEquals(10f, Angles.normalize(370f), 1e-4f)
        assertEquals(0f, Angles.normalize(360f), 0f)
        assertEquals(0f, Angles.normalize(-720f), 0f)
        assertEquals(359.5f, Angles.normalize(-0.5f), 1e-4f)
    }

    @Test
    fun deltaTakesShortestWay() {
        assertEquals(20f, Angles.delta(350f, 10f), 1e-4f)
        assertEquals(-20f, Angles.delta(10f, 350f), 1e-4f)
        assertEquals(-90f, Angles.delta(45f, -45f), 1e-4f)
        assertEquals(180f, Angles.delta(0f, 180f), 1e-4f)
        assertEquals(180f, Angles.delta(180f, 0f), 1e-4f)
        assertEquals(0f, Angles.delta(720f, 0f), 1e-4f)
    }

    @Test
    fun unwrapStaysContinuousAcrossNorth() {
        var a = 340f
        val out = listOf(355f, 5f, 15f, 25f).map { a = Angles.unwrap(a, it); a }
        assertEquals(listOf(355f, 365f, 375f, 385f), out)
        assertEquals(-5f, Angles.unwrap(10f, 355f), 1e-4f)
    }

    @Test
    fun wholeDegreesNeverShow360() {
        assertEquals(0, Angles.whole(359.6f))
        assertEquals(359, Angles.whole(359.4f))
        assertEquals(247, Angles.whole(246.7f))
    }

    @Test
    fun sixteenPointRose() {
        assertEquals(0, Angles.point16(0f))
        assertEquals(0, Angles.point16(355f))
        assertEquals(4, Angles.point16(90f))
        assertEquals(11, Angles.point16(247f)) // WSW
        assertEquals(15, Angles.point16(337.5f)) // NNW
        assertEquals(0, Angles.point16(349f))
    }
}
