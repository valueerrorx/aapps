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
class Haptics internal constructor(private val view: View, private val enabled: () -> Boolean) {
    private val gate = HapticGate()
    private val vibrator: Vibrator? by lazy { vibratorOf(view.context) }

    // Ereignis (Fund, Einrasten, gestimmt): spuerbar, auch wenn Touch-Haptik im System aus ist
    fun tick() = event(strong = false)

    fun confirm() = event(strong = true)

    // Raster beim Ziehen oder Wechsel im Segment: nur Touch-Feedback
    fun step() = perform(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )

    fun press() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

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

// Entprellung: hoechstens alle minGapMs, nur ein starkes Ereignis darf ein frisches schwaches ueberholen
internal class HapticGate(private val minGapMs: Long = MIN_GAP_MS) {
    private var lastAt = 0L
    private var last: Boolean? = null

    fun allow(strong: Boolean, nowMs: Long): Boolean {
        val free = last == null || nowMs - lastAt >= minGapMs
        if (!free && !(strong && last == false)) return false
        lastAt = nowMs
        last = strong
        return true
    }

    companion object {
        const val MIN_GAP_MS = 120L
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
fun rememberHaptics(enabled: Boolean = true): Haptics {
    val view = LocalView.current
    val on = rememberUpdatedState(enabled)
    return remember(view) { Haptics(view) { on.value } }
}
