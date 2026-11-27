package solutions.xapient.thinkr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class SpringTest {
    @Test fun stepSettlesWithOvershoot() {
        assertEquals(0.0, Spring.step(0.0), 0.0)
        assertEquals(0.0, Spring.step(-50.0), 0.0)
        assertEquals(1.0, Spring.step(5_000.0), 0.0)
        val peak = (1..600).maxOf { Spring.step(it.toDouble()) }
        assertTrue(peak > 1.05 && peak < 1.3)
        // ohne Sprung am Ende der Feder
        assertEquals(1.0, Spring.step(800.0), 0.01)
    }

    @Test fun kickDiesOut() {
        assertEquals(0.0, Spring.kick(0.0), 0.0)
        assertTrue((1..200).maxOf { Spring.kick(it.toDouble()) } > 0.4)
        assertEquals(0.0, Spring.kick(2_000.0), 0.0)
    }
}
