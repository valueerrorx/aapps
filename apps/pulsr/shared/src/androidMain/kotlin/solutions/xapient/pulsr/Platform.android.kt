package solutions.xapient.pulsr

import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

actual fun uptimeMillis(): Long = SystemClock.uptimeMillis()

actual fun presentNanos(frameNanos: Long): Long = frameNanos

@Composable
actual fun frameNanos(): Long {
    val view = LocalView.current
    return remember(view) { (1e9f / (view.display?.refreshRate?.takeIf { it > 0f } ?: 60f)).toLong() }
}

@Composable
actual fun rememberReject(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS) }
    }
}
