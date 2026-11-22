package solutions.xapient.magnr.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import solutions.xapient.magnr.domain.FieldListener
import solutions.xapient.magnr.domain.FieldSource

// Kalibriertes Magnetometer (Hart-Eisen-Versatz abgezogen, Betrag lageunabhaengig); Begruendung in SPEC.md
class AndroidFieldSource(private val manager: SensorManager?) : FieldSource {
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
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
                status = event.accuracy
                listener.onField(v[0], v[1], v[2], status, event.timestamp)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                status = accuracy
            }
        }
        this.listener = l
        // 50 Hz: flott genug fuer Ton und Ausschlag (SENSOR_DELAY_GAME)
        manager?.registerListener(l, s, SAMPLING_US)
    }

    override fun stop() {
        listener?.let { manager?.unregisterListener(it) }
        listener = null
    }

    companion object {
        const val SAMPLING_US = 20_000
    }
}
