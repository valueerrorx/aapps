package solutions.xapient.hertzr.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.hertzr.res.Res
import solutions.xapient.hertzr.res.note_background

@Composable
internal actual fun rememberLimitBump(): () -> Unit {
    val view = LocalView.current
    return { view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS) }
}

@Composable
internal actual fun backgroundNote(): String = stringResource(Res.string.note_background)
