package solutions.xapient.loudr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import solutions.xapient.aapps.core.ui.SharedPrefsKeyValues
import solutions.xapient.loudr.ui.MicState

private const val MIC = Manifest.permission.RECORD_AUDIO

@Composable
fun LoudrApp(newMeter: (Context) -> LevelMeter = ::MicMeter) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = remember { Prefs(context) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var measuring by remember { mutableStateOf(false) }
    var silenced by remember { mutableStateOf(false) }
    // Min/Max/Leq/Verlauf ueberstehen Prozess-Tod
    val meter = rememberSaveable(
        saver = Saver<LevelMeter, DoubleArray>({ it.save() }, { s -> newMeter(context).apply { restore(s) } }),
    ) { newMeter(context) }

    fun granted() = context.checkSelfPermission(MIC) == PackageManager.PERMISSION_GRANTED
    // "nie wieder fragen": nach echter Ablehnung (Begruendung war schon gefragt) zeigt das System keine mehr; weggetippt bleibt fragbar
    fun blockedNow(): Boolean {
        if (granted()) return false
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, MIC) } == true
        if (rationale) prefs.rationaleSeen = true
        return prefs.rationaleSeen && !rationale
    }
    var hasMic by remember { mutableStateOf(granted()) }
    var blocked by remember { mutableStateOf(blockedNow()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        prefs.askedMic = true
        hasMic = ok
        blocked = blockedNow()
    }
    // beim allerersten Start einmal von selbst fragen
    LaunchedEffect(Unit) {
        if (!hasMic && !prefs.askedMic) ask.launch(MIC)
    }
    // Rueckkehr aus den App-Einstellungen
    LifecycleResumeEffect(Unit) {
        hasMic = granted()
        blocked = blockedNow()
        onPauseOrDispose { }
    }

    // Fehler und Stummschaltung kommen vom Audio-Thread
    DisposableEffect(meter) {
        val main = Handler(Looper.getMainLooper())
        meter.onFailure = { main.post { failed = true } }
        meter.onSilenced = { s -> main.post { silenced = s } }
        onDispose {
            meter.onFailure = null
            meter.onSilenced = null
        }
    }
    // Mikrofon nur zwischen ON_START und ON_STOP, nie im Hintergrund
    LifecycleStartEffect(meter, hasMic, paused, failed) {
        silenced = false
        measuring = hasMic && !paused && !failed && meter.start()
        if (hasMic && !paused && !failed && !measuring) failed = true
        onStopOrDispose {
            meter.stop()
            measuring = false
            silenced = false
        }
    }

    val mic = when {
        !hasMic && blocked -> MicState.BLOCKED
        !hasMic -> MicState.ASK
        failed -> MicState.FAILED
        silenced && measuring -> MicState.SILENCED
        else -> MicState.READY
    }
    fun onMic() {
        when (mic) {
            MicState.ASK -> ask.launch(MIC)
            MicState.BLOCKED -> try {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: ActivityNotFoundException) {
                // keine Einstellungs-App: nichts zu tun
            }
            MicState.FAILED -> failed = false
            MicState.READY, MicState.SILENCED -> Unit
        }
    }
    LoudrContent(meter, prefs, mic, measuring, BuildConfig.VERSION_NAME, onToggle = { paused = !paused }, onMic = ::onMic)
}

fun Prefs(context: Context) = Prefs(SharedPrefsKeyValues(context.applicationContext, "loudr"))
