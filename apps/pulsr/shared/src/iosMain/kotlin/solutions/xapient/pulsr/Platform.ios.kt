package solutions.xapient.pulsr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIScreen
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

// CACurrentMediaTime = mach_absolute_time in Sekunden: gleiche Basis wie AVAudioTime.hostTime
actual fun uptimeMillis(): Long = (CACurrentMediaTime() * 1_000.0).toLong()

fun mediaNanos(): Long = (CACurrentMediaTime() * 1e9).toLong()

// Bildzeit von Compose hat auf iOS keine zugesicherte Basis: jetzt nehmen (Abstand zum Bild < 1 Frame)
actual fun presentNanos(frameNanos: Long): Long = mediaNanos()

@Composable
actual fun frameNanos(): Long = remember {
    val fps = UIScreen.mainScreen.maximumFramesPerSecond.toInt().takeIf { it > 0 } ?: 60
    1_000_000_000L / fps
}

@Composable
actual fun rememberReject(): () -> Unit = remember {
    val gen = UINotificationFeedbackGenerator();
    { gen.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError) }
}
