package solutions.xapient.dividr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import solutions.xapient.dividr.domain.Locale
import solutions.xapient.dividr.ui.CameraUi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Kamera-Erlaubnis erst beim Scannen; "nie wieder fragen" fuehrt zu den App-Infos
@Composable
actual fun rememberCameraAccess(store: DividrStore): CameraAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current
    fun current(): CameraUi {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted && store.cameraBlocked) store.cameraBlocked = false
        return when {
            granted -> CameraUi.ON
            store.cameraBlocked -> CameraUi.BLOCKED
            else -> CameraUi.ASK
        }
    }
    var ui by remember { mutableStateOf(current()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false) store.cameraBlocked = true
        ui = current()
    }
    return remember(ask) {
        object : CameraAccess {
            override val state get() = ui
            override fun refresh() { ui = current() }
            override fun allow() {
                if (ui == CameraUi.BLOCKED) openAppInfo(context) else ask.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun openAppInfo(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // ohne App-Infos bleibt der Hinweis stehen
    }
}

@Composable
actual fun rememberShare(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            try {
                context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                // kein Ziel zum Teilen
            }
        }
    }
}

@Composable
actual fun FullBrightness() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val old = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = old } }
    }
}

@Composable
actual fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

actual fun todayEpochDay(): Int = LocalDate.now().toEpochDay().toInt()

actual fun formatDay(day: Int, today: Int, locale: Locale): String {
    val d = LocalDate.ofEpochDay(day.toLong())
    val pattern = if (d.year == LocalDate.ofEpochDay(today.toLong()).year) "EEE d MMM" else "d MMM yyyy"
    return d.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, pattern), locale))
}
