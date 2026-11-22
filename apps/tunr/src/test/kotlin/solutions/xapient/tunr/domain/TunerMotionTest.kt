package solutions.xapient.tunr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerMotionTest {
    private fun run(m: TunerMotion, seconds: Float) {
        var t = 0f
        while (t < seconds) {
            m.step(1f / 60f); t += 1f / 60f
        }
    }

    @Test fun idleIsSettled() = assertTrue(TunerMotion().isSettled)

    @Test fun needleFollowsAndSleeps() {
        val m = TunerMotion()
        m.setTarget(Reading(active = true, heard = true, cents = 0f, target = 69, inTune = true, locks = 0))
        run(m, 2f)
        assertTrue("0 Cent: Strobe steht, alles ruht", m.isSettled)
        m.setTarget(Reading(active = true, heard = true, cents = 20f, target = 69))
        assertFalse(m.isSettled)
        run(m, 2f)
        assertEquals(20f, m.needle.value, 0.1f)
        // klingt verstimmt: Strobe dreht weiter
        assertFalse(m.isSettled)
        m.setTarget(Reading(active = false, heard = true, cents = 20f, target = 69))
        run(m, 3f)
        assertTrue(m.isSettled)
        assertEquals(0f, m.presence.value, 0.01f)
    }

    // zu hoch dreht im Uhrzeigersinn, zu tief dagegen
    @Test fun strobeDirection() {
        val m = TunerMotion()
        m.setTarget(Reading(active = true, heard = true, cents = 10f, target = 69))
        m.step(0.1f)
        assertEquals(6f, m.strobe, 0.01f)
        val n = TunerMotion()
        n.setTarget(Reading(active = true, heard = true, cents = -10f, target = 69))
        n.step(0.1f)
        assertEquals(354f, n.strobe, 0.01f)
    }

    @Test fun needleClampedAtEdge() {
        val m = TunerMotion()
        m.pose(Reading(active = true, heard = true, cents = -180f, target = 40))
        assertEquals(-TunerMotion.NEEDLE_LIMIT, m.needle.value, 0f)
    }

    // Glanz nur bei neuem Einrasten, nicht beim ersten Anzeigen
    @Test fun shineOnLock() {
        val m = TunerMotion()
        m.setTarget(Reading(active = true, heard = true, inTune = true, locks = 3))
        assertEquals(1f, m.shine, 0f)
        m.setTarget(Reading(active = true, heard = true, inTune = true, locks = 4))
        assertEquals(0f, m.shine, 0f)
        run(m, 1.2f)
        assertEquals(1f, m.shine, 0f)
    }
}
