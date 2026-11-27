package solutions.xapient.dividr

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import solutions.xapient.dividr.domain.Locale
import solutions.xapient.dividr.ui.CameraUi
import kotlin.math.floor

/**
 * Kamera und Teilen in Swift (iosApp/dividr/dividr/DividrHost.swift): AVFoundation und
 * UIActivityViewController, Kotlin bettet nur die Vorschau ein.
 */
interface DividrHost {
    /** [CAMERA_ASK], [CAMERA_ON], [CAMERA_BLOCKED] oder [CAMERA_NONE] */
    fun cameraStatus(): Int

    /** fragt nach der Kamera; [done] laeuft auf dem Main-Thread */
    fun requestCamera(done: () -> Unit)

    fun openSettings()

    /** Vorschau mit QR-Erkennung; Texte kommen auf dem Main-Thread */
    fun makeScanner(onText: (String) -> Unit, onError: () -> Unit): UIView

    fun stopScanner(view: UIView)

    fun share(text: String)
}

const val CAMERA_ASK = 0
const val CAMERA_ON = 1
const val CAMERA_BLOCKED = 2
const val CAMERA_NONE = 3

internal val LocalDividrHost = staticCompositionLocalOf<DividrHost?> { null }

@Composable
actual fun rememberCameraAccess(store: DividrStore): CameraAccess {
    val host = LocalDividrHost.current
    fun current(): CameraUi = when (host?.cameraStatus() ?: CAMERA_NONE) {
        CAMERA_ON -> CameraUi.ON
        CAMERA_BLOCKED -> CameraUi.BLOCKED
        CAMERA_ASK -> CameraUi.ASK
        else -> CameraUi.ERROR
    }
    var ui by remember { mutableStateOf(current()) }
    return remember(host) {
        object : CameraAccess {
            override val state get() = ui
            override fun refresh() { ui = current() }
            override fun allow() {
                when (ui) {
                    CameraUi.BLOCKED -> host?.openSettings()
                    CameraUi.ASK -> host?.requestCamera { ui = current() }
                    else -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraScan(onText: (String) -> Unit, onError: () -> Unit, modifier: Modifier) {
    val host = LocalDividrHost.current ?: return
    val text by rememberUpdatedState(onText)
    val error by rememberUpdatedState(onError)
    UIKitView(
        factory = { host.makeScanner({ text(it) }, { error() }) },
        modifier = modifier.fillMaxSize(),
        onRelease = { host.stopScanner(it) },
    )
}

@Composable
actual fun rememberShare(): (String) -> Unit {
    val host = LocalDividrHost.current
    return remember(host) { { text -> host?.share(text) } }
}

@Composable
actual fun FullBrightness() {
    DisposableEffect(Unit) {
        val screen = UIScreen.mainScreen
        val old = screen.brightness
        screen.brightness = 1.0
        onDispose { screen.brightness = old }
    }
}

@Composable
actual fun currentLocale(): Locale = remember { NSLocale.currentLocale }

actual fun todayEpochDay(): Int {
    val local = NSDate().timeIntervalSince1970 + NSTimeZone.localTimeZone.secondsFromGMT.toDouble()
    return floor(local / 86_400.0).toInt()
}

actual fun formatDay(day: Int, today: Int, locale: Locale): String {
    val f = NSDateFormatter()
    f.locale = locale
    f.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    f.setLocalizedDateFormatFromTemplate(if (yearOf(day) == yearOf(today)) "EEEdMMM" else "dMMMyyyy")
    return f.stringFromDate(NSDate.dateWithTimeIntervalSince1970(day * 86_400.0))
}

// Jahr aus Tagen seit 1970 (proleptisch gregorianisch)
private fun yearOf(day: Int): Int {
    val z = day + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val m = if (mp < 10) mp + 3 else mp - 9
    return yoe + era * 400 + if (m <= 2) 1 else 0
}
