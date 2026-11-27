package solutions.xapient.nordr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class CompassPhysicsTest {
    private fun CompassPhysics.settle() {
        repeat(600) { step(1f / 60f) }
    }

    @Test
    fun tiltSnapsFirstThenFollowsBeyondDeadband() {
        val p = CompassPhysics()
        assertFalse(p.setTilt(0.1f, 0.2f))
        assertEquals(0.1f, p.glintX.value, 1e-6f)
        // Zittern unter dem Totband weckt nichts
        assertFalse(p.setTilt(0.13f, 0.17f))
        assertTrue(p.isSettled)
        assertTrue(p.setTilt(0.2f, 0.2f))
        assertFalse(p.isSettled)
        p.settle()
        assertTrue(p.isSettled)
        assertEquals(0.2f, p.glintX.value, 0.01f)
    }

    @Test
    fun dialTakesShortestWay() {
        val p = CompassPhysics()
        p.setTarget(CompassReading(valid = true, heading = 350f))
        p.setTarget(CompassReading(valid = true, heading = 10f))
        assertEquals(370f, p.dial.target, 1e-3f)
        p.settle()
        assertTrue(p.isSettled)
    }
}
