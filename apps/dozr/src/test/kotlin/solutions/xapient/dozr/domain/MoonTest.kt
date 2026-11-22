package solutions.xapient.dozr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MoonTest {
    @Test fun fractionAlongArc() {
        assertEquals(0f, Moon.fractionOf(0), 1e-6f)
        assertEquals(0.5f, Moon.fractionOf(60), 1e-6f)
        assertEquals(1f, Moon.fractionOf(120), 1e-6f)
        assertEquals(1f, Moon.fraction(500 * Sleep.MINUTE), 1e-6f)
        assertEquals(0f, Moon.fraction(-5), 1e-6f)
    }

    @Test fun snapsToFiveMinutes() {
        assertEquals(30, Moon.snap(0.25))
        assertEquals(30, Moon.snap(0.26))
        assertEquals(35, Moon.snap(0.28))
        assertEquals(5, Moon.snap(0.0))
        assertEquals(120, Moon.snap(1.0))
        assertEquals(120, Moon.snap(3.0))
    }

    private fun at(deg: Double) = cos(deg * PI / 180).toFloat() to sin(deg * PI / 180).toFloat()

    @Test fun dragFollowsFingerAboveHorizon() {
        val d = MoonDrag(0.25)
        val (x, y) = at(90.0)
        assertEquals(0.5, d.move(x, y), 1e-6)
        val (x2, y2) = at(135.0)
        assertEquals(0.75, d.move(x2, y2), 1e-6)
    }

    // Erster Punkt greift dort, wo der Finger ist, auch weit vom Mond weg
    @Test fun firstTouchGrabsAbsolutely() {
        val d = MoonDrag(0.25)
        val (x, y) = at(150.0)
        assertEquals(150.0 / 180.0, d.move(x, y), 1e-6)
    }

    // Unter dem Horizont am naechsten Ende stehen bleiben
    @Test fun belowHorizonPinsToNearestEnd() {
        val low = MoonDrag(0.1)
        assertEquals(0.0, low.move(1f, -0.3f), 1e-9)
        val high = MoonDrag(0.9)
        assertEquals(1.0, high.move(-1f, -0.3f), 1e-9)
    }

    // Finger geht unten herum von links nach rechts: kein Sprung von 120 auf 5
    @Test fun noJumpAroundTheBottom() {
        val d = MoonDrag(0.9)
        d.move(-1f, -0.2f)
        d.move(0f, -1f)
        d.move(1f, -0.2f)
        val (x, y) = at(10.0)
        assertEquals(1.0, d.move(x, y), 1e-9)
        // zurueck nach oben links greift wieder
        val (x2, y2) = at(160.0)
        assertEquals(160.0 / 180.0, d.move(x2, y2), 1e-6)
    }

    @Test fun repaintIntervalFromRadius() {
        // 120 min auf einem Bogen mit 400 px: rund 5,7 s pro Pixel
        val ms = Moon.msPerPixel(400f)
        assertTrue(ms in 5_600L..5_800L)
        assertEquals(30_000L, Moon.msPerPixel(0f))
    }
}

class TimeTextTest {
    @Test fun clock() {
        assertEquals("30:00", TimeText.clock(30 * Sleep.MINUTE))
        assertEquals("0:01", TimeText.clock(1))
        assertEquals("0:00", TimeText.clock(0))
        assertEquals("23:41", TimeText.clock(23 * Sleep.MINUTE + 40_500))
        assertEquals("2:00:00", TimeText.clock(120 * Sleep.MINUTE))
        assertEquals("1:05:09", TimeText.clock(65 * Sleep.MINUTE + 9_000))
        assertEquals("0:00", TimeText.clock(-500))
    }

    @Test fun parts() {
        assertEquals(TimeText.Parts(23, 41), TimeText.parts(23 * Sleep.MINUTE + 40_500))
        assertEquals(TimeText.Parts(30, 0), TimeText.parts(30 * Sleep.MINUTE))
        assertEquals(TimeText.Parts(120, 0), TimeText.parts(120 * Sleep.MINUTE))
        assertEquals(TimeText.Parts(0, 1), TimeText.parts(1))
        assertEquals(TimeText.Parts(0, 0), TimeText.parts(-5))
    }
}
