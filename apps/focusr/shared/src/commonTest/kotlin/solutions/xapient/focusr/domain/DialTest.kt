package solutions.xapient.focusr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class DialTest {
    private val eps = 1e-9

    @Test fun fractionClockwiseFromTwelve() {
        assertEquals(0.0, Dial.fraction(0f, -10f), eps)
        assertEquals(0.25, Dial.fraction(10f, 0f), eps)
        assertEquals(0.5, Dial.fraction(0f, 10f), eps)
        assertEquals(0.75, Dial.fraction(-10f, 0f), eps)
        assertEquals(0.125, Dial.fraction(10f, -10f), eps)
    }

    @Test fun fractionJustLeftOfTwelveIsAlmostFull() {
        val f = Dial.fraction(-0.1f, -10f)
        assertTrue(f > 0.99 && f < 1.0, "f=$f")
    }

    // Finger von a nach b (Anteile, auch > 1 bzw. < 0) in kleinen Schritten
    private fun DialDrag.sweep(from: Double, to: Double): Double {
        val n = (kotlin.math.abs(to - from) * 100).toInt().coerceAtLeast(1)
        for (i in 0..n) move(((from + (to - from) * i / n) % 1.0 + 1.0) % 1.0)
        return value
    }

    @Test fun firstMoveJumpsToFinger() {
        assertEquals(0.75, DialDrag().move(0.75), eps)
    }

    @Test fun followTracksSmallMoves() {
        val d = DialDrag()
        d.move(0.3)
        assertEquals(0.31, d.move(0.31), eps)
        assertEquals(0.29, d.move(0.29), eps)
    }

    @Test fun pinsFullWhenCrossingTwelveClockwise() {
        val d = DialDrag()
        d.move(0.98)
        assertEquals(1.0, d.move(0.02), eps)
        assertEquals(1.0, d.sweep(0.02, 0.45), eps)
        // zurueck ueber 12 Uhr: folgt wieder
        d.sweep(0.45, 0.0)
        assertEquals(0.97, d.move(0.97), eps)
    }

    @Test fun pinsEmptyWhenCrossingTwelveCounterClockwise() {
        val d = DialDrag()
        d.move(0.02)
        assertEquals(0.0, d.move(0.98), eps)
        assertEquals(0.0, d.sweep(0.98, 0.7), eps)
        d.sweep(0.7, 1.0)
        assertEquals(0.03, d.move(0.03), eps)
    }

    // Ueberdrehen springt nicht (frueher: voll, weiter bis 6 Uhr -> halb); 1,5 und 2 Umdrehungen
    @Test fun overdriveClockwiseStaysFullUntilBackOverTwelve() {
        for (turns in listOf(1.5, 2.0)) {
            val d = DialDrag()
            d.move(0.0)
            assertEquals(0.75, d.sweep(0.0, 0.75), eps)
            assertEquals(1.0, d.sweep(0.75, turns), eps)
            // zurueck ueber 12 Uhr: greift wieder
            assertEquals(0.9, d.sweep(turns, kotlin.math.floor(turns) - 0.1), 0.011, "bei $turns")
        }
    }

    @Test fun overdriveCounterClockwiseStaysEmptyUntilBackOverTwelve() {
        for (turns in listOf(1.5, 2.0)) {
            val d = DialDrag()
            d.move(0.25)
            assertEquals(0.0, d.sweep(0.25, -turns), eps)
            assertEquals(0.1, d.sweep(-turns, -kotlin.math.floor(turns) + 0.1), 0.011, "bei $turns")
        }
    }

    @Test fun edgeGrab() {
        assertTrue(Dial.nearEdge(0.3, 0.3f))
        assertTrue(Dial.nearEdge(0.3 + 19.0 / 360, 0.3f))
        assertFalse(Dial.nearEdge(0.3 + 25.0 / 360, 0.3f))
        // ueber 12 Uhr hinweg
        assertTrue(Dial.nearEdge(0.99, 0.02f))
        assertTrue(Dial.nearEdge(0.01, 1f))
        assertFalse(Dial.nearEdge(0.5, 1f))
    }

    @Test fun snapToSteps() {
        assertEquals(15 * Timer.MINUTE, Dial.snap(0.25, Scale.MIN60))
        assertEquals(15 * Timer.MINUTE, Dial.snap(0.2541, Scale.MIN60))
        assertEquals(16 * Timer.MINUTE, Dial.snap(0.2589, Scale.MIN60))
        assertEquals(60 * Timer.MINUTE, Dial.snap(1.0, Scale.MIN60))
        assertEquals(0L, Dial.snap(-0.3, Scale.MIN60))
        assertEquals(30_000L, Dial.snap(0.1, Scale.MIN5))
        assertEquals(40_000L, Dial.snap(1.0 / 30, Scale.MIN20))
        assertEquals(30 * Timer.MINUTE, Dial.snap(0.25, Scale.MIN120))
    }

    @Test fun scaleSteps() {
        assertEquals(5_000L, Scale.MIN5.stepMs)
        assertEquals(20_000L, Scale.MIN20.stepMs)
        assertEquals(60_000L, Scale.MIN60.stepMs)
        assertEquals(120_000L, Scale.MIN120.stepMs)
    }

    @Test fun scaleLabels() {
        assertEquals(listOf(0, 1, 2, 3, 4), Scale.MIN5.labels.map { it.second })
        assertEquals((0 until 20 step 2).toList(), Scale.MIN20.labels.map { it.second })
        assertEquals((0 until 60 step 5).toList(), Scale.MIN60.labels.map { it.second })
        assertEquals((0 until 120 step 10).toList(), Scale.MIN120.labels.map { it.second })
        assertEquals(listOf(0, 5, 10), Scale.MIN60.labels.take(3).map { it.first })
    }

    @Test fun scaleFromMinutes() {
        assertEquals(Scale.MIN20, Scale.fromMinutes(20))
        assertEquals(Scale.MIN60, Scale.fromMinutes(7))
    }

    @Test fun fillClamps() {
        assertEquals(0.5f, Dial.fill(30 * Timer.MINUTE, Scale.MIN60), 1e-6f)
        assertEquals(1f, Dial.fill(90 * Timer.MINUTE, Scale.MIN60), 1e-6f)
        assertEquals(0f, Dial.fill(-1, Scale.MIN60), 1e-6f)
    }

    @Test fun tickFollowsPixelSpeed() {
        // 60 min auf r = 500 px: Rand wandert ~0,87 px/s -> 1/s reicht
        assertEquals(1000L, Dial.tickMs(Scale.MIN60, 500f))
        // 5 min auf r = 500 px: ~10,5 px/s -> ~95 ms
        assertEquals(95L, Dial.tickMs(Scale.MIN5, 500f))
        // nie schneller als ein Frame
        assertEquals(16L, Dial.tickMs(Scale.MIN5, 50_000f))
        assertEquals(1000L, Dial.tickMs(Scale.MIN5, 0f))
    }

    @Test fun nextSecond() {
        assertEquals(234L, Dial.msToNextSecond(60_234))
        assertEquals(1000L, Dial.msToNextSecond(60_000))
    }
}
