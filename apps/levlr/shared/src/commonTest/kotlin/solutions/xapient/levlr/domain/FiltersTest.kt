package solutions.xapient.levlr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class FiltersTest {
    @Test
    fun smootherConvergesAndDampsNoise() {
        val s = VectorSmoother()
        s.update(Vec3(0f, 0f, 9.81f), 0f)
        var out = Vec3.ZERO
        repeat(200) { i ->
            val noise = if (i % 2 == 0) 0.05f else -0.05f
            out = s.update(Vec3(0.2f + noise, 0f, 9.81f), 0.02f)
        }
        assertEquals(0.2f, out.x, 0.01f)
    }

    @Test
    fun smootherFollowsFastOnBigMove() {
        val s = VectorSmoother()
        s.update(Vec3(0f, 9.81f, 0f), 0f)
        val out = s.update(Vec3(9.81f, 0f, 0f), 0.02f)
        // schnelle Zeitkonstante: nach 20 ms schon ein deutlicher Teil des Wegs
        assertTrue(out.x > 2f)
    }

    @Test
    fun deadbandHoldsSmallChanges() {
        val d = Deadband(0.03f)
        assertEquals(1f, d.update(1f), 0f)
        assertEquals(1f, d.update(1.02f), 0f)
        assertEquals(1.05f, d.update(1.05f), 0f)
    }

    @Test
    fun snapHasHysteresis() {
        val s = SnapDetector(enter = 0.05f, exit = 0.2f)
        assertFalse(s.update(0.1f))
        assertTrue(s.update(0.04f))
        assertFalse(s.update(0.15f)) // bleibt eingerastet, meldet aber nicht erneut
        assertTrue(s.isLevel)
        assertFalse(s.update(0.25f))
        assertFalse(s.isLevel)
        assertTrue(s.update(0.01f))
    }

    @Test
    fun modeHysteresis() {
        val m = ModeDetector(toEdge = 50f, toFlat = 35f)
        assertEquals(LevelMode.FLAT, m.update(10f))
        assertEquals(LevelMode.FLAT, m.update(45f))
        assertEquals(LevelMode.EDGE, m.update(55f))
        assertEquals(LevelMode.EDGE, m.update(40f))
        assertEquals(LevelMode.FLAT, m.update(30f))
    }

    @Test
    fun modeStartsWithBestGuess() {
        assertEquals(LevelMode.EDGE, ModeDetector().update(88f))
    }
}
