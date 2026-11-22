package com.valueerrorx.nordr.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.valueerrorx.nordr.domain.Angles
import com.valueerrorx.nordr.domain.CompassListener
import com.valueerrorx.nordr.domain.Orientation
import com.valueerrorx.nordr.domain.Poses
import com.valueerrorx.nordr.domain.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowSensor
import kotlin.math.abs

// Verdrahtung mit und ohne Rotation-Vector
@RunWith(RobolectricTestRunner::class)
class AndroidSensorSourceTest {
    private val manager = RuntimeEnvironment.getApplication().getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private fun add(type: Int): Sensor = ShadowSensor.newInstance(type).also { shadowOf(manager).addSensor(it) }

    private class Recorder : CompassListener {
        val rotations = mutableListOf<FloatArray>()
        var magnetics = 0
        override fun onRotation(r: FloatArray, timestampNanos: Long) {
            rotations += r
        }
        override fun onMagnetic(m: Vec3, status: Int, timestampNanos: Long) {
            magnetics++
        }
    }

    private fun send(sensor: Sensor, values: FloatArray, t: Long) = shadowOf(manager).sendSensorEventToListeners(
        SensorEventBuilder.newBuilder().setSensor(sensor).setValues(values).setTimestamp(t).setAccuracy(3).build(),
    )

    private fun Vec3.array() = floatArrayOf(x, y, z)

    @Test
    fun rotationVectorPreferred() {
        val rotation = add(Sensor.TYPE_ROTATION_VECTOR)
        val magnetic = add(Sensor.TYPE_MAGNETIC_FIELD)
        val accel = add(Sensor.TYPE_ACCELEROMETER)
        val source = AndroidSensorSource(manager)
        val rec = Recorder()
        source.start(rec)
        val listener = shadowOf(manager).listeners.single()
        assertTrue(shadowOf(manager).hasListener(listener, rotation))
        assertTrue(shadowOf(manager).hasListener(listener, magnetic))
        assertFalse(shadowOf(manager).hasListener(listener, accel))

        send(rotation, floatArrayOf(0f, 0f, 0f, 1f), 1L)
        send(magnetic, Poses.magnetic(Poses.pose(0f)).array(), 2L)
        assertEquals(1, rec.rotations.size)
        assertEquals(1, rec.magnetics)

        source.stop()
        assertTrue(shadowOf(manager).listeners.isEmpty())
    }

    @Test
    fun fallbackUsesAccelerometerAndMagnetometer() {
        val magnetic = add(Sensor.TYPE_MAGNETIC_FIELD)
        val accel = add(Sensor.TYPE_ACCELEROMETER)
        val source = AndroidSensorSource(manager)
        assertTrue(source.isAvailable)
        val rec = Recorder()
        source.start(rec)
        val listener = shadowOf(manager).listeners.single()
        assertTrue(shadowOf(manager).hasListener(listener, accel))
        assertTrue(shadowOf(manager).hasListener(listener, magnetic))

        val r = Poses.pose(60f)
        send(accel, Poses.gravity(r).array(), 1L)
        send(magnetic, Poses.magnetic(r).array(), 2L)
        val heading = Orientation.heading(rec.rotations.single(), 0)
        assertTrue("war $heading", abs(Angles.delta(60f, heading)) < 0.1f)
        source.stop()
    }

    @Test
    fun withoutMagnetometerNothingStarts() {
        add(Sensor.TYPE_ROTATION_VECTOR)
        val source = AndroidSensorSource(manager)
        assertFalse(source.isAvailable)
        source.start(Recorder())
        assertTrue(shadowOf(manager).listeners.isEmpty())
    }
}
