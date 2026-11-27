package solutions.xapient.nordr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMAttitudeReferenceFrameXMagneticNorthZVertical
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracy
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracyHigh
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracyLow
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracyMedium
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import solutions.xapient.nordr.domain.CompassListener
import solutions.xapient.nordr.domain.SensorSource
import solutions.xapient.nordr.domain.Vec3

// CoreMotion Device Motion gegen magnetisch Nord: fusionierte Lage (Gyro, Magnetometer, Schwerkraft) wie der
// Android-Rotation-Vector, dazu das kalibrierte Feld fuer Genauigkeit und Stoerung. Ohne Standort, ohne Berechtigung
@OptIn(ExperimentalForeignApi::class)
class IosSensorSource : SensorSource {
    private val manager = CMMotionManager().apply {
        // eigene Kalibrierhilfe statt der von iOS
        showsDeviceMovementDisplay = false
    }

    override val isAvailable: Boolean
        get() = manager.deviceMotionAvailable && manager.magnetometerAvailable &&
            (CMMotionManager.availableAttitudeReferenceFrames() and CMAttitudeReferenceFrameXMagneticNorthZVertical) != 0uL

    override fun start(listener: CompassListener) {
        if (!isAvailable) return
        stop()
        // CoreMotion rundet auf 10-ms-Schritte auf; 1/60 s ergibt nur ~33 Hz
        manager.deviceMotionUpdateInterval = 1.0 / 100
        manager.startDeviceMotionUpdatesUsingReferenceFrame(
            CMAttitudeReferenceFrameXMagneticNorthZVertical,
            NSOperationQueue.mainQueue,
        ) { motion, _ ->
            if (motion != null) {
                val t = (motion.timestamp * 1e9).toLong()
                // Matrix bildet den Bezug (X Nord, Y West, Z oben) aufs Geraet ab: Spalten = Bezugsachsen im Geraet.
                // Android-Zeilen = Ost, Nord, Oben, also -Y, X, Z
                val r = motion.attitude.rotationMatrix.useContents {
                    floatArrayOf(
                        -m12.toFloat(), -m22.toFloat(), -m32.toFloat(),
                        m11.toFloat(), m21.toFloat(), m31.toFloat(),
                        m13.toFloat(), m23.toFloat(), m33.toFloat(),
                    )
                }
                listener.onRotation(r, t)
                motion.magneticField.useContents {
                    val m = Vec3(field.x.toFloat(), field.y.toFloat(), field.z.toFloat())
                    // vor der ersten Kalibrierung liefert iOS teils kein Feld (0): dann nichts melden
                    if (m.length > 1f) listener.onMagnetic(m, status(accuracy), t)
                }
            }
        }
    }

    override fun stop() {
        manager.stopDeviceMotionUpdates()
    }

    // wie Android SENSOR_STATUS_*: 0 unzuverlaessig .. 3 hoch
    private fun status(a: CMMagneticFieldCalibrationAccuracy): Int = when (a) {
        CMMagneticFieldCalibrationAccuracyHigh -> 3
        CMMagneticFieldCalibrationAccuracyMedium -> 2
        CMMagneticFieldCalibrationAccuracyLow -> 1
        else -> 0
    }
}
