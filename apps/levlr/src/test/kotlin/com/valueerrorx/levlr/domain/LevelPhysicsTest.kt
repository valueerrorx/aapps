package com.valueerrorx.levlr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue("Feder schlaeft nicht ein", p.isSettled)
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
        assertTrue("Fluessigkeit soll nachschwappen", min < -10.5f)
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
