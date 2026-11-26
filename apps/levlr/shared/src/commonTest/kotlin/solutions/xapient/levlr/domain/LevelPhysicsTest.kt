package solutions.xapient.levlr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class LevelPhysicsTest {
    @Test
    fun bubbleMovesToHigherSideAndSettles() {
        val p = LevelPhysics()
        p.setTarget(LevelReading(LevelMode.EDGE, edgeAngle = 0f))
        assertTrue(p.isSettled)
        // rechts tiefer -> Blase nach links
        p.setTarget(LevelReading(LevelMode.EDGE, edgeAngle = 3f))
        assertFalse(p.isSettled)
        var frames = 0
        while (!p.isSettled && frames < 600) {
            p.step(1f / 60f); frames++
        }
        assertTrue(p.isSettled, "Feder schlaeft nicht ein")
        assertTrue(p.bubble.value < 0f)
        assertEquals(-3f, p.surface.value, 0.05f)
    }

    @Test
    fun sloshOvershoots() {
        val p = LevelPhysics()
        p.setTarget(LevelReading(LevelMode.EDGE, edgeAngle = 0f))
        p.setTarget(LevelReading(LevelMode.EDGE, edgeAngle = 10f))
        var min = 0f
        repeat(120) { p.step(1f / 60f); min = minOf(min, p.surface.value) }
        assertTrue(min < -10.5f, "Fluessigkeit soll nachschwappen")
    }

    @Test
    fun flatBubbleStaysInside() {
        val p = LevelPhysics()
        p.setTarget(LevelReading(LevelMode.FLAT, flatX = 40f, flatY = 40f))
        repeat(300) { p.step(1f / 60f) }
        val r = kotlin.math.hypot(p.bubbleX.value, p.bubbleY.value)
        assertTrue(r <= 1.0f)
        assertTrue(p.bubbleX.value > 0f && p.bubbleY.value < 0f)
    }
}
