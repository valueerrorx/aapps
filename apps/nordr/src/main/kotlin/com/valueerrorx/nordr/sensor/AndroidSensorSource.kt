package com.valueerrorx.nordr.sensor

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.valueerrorx.nordr.domain.CompassListener
import com.valueerrorx.nordr.domain.DeclinationModel
import com.valueerrorx.nordr.domain.GravityMagFusion
import com.valueerrorx.nordr.domain.Orientation
import com.valueerrorx.nordr.domain.SensorSource
import com.valueerrorx.nordr.domain.Vec3

// Rotation-Vector bevorzugt; sonst Beschleunigung + Magnetfeld. Magnetometer immer fuer die Genauigkeit
class AndroidSensorSource(private val manager: SensorManager?) : SensorSource {
    private val rotation: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magnetic: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accel: Sensor? = if (rotation == null) manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) else null
    private val fusion = GravityMagFusion()
    private var listener: SensorEventListener? = null

    override val isAvailable: Boolean get() = magnetic != null && (rotation != null || accel != null)

    override fun start(listener: CompassListener) {
        if (!isAvailable) return
        stop()
        fusion.reset()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val v = event.values
                if (v.size < 3) return
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR ->
                        listener.onRotation(Orientation.fromRotationVector(v.copyOf(minOf(v.size, 4))), event.timestamp)
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val m = Vec3(v[0], v[1], v[2])
                        listener.onMagnetic(m, event.accuracy, event.timestamp)
                        if (rotation == null) fusion.onMagnetic(m)?.let { listener.onRotation(it, event.timestamp) }
                    }
                    Sensor.TYPE_ACCELEROMETER -> fusion.onAccelerometer(Vec3(v[0], v[1], v[2]), event.timestamp)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        this.listener = l
        listOfNotNull(rotation, magnetic, accel).forEach {
            manager?.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
    }
}

// Deklination aus dem eingebauten Weltmagnetfeld-Modell, ohne Standort-Berechtigung
object AndroidDeclination : DeclinationModel {
    override fun declination(lat: Float, lon: Float, timeMillis: Long): Float =
        GeomagneticField(lat, lon, 0f, timeMillis).declination
}
