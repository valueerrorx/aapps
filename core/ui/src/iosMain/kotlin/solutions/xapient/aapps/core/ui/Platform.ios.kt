package solutions.xapient.aapps.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator
import platform.posix.memcpy

// iOS-Haptik folgt immer der Systemeinstellung "Systemhaptik" (anders als der Android-Vibrator)
actual class Haptics internal constructor(private val enabled: () -> Boolean) {
    private val gate = HapticGate()
    private val impact = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val notify = UINotificationFeedbackGenerator()
    private val select = UISelectionFeedbackGenerator()

    actual fun tick() {
        if (allowed(strong = false)) impact.impactOccurred()
    }

    actual fun confirm() {
        if (allowed(strong = true)) notify.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    actual fun step() {
        if (enabled()) select.selectionChanged()
    }

    actual fun press() {
        if (enabled()) light.impactOccurred()
    }

    private fun allowed(strong: Boolean): Boolean =
        enabled() && gate.allow(strong, (NSProcessInfo.processInfo.systemUptime * 1000).toLong())
}

@Composable
actual fun rememberHaptics(enabled: Boolean): Haptics {
    val on = rememberUpdatedState(enabled)
    return remember { Haptics { on.value } }
}

@Composable
actual fun KeepScreenOn() {
    DisposableEffect(Unit) {
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

@Composable
internal actual fun noInternetText(): String = stringResource(Res.string.ios_promise_offline)

actual val SpaceGrotesk: FontFamily = family(
    "font/space_grotesk_medium.ttf" to FontWeight.Medium,
    "font/space_grotesk_bold.ttf" to FontWeight.Bold,
)

actual val Inter: FontFamily = family(
    "font/inter_regular.ttf" to FontWeight.Normal,
    "font/inter_semibold.ttf" to FontWeight.SemiBold,
)

// Schriften synchron aus den Compose-Ressourcen im App-Bundle; fehlt eine, gilt die Systemschrift
@OptIn(ExperimentalResourceApi::class)
private fun family(vararg files: Pair<String, FontWeight>): FontFamily {
    val fonts = files.map { (path, weight) ->
        val url = NSURL.URLWithString(Res.getUri(path)) ?: return FontFamily.Default
        val data = NSData.dataWithContentsOfURL(url) ?: return FontFamily.Default
        Font(path, data.toByteArray(), weight)
    }
    return FontFamily(fonts)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val out = ByteArray(size)
    if (size > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

/** Millisekunden seit 1970, wie System.currentTimeMillis */
fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

// Einstellungen einer App in den UserDefaults (Schluessel mit Praefix)
class UserDefaultsKeyValues(private val prefix: String) : KeyValues {
    private val d = NSUserDefaults.standardUserDefaults
    private fun has(key: String) = d.objectForKey(prefix + key) != null
    override fun int(key: String, default: Int) = if (has(key)) d.integerForKey(prefix + key).toInt() else default
    override fun putInt(key: String, value: Int) = d.setInteger(value.toLong(), prefix + key)
    override fun float(key: String, default: Float) = if (has(key)) d.floatForKey(prefix + key) else default
    override fun putFloat(key: String, value: Float) = d.setFloat(value, prefix + key)
    override fun bool(key: String, default: Boolean) = if (has(key)) d.boolForKey(prefix + key) else default
    override fun putBool(key: String, value: Boolean) = d.setBool(value, prefix + key)
}
