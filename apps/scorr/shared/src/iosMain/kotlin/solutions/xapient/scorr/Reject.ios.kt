package solutions.xapient.scorr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

// Fehler-Haptik; folgt wie alle iOS-Haptik der Systemeinstellung
@Composable
actual fun rememberReject(enabled: Boolean): () -> Unit {
    val on = rememberUpdatedState(enabled)
    val generator = remember { UINotificationFeedbackGenerator() }
    return remember(generator) {
        { if (on.value) generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError) }
    }
}
