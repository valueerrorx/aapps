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
        m.step(1f / 60f)
        assertTrue(m.strobe in 0.5f..1.5f)
        val n = TunerMotion()
        n.setTarget(Reading(active = true, heard = true, cents = -10f, target = 69))
        n.step(1f / 60f)
        assertTrue(n.strobe in 358.5f..359.5f)
    }

    // kein Rueckwaerts-Effekt: je Bild weniger als 0,4 Strichabstand, beide Ringe, auch bei Rucklern
    @Test fun strobeNoAliasing() {
        val outer = 360f / TunerMotion.STROBE_OUTER
        val inner = 360f / TunerMotion.STROBE_INNER
        var last = -Float.MAX_VALUE
        for (c in -200..200) {
            for (dt in listOf(1f / 120f, 1f / 60f, 1f / 30f, 0.2f)) {
                val m = TunerMotion()
                m.setTarget(Reading(active = true, heard = true, cents = c.toFloat(), target = 69))
                m.step(dt)
                val shift = if (m.strobe > 180f) 360f - m.strobe else m.strobe
                assertTrue("aussen $c Cent: $shift", shift < 0.4f * outer)
                assertTrue("innen $c Cent", shift * TunerMotion.INNER_RATIO < 0.4f * inner)
                if (c != 0) assertEquals("Richtung $c", c > 0, m.strobe in 0f..180f)
            }
            // mehr Abweichung = schneller
            val s = TunerMotion.strobeSpeedOf(c.toFloat())
            assertTrue("monoton $c", if (c in -60..60) s > last else s >= last)
            last = s
        }
        assertEquals(6f, TunerMotion.strobeSpeedOf(1f), 0.01f)
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
