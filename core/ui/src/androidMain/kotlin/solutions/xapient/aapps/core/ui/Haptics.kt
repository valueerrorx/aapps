package solutions.xapient.aapps.core.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView

// tick/confirm melden Ereignisse ueber den Vibrator, step/press sind Touch-Feedback ueber die View
actual class Haptics internal constructor(private val view: View, private val enabled: () -> Boolean) {
    private val gate = HapticGate()
    private val vibrator: Vibrator? by lazy { vibratorOf(view.context) }

    // Ereignis (Fund, Einrasten, gestimmt): spuerbar, auch wenn Touch-Haptik im System aus ist
    actual fun tick() = event(strong = false)

    actual fun confirm() = event(strong = true)

    // Raster beim Ziehen oder Wechsel im Segment: nur Touch-Feedback
    actual fun step() = perform(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )

    actual fun press() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

    private fun perform(type: Int) {
        if (enabled()) view.performHapticFeedback(type)
    }

    private fun event(strong: Boolean) {
        if (!enabled() || !gate.allow(strong, SystemClock.uptimeMillis())) return
        val v = vibrator ?: return
        try {
            // Wecker-Kategorie: folgt Hauptschalter und Wecker-Vibration, nicht der Touch-Haptik, auch im Energiesparmodus
            if (Build.VERSION.SDK_INT >= 33) {
                v.vibrate(effect(strong), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effect(strong), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        } catch (_: RuntimeException) {
            // Vibrator nicht verfuegbar: Ereignis bleibt sichtbar
        }
    }
}

private fun vibratorOf(context: Context): Vibrator? {
    val v = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    return v?.takeIf { it.hasVibrator() }
}

// vordefinierte Effekte mit eingebautem Ersatzmuster; vor API 29 kurze Einzelimpulse
private fun effect(strong: Boolean): VibrationEffect = if (Build.VERSION.SDK_INT >= 29) {
    VibrationEffect.createPredefined(if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK)
} else {
    VibrationEffect.createOneShot(if (strong) 60L else 30L, VibrationEffect.DEFAULT_AMPLITUDE)
}

@Composable
actual fun rememberHaptics(enabled: Boolean): Haptics {
    val view = LocalView.current
    val on = rememberUpdatedState(enabled)
    return remember(view) { Haptics(view) { on.value } }
}
