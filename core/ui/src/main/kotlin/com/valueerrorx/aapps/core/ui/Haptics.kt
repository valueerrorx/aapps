package com.valueerrorx.aapps.core.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView

// Haptik ueber die View, braucht keine VIBRATE-Berechtigung
class Haptics internal constructor(private val view: View, private val enabled: () -> Boolean) {
    fun tick() = perform(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )

    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    fun press() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

    private fun perform(type: Int) {
        if (enabled()) view.performHapticFeedback(type)
    }
}

@Composable
fun rememberHaptics(enabled: Boolean = true): Haptics {
    val view = LocalView.current
    val on = rememberUpdatedState(enabled)
    return remember(view) { Haptics(view) { on.value } }
}
