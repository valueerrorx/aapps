package solutions.xapient.magnr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMAttitudeReferenceFrameXArbitraryCorrectedZVertical
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracy
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracyHigh
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracyLow
import platform.CoreMotion.CMMagneticFieldCalibrationAccuracyMedium
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import solutions.xapient.magnr.domain.FieldListener
import solutions.xapient.magnr.domain.FieldSource
import solutions.xapient.magnr.domain.quaternionOf

// CoreMotion Device Motion: kalibriertes Magnetfeld (wie Android TYPE_MAGNETIC_FIELD) und Lage fuer den Rest-Versatz.
// Bezug "beliebig, korrigiert": nur dann kalibriert iOS das Feld; die Lage zaehlt nur relativ (Drehungen in 1 s).
// Ohne Standort, ohne Berechtigung
@OptIn(ExperimentalForeignApi::class)
class IosFieldSource : FieldSource {
    private val manager = CMMotionManager().apply {
        // eigene Kalibrierhilfe statt der von iOS
        showsDeviceMovementDisplay = false
    }

    override val isAvailable: Boolean
        get() = manager.deviceMotionAvailable && manager.magnetometerAvailable &&
            (CMMotionManager.availableAttitudeReferenceFrames() and CMAttitudeReferenceFrameXArbitraryCorrectedZVertical) != 0uL

    // iOS nennt keinen Messbereich: Anschlag nur ueber die klebende Achse
    override val maxRange: Float = 0f

    override fun start(listener: FieldListener) {
        if (!isAvailable) return
        stop()
        // 50 Hz wie Android
        manager.deviceMotionUpdateInterval = 1.0 / 50
        manager.startDeviceMotionUpdatesUsingReferenceFrame(
            CMAttitudeReferenceFrameXArbitraryCorrectedZVertical,
            NSOperationQueue.mainQueue,
        ) { motion, _ ->
            if (motion != null) {
                val t = (motion.timestamp * 1e9).toLong()
                // CoreMotion-Matrix bildet den Bezug aufs Geraet ab; transponiert = Handy -> Welt wie Android
                val r = motion.attitude.rotationMatrix.useContents {
                    doubleArrayOf(m11, m21, m31, m12, m22, m32, m13, m23, m33)
                }
                val q = quaternionOf(r)
                listener.onRotation(q[0], q[1], q[2], q[3], t)
                motion.magneticField.useContents {
                    val x = field.x.toFloat()
                    val y = field.y.toFloat()
                    val z = field.z.toFloat()
                    // vor der ersten Kalibrierung liefert iOS teils kein Feld (0): dann nichts melden
                    if (x * x + y * y + z * z > 1f) listener.onField(x, y, z, status(accuracy), t)
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
