package solutions.xapient.nordr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AccuracyTest {
    private val earth = Vec3(0f, 20f, -44f) // ~48 µT
    private var t = 1L

    private fun AccuracyMonitor.feed(seconds: Float, field: Vec3 = earth, status: Int = 3): Accuracy {
        var s = state
        repeat((seconds * 50).toInt()) {
            s = update(field, status, t)
            t += 20_000_000L
        }
        return s
    }

    @Test
    fun goodFieldAndHighStatusIsGood() {
        assertEquals(Accuracy.GOOD, AccuracyMonitor().feed(1f))
    }

    @Test
    fun lowStatusNeedsGraceTime() {
        val m = AccuracyMonitor()
        m.feed(0.5f)
        assertEquals(Accuracy.GOOD, m.feed(0.6f, status = 1))
        assertEquals(Accuracy.LOW, m.feed(0.6f, status = 1))
        assertEquals(Accuracy.LOW, m.feed(0.5f, status = 0))
    }

    @Test
    fun recoversImmediately() {
        val m = AccuracyMonitor()
        m.feed(2f, status = 0)
        assertEquals(Accuracy.LOW, m.state)
        m.update(earth, 2, t)
        assertEquals(Accuracy.GOOD, m.state)
    }

    @Test
    fun shortDipDoesNotNag() {
        val m = AccuracyMonitor()
        repeat(5) {
            m.feed(0.4f, status = 1)
            m.feed(0.1f, status = 3)
        }
        assertEquals(Accuracy.GOOD, m.state)
    }

    @Test
    fun strongFieldIsInterference() {
        val m = AccuracyMonitor()
        m.feed(1f)
        assertEquals(Accuracy.INTERFERENCE, m.feed(2f, field = Vec3(0f, 120f, -44f)))
        // Hysterese: knapp unter der Grenze bleibt die Stoerung
        assertEquals(Accuracy.INTERFERENCE, m.feed(2f, field = Vec3(0f, 0f, 68.5f)))
        assertEquals(Accuracy.GOOD, m.feed(2f))
    }

    @Test
    fun weakFieldIsInterference() {
        val m = AccuracyMonitor()
        assertEquals(Accuracy.INTERFERENCE, m.feed(2f, field = Vec3(0f, 5f, -8f)))
    }

    @Test
    fun interferenceBeatsLowStatus() {
        val m = AccuracyMonitor()
        assertEquals(Accuracy.INTERFERENCE, m.feed(2f, field = Vec3(0f, 150f, 0f), status = 0))
    }
}
