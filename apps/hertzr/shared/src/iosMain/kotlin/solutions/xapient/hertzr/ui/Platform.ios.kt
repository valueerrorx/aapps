package solutions.xapient.hertzr.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import solutions.xapient.hertzr.res.Res
import solutions.xapient.hertzr.res.note_background_ios

// folgt wie alle iOS-Haptik der Systemeinstellung
@Composable
internal actual fun rememberLimitBump(): () -> Unit {
    val gen = remember { UINotificationFeedbackGenerator() }
    return { gen.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError) }
}

@Composable
internal actual fun backgroundNote(): String = stringResource(Res.string.note_background_ios)
