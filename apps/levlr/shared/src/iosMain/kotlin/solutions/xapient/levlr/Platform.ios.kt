package solutions.xapient.levlr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import solutions.xapient.levlr.domain.SensorSource
import solutions.xapient.levlr.domain.Vec3

private const val G = 9.80665f

// CoreMotion: Device Motion (fusionierte Schwerkraft) bevorzugt, sonst Beschleunigungssensor; keine Berechtigung noetig
@OptIn(ExperimentalForeignApi::class)
class IosSensorSource : SensorSource {
    private val manager = CMMotionManager()

    override val isAvailable: Boolean
        get() = manager.deviceMotionAvailable || manager.accelerometerAvailable

    // iOS misst in g mit Schwerkraft nach unten, Android in m/s² nach oben: umdrehen und skalieren
    private fun up(x: Double, y: Double, z: Double) = Vec3((-x * G).toFloat(), (-y * G).toFloat(), (-z * G).toFloat())

    override fun start(listener: (Vec3, Long) -> Unit) {
        stop()
        val queue = NSOperationQueue.mainQueue
        if (manager.deviceMotionAvailable) {
            manager.deviceMotionUpdateInterval = 1.0 / 60.0
            manager.startDeviceMotionUpdatesToQueue(queue) { motion, _ ->
                if (motion != null) {
                    val v = motion.gravity.useContents { up(x, y, z) }
                    listener(v, (motion.timestamp * 1e9).toLong())
                }
            }
        } else if (manager.accelerometerAvailable) {
            manager.accelerometerUpdateInterval = 1.0 / 60.0
            manager.startAccelerometerUpdatesToQueue(queue) { data, _ ->
                if (data != null) {
                    val v = data.acceleration.useContents { up(x, y, z) }
                    listener(v, (data.timestamp * 1e9).toLong())
                }
            }
        }
    }

    override fun stop() {
        manager.stopDeviceMotionUpdates()
        manager.stopAccelerometerUpdates()
    }
}
