package solutions.xapient.levlr.domain

import solutions.xapient.levlr.domain.CalibrationSession.Problem
import solutions.xapient.levlr.domain.CalibrationSession.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CalibrationTest {
    // 1,5 s ruhige Messwerte einspeisen
    private fun CalibrationSession.measure(raw: Vec3, mode: LevelMode) {
        startMeasuring(mode)
        repeat(80) { offer(raw, 0.02f, mode) }
    }

    private fun rotate180(v: Vec3) = Vec3(-v.x, -v.y, v.z)

    @Test
    fun flatBiasOfOneDegreeIsRemoved() {
        val bias = Vec3(0.171f, -0.12f, 0f) // ~1° bzw. ~0,7°
        val surfaceUp = flatSample(0.8f, -0.4f) // schiefe Unterlage
        val s = CalibrationSession()
        s.measure(surfaceUp + bias, LevelMode.FLAT)
        assertEquals(Step.SECOND, s.step)
        s.measure(rotate180(surfaceUp) + bias, LevelMode.FLAT)
        assertEquals(Step.DONE, s.step)
        val b = s.result!!
        // nach Korrektur: Unterlage wird mit ihrer echten Neigung gemessen
        val corrected = surfaceUp + bias - b.asVec()
        assertEquals(0.8f, TiltMath.flatX(corrected), 0.05f)
        assertEquals(-0.4f, TiltMath.flatY(corrected), 0.05f)
    }

    @Test
    fun edgeBiasIsRemovedOnOppositeEdge() {
        val bias = Vec3(-0.17f, 0.05f, 0f)
        val s = CalibrationSession()
        val tilt = 0.6f
        s.measure(edgeSample(tilt) + bias, LevelMode.EDGE)
        s.measure(edgeSample(180f + tilt) + bias, LevelMode.EDGE)
        assertEquals(Step.DONE, s.step)
        val corrected = edgeSample(tilt) + bias - s.result!!.asVec()
        assertTrue(abs(TiltMath.edgeAngle(TiltMath.edgeRaw(corrected)) - tilt) < 0.05f)
    }

    @Test
    fun sameEdgeTwiceIsRejected() {
        val s = CalibrationSession()
        s.measure(edgeSample(0.5f), LevelMode.EDGE)
        s.measure(edgeSample(0.5f), LevelMode.EDGE)
        assertEquals(Step.SECOND, s.step)
        assertEquals(Problem.NOT_ROTATED, s.problem)
        assertNull(s.result)
    }

    @Test
    fun flatNotTurnedIsRejected() {
        val s = CalibrationSession()
        s.measure(flatSample(3f, 0f), LevelMode.FLAT)
        s.measure(flatSample(3f, 0f), LevelMode.FLAT)
        assertEquals(Problem.NOT_ROTATED, s.problem)
        assertEquals(Step.SECOND, s.step)
    }

    @Test
    fun implausibleBiasIsRejected() {
        val bias = Vec3(0.8f, 0f, 0f) // ~4,7°: kein Sensorfehler mehr
        val surface = flatSample(1f, 0f)
        val s = CalibrationSession()
        s.measure(surface + bias, LevelMode.FLAT)
        s.measure(rotate180(surface) + bias, LevelMode.FLAT)
        assertEquals(Problem.IMPLAUSIBLE, s.problem)
    }

    @Test
    fun levelSurfaceWithSmallBiasIsAccepted() {
        val bias = Vec3(0.2f, 0.1f, 0f)
        val s = CalibrationSession()
        s.measure(flatSample(0f, 0f) + bias, LevelMode.FLAT)
        s.measure(flatSample(0f, 0f) + bias, LevelMode.FLAT)
        assertEquals(Step.DONE, s.step)
    }

    @Test
    fun wrongModeWaits() {
        val s = CalibrationSession()
        s.measure(flatSample(0f, 0f), LevelMode.FLAT)
        s.startMeasuring(LevelMode.EDGE)
        s.offer(edgeSample(0f), 0.02f, LevelMode.EDGE)
        assertEquals(Problem.WRONG_MODE, s.problem)
        assertEquals(Step.SECOND, s.step)
    }

    @Test
    fun movementRestartsMeasurement() {
        val a = StableAverager(duration = 1f, tolerance = 0.15f)
        repeat(30) { a.offer(Vec3(0f, 0f, 9.81f), 0.02f) }
        val before = a.progress
        a.offer(Vec3(1f, 0f, 9.7f), 0.02f)
        assertTrue(before > 0.5f)
        assertTrue(a.progress < 0.1f)
    }

    @Test
    fun calibrationIsStoredPerMode() {
        val c = Calibration().with(LevelMode.EDGE, Bias(0.1f, 0f))
        assertTrue(c.flat.isZero)
        assertEquals(0.1f, c.biasFor(LevelMode.EDGE).x, 0f)
    }
}
