package solutions.xapient.magnr.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import solutions.xapient.magnr.domain.FieldListener
import solutions.xapient.magnr.domain.FieldSource
import kotlin.math.sqrt

// Kalibriertes Magnetometer plus Lage ohne Magnetometer (Game Rotation Vector) fuer den Rest-Versatz; Begruendung in SPEC.md
class AndroidFieldSource(private val manager: SensorManager?) : FieldSource {
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotation: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private var listener: SensorEventListener? = null
    private var status = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    override val isAvailable: Boolean get() = sensor != null
    override val maxRange: Float get() = sensor?.maximumRange ?: 0f

    override fun start(listener: FieldListener) {
        val s = sensor ?: return
        stop()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val v = event.values
                if (v.size < 3) return
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    // values = x, y, z, (w)
                    val w = if (v.size >= 4) v[3] else sqrt((1f - v[0] * v[0] - v[1] * v[1] - v[2] * v[2]).coerceAtLeast(0f))
                    listener.onRotation(w, v[0], v[1], v[2], event.timestamp)
                    return
                }
                status = event.accuracy
                listener.onField(v[0], v[1], v[2], status, event.timestamp)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) status = accuracy
            }
        }
        this.listener = l
        // 50 Hz: flott genug fuer Ton und Ausschlag (SENSOR_DELAY_GAME)
        manager?.registerListener(l, s, SAMPLING_US)
        rotation?.let { manager?.registerListener(l, it, SAMPLING_US) }
    }

    override fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
    }

    companion object {
        const val SAMPLING_US = 20_000
    }
}
