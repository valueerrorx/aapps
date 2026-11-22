package solutions.xapient.tunr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.tunr.audio.AudioRecordMic
import solutions.xapient.tunr.audio.Mic
import solutions.xapient.tunr.domain.MicAccess
import solutions.xapient.tunr.domain.Settings
import solutions.xapient.tunr.domain.micAccess
import solutions.xapient.tunr.ui.InstrumentScreen
import solutions.xapient.tunr.ui.MicUi
import solutions.xapient.tunr.ui.TunerScreen
import solutions.xapient.tunr.ui.TunerState
import solutions.xapient.tunr.ui.TunrAbout
import solutions.xapient.tunr.ui.TunrAccent
import solutions.xapient.tunr.ui.TunrSettings

private enum class Screen { TUNER, INSTRUMENT, SETTINGS, ABOUT }

private fun accessOf(context: Context, activity: Activity?, prefs: Prefs): MicAccess = micAccess(
    granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
    askedBefore = prefs.askedMic,
    showRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true,
)

@Composable
fun TunrApp(micFactory: (Context) -> Mic = ::AudioRecordMic) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = remember { Prefs(context) }
    var settings by remember { mutableStateOf(prefs.settings) }
    var screen by rememberSaveable { mutableStateOf(Screen.TUNER) }
    // festgehaltene Saite uebersteht Drehung und Prozess-Tod
    var locked by rememberSaveable { mutableIntStateOf(-1) }
    val state = remember { TunerState(settings.a4, settings.instrument) }
    val mic = remember { micFactory(context) }

    fun change(s: Settings) {
        val next = s.sane()
        if (next.instrument != settings.instrument) locked = -1
        settings = next
        prefs.settings = next
    }

    LaunchedEffect(settings.a4, settings.instrument) { state.configure(settings.a4, settings.instrument) }
    LaunchedEffect(locked, settings.instrument) { state.lock(locked) }

    var access by remember { mutableStateOf(accessOf(context, activity, prefs)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        prefs.askedMic = true
        access = accessOf(context, activity, prefs)
    }
    // zurueck aus den App-Infos: neu pruefen
    LifecycleStartEffect(Unit) {
        access = accessOf(context, activity, prefs)
        onStopOrDispose { }
    }
    // beim ersten Oeffnen einmal fragen; das Werkzeug kann ohne Mikrofon nichts
    LaunchedEffect(Unit) {
        if (!prefs.askedMic && access == MicAccess.ASK) ask.launch(Manifest.permission.RECORD_AUDIO)
    }
    var micError by remember { mutableStateOf(false) }

    AappsTheme(TunrAccent) {
        val haptics = rememberHaptics(settings.haptics)
        when (screen) {
            Screen.TUNER -> {
                val listening = access == MicAccess.GRANTED && !micError
                if (listening) {
                    KeepScreenOn()
                    // Mikrofon nur im Vordergrund: ON_START bis ON_STOP
                    LifecycleStartEffect(mic) {
                        if (!mic.start(state::push) { micError = true; state.stop() }) micError = true
                        onStopOrDispose {
                            mic.stop()
                            state.stop()
                        }
                    }
                }
                TunerScreen(
                    state = state,
                    settings = settings,
                    mic = when {
                        access == MicAccess.ASK -> MicUi.ASK
                        access == MicAccess.BLOCKED -> MicUi.BLOCKED
                        micError -> MicUi.ERROR
                        else -> MicUi.ON
                    },
                    locked = locked,
                    haptics = haptics,
                    onLock = { locked = it },
                    onInstrument = { screen = Screen.INSTRUMENT },
                    onSettings = { screen = Screen.SETTINGS },
                    onMicAction = {
                        when (access) {
                            MicAccess.ASK -> ask.launch(Manifest.permission.RECORD_AUDIO)
                            MicAccess.BLOCKED -> openAppInfo(context)
                            MicAccess.GRANTED -> micError = false
                        }
                    },
                )
            }
            Screen.INSTRUMENT -> {
                BackHandler { screen = Screen.TUNER }
                InstrumentScreen(settings.instrument, settings.notation, onSelect = {
                    haptics.tick()
                    change(settings.copy(instrument = it))
                    screen = Screen.TUNER
                }, onBack = { screen = Screen.TUNER })
            }
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.TUNER }
                TunrSettings(settings, ::change, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.TUNER })
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                TunrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
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
