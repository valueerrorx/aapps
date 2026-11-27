package solutions.xapient.tunr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import solutions.xapient.aapps.core.ui.SharedPrefsKeyValues
import solutions.xapient.tunr.audio.AudioRecordMic
import solutions.xapient.tunr.audio.AudioTrackTone
import solutions.xapient.tunr.audio.TonePlayer
import solutions.xapient.tunr.audio.Mic
import solutions.xapient.tunr.domain.MicAccess
import solutions.xapient.tunr.domain.micAccess

private fun accessOf(context: Context, activity: Activity?, prefs: Prefs): MicAccess {
    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    // in den App-Infos erlaubt: Sperre vergessen
    if (granted && prefs.micBlocked) prefs.micBlocked = false
    return micAccess(
        granted = granted,
        deniedWithoutDialog = prefs.micBlocked,
        showRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true,
    )
}

// laufende Anfrage: zeigt das System einen Dialog, pausiert die Activity; ohne Dialog kommt das Nein sofort
private class Ask {
    var dialog = false
    var byTap = false
    var at = 0L
}

private const val INSTANT_MS = 500L

@Composable
fun TunrApp(toneFactory: () -> TonePlayer = ::AudioTrackTone, micFactory: (Context) -> Mic = ::AudioRecordMic) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = remember { Prefs(context) }
    val mic = remember { micFactory(context) }
    val tone = remember { toneFactory() }

    var access by remember { mutableStateOf(accessOf(context, activity, prefs)) }
    val asking = remember { Ask() }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        prefs.askedMic = true
        // ohne Dialog abgelehnt = dauerhaft gesperrt; nach Tippen gleich zu den App-Infos
        if (!granted && !asking.dialog && SystemClock.uptimeMillis() - asking.at < INSTANT_MS) {
            prefs.micBlocked = true
            if (asking.byTap) openAppInfo(context)
        }
        access = accessOf(context, activity, prefs)
    }
    fun request(byTap: Boolean) {
        asking.dialog = false
        asking.byTap = byTap
        asking.at = SystemClock.uptimeMillis()
        ask.launch(Manifest.permission.RECORD_AUDIO)
    }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { asking.dialog = true }
    }
    // zurueck aus den App-Infos: neu pruefen
    LifecycleStartEffect(Unit) {
        access = accessOf(context, activity, prefs)
        onStopOrDispose { }
    }
    // beim ersten Oeffnen einmal fragen; das Werkzeug kann ohne Mikrofon nichts
    LaunchedEffect(Unit) {
        if (!prefs.askedMic && access == MicAccess.ASK) request(byTap = false)
    }
    TunrContent(prefs, mic, tone, access, BuildConfig.VERSION_NAME, onMicAction = { request(byTap = true) })
}

private fun openAppInfo(context: Context) {
    val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // ohne App-Infos bleibt der Hinweis stehen
    }
}

fun Prefs(context: Context) = Prefs(SharedPrefsKeyValues(context.applicationContext, "tunr"))
