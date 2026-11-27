package solutions.xapient.pulsr.domain

import kotlin.test.assertEquals
import kotlin.test.Test

class WheelTest {
    @Test fun angleScreenCoordinates() {
        assertEquals(0f, Wheel.angle(10f, 0f, 0f, 0f), 1e-4f)
        // y nach unten: unten = +90
        assertEquals(90f, Wheel.angle(0f, 10f, 0f, 0f), 1e-4f)
        assertEquals(-90f, Wheel.angle(0f, -10f, 0f, 0f), 1e-4f)
    }

    @Test fun deltaShortestWay() {
        assertEquals(20f, Wheel.delta(170f, -170f), 1e-4f)
        assertEquals(-20f, Wheel.delta(-170f, 170f), 1e-4f)
        assertEquals(30f, Wheel.delta(0f, 30f), 1e-4f)
    }

    @Test fun stepsKeepRemainder() {
        val s = WheelSteps(6f)
        assertEquals(0, s.add(4f))
        assertEquals(1, s.add(4f))
        assertEquals(0, s.add(3.9f))
        assertEquals(2, s.add(12f))
        assertEquals(-1, s.add(-12f))
    }

    @Test fun fullTurnIsSixtyBpm() {
        val s = WheelSteps()
        var sum = 0
        repeat(72) { sum += s.add(5f) }
        assertEquals(60, sum)
    }
}
