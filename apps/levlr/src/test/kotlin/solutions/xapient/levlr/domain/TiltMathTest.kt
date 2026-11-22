package solutions.xapient.levlr.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val G = 9.81f
private fun rad(deg: Float) = (deg * PI / 180.0)

// Messwert, wenn das Geraet aufrecht steht und um deg im Uhrzeigersinn gedreht ist
internal fun edgeSample(deg: Float) = Vec3((-sin(rad(deg)) * G).toFloat(), (cos(rad(deg)) * G).toFloat(), 0f)

// Messwert flach liegend: rechts um ax angehoben, oben um ay angehoben (kleine Winkel)
internal fun flatSample(ax: Float, ay: Float) =
    Vec3((sin(rad(ax)) * G).toFloat(), (sin(rad(ay)) * G).toFloat(), (cos(rad(ax)) * cos(rad(ay)) * G).toFloat())

class TiltMathTest {
    @Test
    fun edgeAngleIsClockwiseRotation() {
        assertEquals(0f, TiltMath.edgeAngle(TiltMath.edgeRaw(edgeSample(0f))), 1e-4f)
        assertEquals(3.4f, TiltMath.edgeAngle(TiltMath.edgeRaw(edgeSample(3.4f))), 1e-3f)
        assertEquals(-12f, TiltMath.edgeAngle(TiltMath.edgeRaw(edgeSample(-12f))), 1e-3f)
    }

    @Test
    fun edgeAngleFoldsToNearestEdge() {
        val raw = TiltMath.edgeRaw(edgeSample(92f))
        assertEquals(2f, TiltMath.edgeAngle(raw), 1e-3f)
        assertEquals(1, TiltMath.edgeQuadrant(raw))
        assertEquals(3, TiltMath.edgeQuadrant(TiltMath.edgeRaw(edgeSample(-88f))))
        assertEquals(2, TiltMath.edgeQuadrant(TiltMath.edgeRaw(edgeSample(179f))))
    }

    @Test
    fun quadrantHasHysteresis() {
        val raw = TiltMath.edgeRaw(edgeSample(47f))
        assertEquals(1, TiltMath.edgeQuadrant(raw))
        assertEquals(0, TiltMath.edgeQuadrant(raw, previous = 0))
        assertEquals(47f, TiltMath.edgeAngle(raw, 0), 1e-3f)
        assertEquals(1, TiltMath.edgeQuadrant(TiltMath.edgeRaw(edgeSample(52f)), previous = 0))
        assertEquals(-2f, TiltMath.edgeAngle(TiltMath.edgeRaw(edgeSample(178f)), 2), 1e-3f)
    }

    @Test
    fun screenRemapKeepsLevelOnLongEdge() {
        // ROTATION_90 = Geraet 90° gegen den Uhrzeigersinn, dazu 1,5° im Uhrzeigersinn
        for ((turns, base) in listOf(1 to -90f, 3 to 90f, 2 to 180f)) {
            val raw = TiltMath.edgeRaw(TiltMath.toScreen(edgeSample(base + 1.5f), turns))
            assertEquals(0, TiltMath.edgeQuadrant(raw))
            assertEquals(1.5f, TiltMath.edgeAngle(raw), 1e-3f)
        }
    }

    @Test
    fun remapRoundTrip() {
        val v = Vec3(1f, 2f, 3f)
        var w = v
        repeat(4) { w = TiltMath.toScreen(w, 1) }
        assertEquals(v, w)
        assertEquals(Vec3(-1f, -2f, 3f), TiltMath.toScreen(v, 2))
    }

    @Test
    fun flatAxes() {
        val s = flatSample(2f, -1f)
        assertEquals(2f, TiltMath.flatX(s), 0.02f)
        assertEquals(-1f, TiltMath.flatY(s), 0.02f)
        assertEquals(0f, TiltMath.inclination(Vec3(0f, 0f, G)), 1e-4f)
        assertEquals(90f, TiltMath.inclination(Vec3(0f, G, 0f)), 1e-3f)
    }

    @Test
    fun units() {
        assertEquals(45f, TiltMath.convert(45f, AngleUnit.DEGREE), 0f)
        assertEquals(100f, TiltMath.convert(45f, AngleUnit.PERCENT), 1e-3f)
        assertEquals(17.455f, TiltMath.convert(1f, AngleUnit.MM_PER_M), 1e-3f)
    }
}
