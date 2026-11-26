package solutions.xapient.levlr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
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

// Ausrichtung der Oberflaeche; 180°-Drehungen aendern die Groesse nicht, daher leicht abfragen statt nur bei Groessenwechsel
@Composable
internal actual fun DisplayRotationEffect(onRotation: (Int) -> Unit) {
    val vc = LocalUIViewController.current
    val callback = rememberUpdatedState(onRotation)
    val update = { callback.value(quarterTurns(vc.view.window?.windowScene?.interfaceOrientation)) }
    // sofort bei Geraetedrehung (die Oberflaeche dreht kurz danach, deshalb auch verzoegert nachlesen)
    DisposableEffect(vc) {
        val device = UIDevice.currentDevice
        device.beginGeneratingDeviceOrientationNotifications()
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            UIDeviceOrientationDidChangeNotification, null, NSOperationQueue.mainQueue,
        ) { _ -> update() }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            device.endGeneratingDeviceOrientationNotifications()
        }
    }
    // Rueckfallebene, faengt auch die verzoegerte Drehung der Oberflaeche ab
    LaunchedEffect(vc) {
        while (true) {
            update()
            delay(250)
        }
    }
}

// Wie Android Surface.ROTATION_*: LandscapeRight = Geraet gegen den Uhrzeigersinn gedreht (Oberkante links)
private fun quarterTurns(o: UIInterfaceOrientation?): Int = when (o) {
    UIInterfaceOrientationLandscapeRight -> 1
    UIInterfaceOrientationPortraitUpsideDown -> 2
    UIInterfaceOrientationLandscapeLeft -> 3
    else -> 0
}

@Composable
internal actual fun BackEffect(onBack: () -> Unit) = Unit

@Composable
internal actual fun decimalSeparator(): Char = NSNumberFormatter().decimalSeparator?.firstOrNull() ?: '.'
