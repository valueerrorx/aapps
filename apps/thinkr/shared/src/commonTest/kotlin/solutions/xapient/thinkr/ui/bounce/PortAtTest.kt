package solutions.xapient.thinkr.ui.bounce

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test
import solutions.xapient.thinkr.domain.bounce.Edge
import solutions.xapient.thinkr.domain.bounce.Port

// Tipp-Stelle -> Randfeld (Zellkoordinaten des Felds)
class PortAtTest {
    @Test fun tabsAroundTheField() {
        assertEquals(Port(Edge.TOP, 2), portAt(2.5f, -0.4f, 6))
        assertEquals(Port(Edge.BOTTOM, 0), portAt(0.1f, 6.5f, 6))
        assertEquals(Port(Edge.LEFT, 5), portAt(-0.6f, 5.9f, 6))
        assertEquals(Port(Edge.RIGHT, 3), portAt(6.3f, 3.2f, 6))
    }

    @Test fun nearTheEdgeInsideCounts() {
        assertEquals(Port(Edge.TOP, 4), portAt(4.5f, 0.2f, 6))
        assertEquals(Port(Edge.RIGHT, 1), portAt(5.8f, 1.5f, 6))
        assertNull(portAt(3f, 3f, 6))
    }

    @Test fun cornersAndFarAwayIgnored() {
        assertNull(portAt(-0.5f, -0.5f, 6))
        assertNull(portAt(6.5f, 6.5f, 6))
        assertNull(portAt(2f, -3f, 6))
    }
}
