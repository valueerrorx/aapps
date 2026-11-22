package solutions.xapient.levlr.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import solutions.xapient.levlr.domain.SensorSource
import solutions.xapient.levlr.domain.Vec3

// Gravity-Sensor bevorzugt (ruhiger), sonst Beschleunigungssensor
class AndroidSensorSource(private val manager: SensorManager?) : SensorSource {
    private val sensor: Sensor? =
        manager?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var listener: SensorEventListener? = null

    override val isAvailable: Boolean get() = sensor != null

    override fun start(listener: (Vec3, Long) -> Unit) {
        val s = sensor ?: return
        stop()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val v = event.values
                if (v.size >= 3) listener(Vec3(v[0], v[1], v[2]), event.timestamp)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        this.listener = l
        manager?.registerListener(l, s, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
    }
}
