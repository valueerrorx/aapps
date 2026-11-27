package solutions.xapient.nordr.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class OrientationTest {
    private fun assertHeading(expected: Float, actual: Float, eps: Float = 0.05f) =
        assertTrue(abs(Angles.delta(expected, actual)) < eps, "erwartet $expected, war $actual")

    @Test
    fun flatPhoneShowsTopEdgeDirection() {
        for (h in listOf(0f, 45f, 90f, 180f, 247f, 359f)) {
            assertHeading(h, Orientation.heading(Poses.pose(h), 0))
        }
    }

    @Test
    fun uprightPhoneShowsDirectionOfBack() {
        for (h in listOf(0f, 90f, 200f)) {
            assertHeading(h, Orientation.heading(Poses.pose(h, pitch = 90f), 0))
        }
    }

    @Test
    fun noJumpBetweenFlatAndUpright() {
        for (pitch in 0..90 step 5) {
            assertHeading(123f, Orientation.heading(Poses.pose(123f, pitch.toFloat()), 0))
        }
    }

    @Test
    fun sidewaysTiltKeepsDirection() {
        // flach und aufrecht: seitlich kippen aendert die Blickrichtung nicht
        for (pitch in listOf(0f, 30f, 90f)) {
            for (roll in listOf(-60f, -30f, -10f, 10f, 30f, 60f)) {
                for (h in listOf(0f, 123f, 247f)) {
                    assertHeading(h, Orientation.heading(Poses.pose(h, pitch, roll), 0))
                }
            }
        }
    }

    @Test
    fun noFlipBeyondUprightOrFacingDown() {
        // frueher 180°-Sprung bei Pitch -45° und 135°
        for (pitch in -60..120 step 5) {
            assertHeading(77f, Orientation.heading(Poses.pose(77f, pitch.toFloat()), 0))
            assertHeading(77f, Orientation.heading(Poses.pose(77f, pitch.toFloat(), 20f), 0))
        }
    }

    @Test
    fun uprightLandscapeUsesBack() {
        for (h in listOf(0f, 90f, 200f)) {
            // Oberkante links = Rotation 1, Oberkante rechts = Rotation 3
            assertHeading(h, Orientation.heading(Poses.pose(h, 90f, -90f), 1))
            assertHeading(h, Orientation.heading(Poses.pose(h, 90f, 90f), 3))
            assertHeading(h, Orientation.heading(Poses.pose(h, 90f, -75f), 1))
        }
    }

    @Test
    fun screenRightVerticalIsUndetermined() {
        assertTrue(Orientation.heading(Poses.pose(40f, 90f, 90f), 0).isNaN())
        assertTrue(Orientation.heading(Poses.pose(40f, 0f, -88f), 0).isNaN())
    }

    @Test
    fun landscapeUsesScreenTop() {
        // quer gedreht (Oberkante links): Bildschirm-oben ist die rechte Geraetekante
        assertHeading(0f, Orientation.heading(Poses.pose(270f), 1))
        assertHeading(180f, Orientation.heading(Poses.pose(0f), 2))
        assertHeading(0f, Orientation.heading(Poses.pose(90f), 3))
    }

    @Test
    fun rotationVectorAboutVerticalAxis() {
        for (h in listOf(0f, 30f, 135f, 300f)) {
            val half = toRadians(-h / 2.0)
            val q = floatArrayOf(0f, 0f, sin(half).toFloat(), cos(half).toFloat())
            assertHeading(h, Orientation.heading(Orientation.fromRotationVector(q), 0))
        }
    }

    @Test
    fun rotationVectorWithoutScalarPart() {
        val half = toRadians(-40.0 / 2)
        val q = floatArrayOf(0f, 0f, sin(half).toFloat())
        assertHeading(40f, Orientation.heading(Orientation.fromRotationVector(q), 0))
    }

    @Test
    fun gravityAndMagneticMatchesPose() {
        for ((h, pitch, roll) in listOf(Triple(0f, 0f, 0f), Triple(77f, 20f, 15f), Triple(250f, 60f, -40f), Triple(320f, 90f, 0f))) {
            val r = Poses.pose(h, pitch, roll)
            val m = Orientation.fromGravityMagnetic(Poses.gravity(r), Poses.magnetic(r))!!
            for (i in 0 until 9) assertEquals(r[i], m[i], 1e-4f, "Element $i bei $h/$pitch")
        }
    }

    @Test
    fun rotationVectorAndFallbackAgree() {
        // beliebige Lage als Quaternion, dieselbe Lage ueber Schwerkraft + Magnetfeld
        val n = Vec3(0.3f, -0.5f, 0.8f).let { it * (1f / it.length) }
        val a = toRadians(70.0)
        val q = floatArrayOf(n.x * sin(a / 2).toFloat(), n.y * sin(a / 2).toFloat(), n.z * sin(a / 2).toFloat(), cos(a / 2).toFloat())
        val r = Orientation.fromRotationVector(q)
        val f = Orientation.fromGravityMagnetic(Poses.gravity(r), Poses.magnetic(r))!!
        assertHeading(Orientation.heading(r, 0), Orientation.heading(f, 0), 0.01f)
    }

    @Test
    fun degenerateFieldGivesNull() {
        assertNull(Orientation.fromGravityMagnetic(Vec3(0f, 0f, 9.81f), Vec3(0f, 0f, -40f)))
        assertNull(Orientation.fromGravityMagnetic(Vec3.ZERO, Vec3(0f, 20f, -40f)))
    }

    @Test
    fun upOnScreenFollowsTilt() {
        val flat = Orientation.upOnScreen(Poses.pose(10f), 0)
        assertEquals(1f, flat.z, 1e-4f)
        val upright = Orientation.upOnScreen(Poses.pose(10f, pitch = 90f), 0)
        assertEquals(1f, upright.y, 1e-4f)
        val land = Orientation.upOnScreen(Poses.pose(10f, pitch = 90f), 1)
        assertEquals(-1f, land.x, 1e-4f)
    }

    @Test
    fun fusionLowPassesAccelerometer() {
        val fusion = GravityMagFusion()
        val r = Poses.pose(60f)
        assertNull(fusion.onMagnetic(Poses.magnetic(r)))
        var t = 0L
        repeat(100) { i ->
            // Ruetteln ueberlagert die Schwerkraft
            val shake = if (i % 2 == 0) Vec3(1.5f, -1f, 0f) else Vec3(-1.5f, 1f, 0f)
            fusion.onAccelerometer(Poses.gravity(r) + shake, t)
            t += 20_000_000L
        }
        val m = fusion.onMagnetic(Poses.magnetic(r))!!
        assertHeading(60f, Orientation.heading(m, 0), 2f)
    }
}

private fun toRadians(deg: Double): Double = deg * kotlin.math.PI / 180.0
