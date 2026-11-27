package solutions.xapient.aapps.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.coroutines.delay
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown

// Ausrichtung der Oberflaeche; 180°-Drehungen aendern die Groesse nicht, daher leicht abfragen statt nur bei Groessenwechsel
@Composable
actual fun DisplayRotationEffect(onRotation: (Int) -> Unit) {
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
actual fun BackEffect(onBack: () -> Unit) = Unit

@Composable
actual fun decimalSeparator(): Char = NSNumberFormatter().decimalSeparator?.firstOrNull() ?: '.'
