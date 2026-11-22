package solutions.xapient.roundr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTest {
    private fun up(f: Field, v: Int) = f.step(v, +1)
    private fun down(f: Field, v: Int) = f.step(v, -1)

    @Test fun timeStepsSnapToGrid() {
        assertEquals(25, up(Field.WORK, 20))
        assertEquals(10, up(Field.WORK, 7))
        assertEquals(5, down(Field.WORK, 7))
        assertEquals(75, up(Field.WORK, 60))
        assertEquals(55, down(Field.WORK, 60))
        assertEquals(300, up(Field.WORK, 285))
        assertEquals(330, up(Field.WORK, 300))
        assertEquals(285, down(Field.WORK, 300))
        assertEquals(300, down(Field.WORK, 310))
    }

    @Test fun limits() {
        assertEquals(5, down(Field.WORK, 5))
        assertEquals(0, down(Field.REST, 5))
        assertEquals(0, down(Field.REST, 0))
        assertEquals(3600, up(Field.WORK, 3600))
        assertEquals(60, up(Field.PREP, 60))
        assertEquals(99, up(Field.ROUNDS, 99))
        assertEquals(1, down(Field.SETS, 1))
        assertEquals(9, up(Field.ROUNDS, 8))
    }

    // Hochzaehlen und wieder runter landet auf denselben Werten
    @Test fun stepsAreReversibleOnGrid() {
        var v = 5
        val seen = mutableListOf(v)
        while (v < 3600) { v = up(Field.WORK, v); seen += v }
        val back = mutableListOf(v)
        while (v > 5) { v = down(Field.WORK, v); back += v }
        assertEquals(seen, back.reversed())
    }

    @Test fun editingPresetDropsName() {
        val t = Presets.default
        assertTrue(t.isPreset)
        assertEquals("", t.step(Field.WORK, 1).name)
        // keine Aenderung an der Grenze: Name bleibt
        assertSame(t, t.step(Field.SETS, -1))
        val own = Workout("Beine", workSec = 30)
        assertEquals("Beine", own.step(Field.WORK, 1).name)
    }

    @Test fun cleanName() {
        assertEquals("Beine Po", Workout.cleanName("  Beine|Po\n"))
        assertEquals("x", Workout.cleanName("@x"))
        assertEquals(24, Workout.cleanName("a".repeat(40)).length)
    }

    @Test fun sameTiming() {
        assertTrue(Presets.default.sameTiming(Workout("x", 10, 20, 10, 8)))
        assertFalse(Presets.default.sameTiming(Workout("x", 10, 25, 10, 8)))
    }

    @Test fun presetsAreValid() {
        Presets.all.forEach { assertEquals(it, it.clamped()) }
        assertNull(Presets.all.groupBy { it.name }.values.firstOrNull { it.size > 1 })
    }
}
