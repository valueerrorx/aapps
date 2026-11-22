package solutions.xapient.hudr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarningTest {
    private val w = Warning()

    @Test fun offWithoutLimit() {
        assertFalse(w.update(200f, null))
        assertEquals(Alert.NONE, w.state)
    }

    // angezeigt 50 bei Schwelle 50 ist noch erlaubt, 51 nicht
    @Test fun overWhenShownAboveLimit() {
        w.update(50.4f, 50)
        assertEquals(Alert.NEAR, w.state)
        assertTrue(w.update(50.5f, 50))
        assertEquals(Alert.OVER, w.state)
    }

    @Test fun nearFromNinetyPercent() {
        w.update(44.9f, 50)
        assertEquals(Alert.NONE, w.state)
        w.update(45f, 50)
        assertEquals(Alert.NEAR, w.state)
    }

    // Zittern um die Schwelle: ein Ton, Rot bleibt bis 2 darunter
    @Test fun hysteresisOver() {
        var beeps = 0
        listOf(49f, 51f, 50f, 49.5f, 51.2f, 48.6f, 50.6f).forEach { if (w.update(it, 50)) beeps++ }
        assertEquals(1, beeps)
        assertEquals(Alert.OVER, w.state)
        w.update(48.4f, 50)
        assertEquals(Alert.NEAR, w.state)
        assertTrue(w.update(51f, 50))
    }

    @Test fun hysteresisNear() {
        w.update(46f, 50)
        w.update(44.2f, 50)
        assertEquals(Alert.NEAR, w.state)
        w.update(43.9f, 50)
        assertEquals(Alert.NONE, w.state)
    }

    // Signal weg: Zustand bleibt, kein zweiter Ton bei Rueckkehr
    @Test fun noValueKeepsState() {
        assertTrue(w.update(60f, 50))
        assertFalse(w.update(Float.NaN, 50))
        assertEquals(Alert.OVER, w.state)
        assertFalse(w.update(60f, 50))
    }

    @Test fun jumpStraightToOverBeeps() {
        assertTrue(w.update(80f, 50))
        w.reset()
        assertEquals(Alert.NONE, w.state)
    }
}
