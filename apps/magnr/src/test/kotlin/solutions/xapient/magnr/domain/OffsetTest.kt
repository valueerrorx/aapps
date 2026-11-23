package solutions.xapient.magnr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class OffsetTest {
    // Erdfeld in Weltkoordinaten (Ost, Nord, oben), ~49 µT wie in Mitteleuropa
    private val earth = floatArrayOf(0f, 21f, -44f)
    // Rest-Versatz nach der Systemkalibrierung, ~15 µT
    private val residual = floatArrayOf(12f, -8f, 6f)
    private val rnd = Random(7)
    private val ms = 1_000_000L

    private fun quat(ax: Float, ay: Float, az: Float, deg: Double): FloatArray {
        val h = Math.toRadians(deg) / 2
        val s = sin(h).toFloat()
        return floatArrayOf(cos(h).toFloat(), ax * s, ay * s, az * s)
    }

    // Kippen um x, dann um y, je 0 -> 80° -> 0 in 3 s
    private fun tilt(sec: Double): FloatArray {
        val cycle = (sec / 3).toInt()
        val deg = 80 * sin(Math.PI * (sec % 3) / 3)
        return if (cycle % 2 == 0) quat(1f, 0f, 0f, deg) else quat(0f, 1f, 0f, deg)
    }

    // Weltvektor in Handy-Koordinaten (R^T v)
    private fun toDevice(q: FloatArray, v: FloatArray): FloatArray {
        val r = rotationMatrix(q[0], q[1], q[2], q[3])
        return FloatArray(3) { i -> (r[i] * v[0] + r[3 + i] * v[1] + r[6 + i] * v[2]).toFloat() }
    }

    private fun noise() = (rnd.nextFloat() - 0.5f) * 1.4f

    /**
     * Simuliert 50 Hz Magnetometer plus Lage (9 ms versetzt) und liefert den Ausschlag je Wert.
     * metal: zusaetzliches Weltfeld ab Sekunde metalFrom; rotation=false = Geraet ohne Lage.
     */
    private fun run(
        meter: FieldMeter,
        seconds: Double,
        start: Double = 0.0,
        motion: (Double) -> FloatArray = ::tilt,
        metal: FloatArray? = null,
        rotation: Boolean = true,
    ): List<Reading> {
        val out = ArrayList<Reading>()
        var sec = start
        while (sec < start + seconds) {
            val t = (10_000 + sec * 1000).toLong() * ms
            if (rotation) {
                val tr = sec - 0.011
                val qr = motion(tr)
                meter.onRotation(qr[0], qr[1], qr[2], qr[3], t - 11 * ms)
            }
            val q = motion(sec)
            val world = if (metal != null) FloatArray(3) { earth[it] + metal[it] } else earth
            val b = toDevice(q, world)
            out += meter.onSample(b[0] + residual[0] + noise(), b[1] + residual[1] + noise(), b[2] + residual[2] + noise(), 2000f, 3, t)
            sec += 0.02
        }
        return out
    }

    private val still: (Double) -> FloatArray = { quat(1f, 0f, 0f, 0.0) }

    @Test fun rotationMatrixMapsDeviceToWorld() {
        // 90° um z: Handy-x zeigt nach Welt-y
        val r = rotationMatrix(cos(Math.PI / 4).toFloat(), 0f, 0f, sin(Math.PI / 4).toFloat())
        assertEquals(0.0, r[0], 1e-6)
        assertEquals(1.0, r[3], 1e-6)
    }

    @Test fun trackInterpolatesAndIgnoresSign() {
        val track = RotationTrack()
        val a = quat(1f, 0f, 0f, 0.0)
        val b = quat(1f, 0f, 0f, 20.0)
        track.add(a[0], a[1], a[2], a[3], 0L)
        // -q ist dieselbe Lage
        track.add(-b[0], -b[1], -b[2], -b[3], 20 * ms)
        val mid = track.at(10 * ms)!!
        val q = quat(1f, 0f, 0f, 10.0)
        val want = rotationMatrix(q[0], q[1], q[2], q[3])
        for (i in 0..8) assertEquals(want[i], mid[i], 1e-3)
        assertNull(track.at(500 * ms))
    }

    @Test fun estimatorLearnsResidualFromTilting() {
        val est = OffsetEstimator()
        val track = RotationTrack()
        var sec = 0.0
        while (sec < 9.0) {
            val t = (sec * 1000).toLong() * ms
            val q = tilt(sec)
            track.add(q[0], q[1], q[2], q[3], t)
            val b = toDevice(q, earth)
            est.add(b[0] + residual[0] + noise(), b[1] + residual[1] + noise(), b[2] + residual[2] + noise(), track.at(t)!!, t)
            sec += 0.02
        }
        for (i in 0..2) assertEquals(residual[i], est.offset[i], 1.5f)
    }

    @Test fun shiftingWithoutTurningGivesNoEquation() {
        val est = OffsetEstimator()
        val r = rotationMatrix(1f, 0f, 0f, 0f)
        // Metall beim Ueberstreichen: Feld aendert sich stark, Lage nicht
        for (i in 0 until 200) assertTrue(!est.add(0f, 21f + i * 0.3f, -44f, r, i * 20 * ms))
        assertEquals(0f, est.offset[0], 0f)
    }

    @Test fun tiltingWithoutMetalStaysQuiet() {
        val meter = FieldMeter()
        // erster Zyklus je Achse lernt, danach ruhig
        run(meter, 6.0)
        val after = run(meter, 12.0, start = 6.0)
        val worst = after.maxOf { abs(it.dev) }
        assertTrue("max |dev| $worst", after.all { it.level == Level.QUIET })
        assertTrue("max |dev| $worst", worst < 3f)
    }

    @Test fun withoutRotationTiltingLooksLikeAFind() {
        // Fallback ohne Lage = altes Verhalten; zeigt das Problem, das die Lage loest
        val meter = FieldMeter()
        val r = run(meter, 12.0, rotation = false)
        assertTrue(r.any { it.level >= Level.WEAK })
    }

    @Test fun metalStillShowsAfterLearning() {
        val meter = FieldMeter()
        run(meter, 12.0)
        val quiet = run(meter, 2.0, start = 12.0, motion = still)
        assertEquals(Level.QUIET, quiet.last().level)
        // Schraube in der Naehe: +20 µT entlang des Erdfelds
        val near = run(meter, 1.0, start = 14.0, motion = still, metal = floatArrayOf(0f, 8.6f, -18f))
        assertEquals(Level.NEAR, near.last().level)
        assertEquals(20f, near.last().dev, 1.5f)
    }

    @Test fun offsetSurvivesSaveAndRestore() {
        val meter = FieldMeter()
        run(meter, 12.0)
        meter.pause(100f)
        val g = FieldMeter().apply { restore(meter.save()) }
        g.resume(110f)
        val after = run(g, 6.0, start = 20.0)
        assertTrue(after.all { it.level == Level.QUIET })
    }
}
