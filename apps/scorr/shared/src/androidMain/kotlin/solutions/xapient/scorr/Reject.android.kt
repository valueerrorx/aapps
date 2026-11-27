package solutions.xapient.scorr

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberReject(enabled: Boolean): () -> Unit {
    val view = LocalView.current
    val on = rememberUpdatedState(enabled)
    return remember(view) {
        {
            if (on.value) {
                view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
                )
            }
        }
    }
}
