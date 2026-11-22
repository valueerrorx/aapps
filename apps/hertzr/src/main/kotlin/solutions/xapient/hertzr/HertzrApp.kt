package solutions.xapient.hertzr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.hertzr.ui.HertzrAbout
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.HertzrSettings
import solutions.xapient.hertzr.ui.ToneActions
import solutions.xapient.hertzr.ui.ToneScreen

private enum class Screen { MAIN, SETTINGS, ABOUT }

@Composable
fun HertzrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val store = remember { ToneStore.get(context) }
    val settings by store.settings.collectAsState()
    val playing by store.playing.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    val headphones by store.headphones.collectAsState()
    val rate = remember { AudioEngine.nativeRate() }

    // Recht fuer die Benachrichtigung VOR dem Start erfragen; gespielt wird auch ohne.
    // Endgueltige Ablehnung erkennen: Hinweis war da und ist nach dem Nein weg. Weggetippt = beim naechsten Start wieder.
    fun rationale(): Boolean =
        Build.VERSION.SDK_INT >= 33 && context.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
    var rationaleBefore by remember { mutableStateOf(false) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && rationaleBefore && !rationale()) prefs.notifyBlocked = true
        store.start()
    }
    // liest den Store direkt: die gemerkten Aktionen sehen sonst einen alten Stand
    fun toggle() {
        if (store.playing.value) {
            store.stop()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !prefs.notifyBlocked &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            rationaleBefore = rationale()
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        store.start()
    }

    // Dienst haelt den Ton bei ausgeschaltetem Bildschirm am Leben
    LaunchedEffect(playing) {
        if (playing) ToneService.start(context)
    }

    AappsTheme(HertzrAccent) {
        val haptics = rememberHaptics()
        when (screen) {
            Screen.MAIN -> ToneScreen(
                settings = settings,
                playing = playing,
                maxHz = store.maxHz,
                live = store::shownHz,
                rate = rate,
                progress = store::progress,
                headphones = headphones,
                haptics = haptics,
                actions = remember(store) {
                    ToneActions(
                        onHz = store::setHz,
                        onNudge = store::nudge,
                        onOctave = store::octave,
                        onWave = store::setWave,
                        onLevel = store::setLevel,
                        onMode = store::setMode,
                        onSweepFrom = store::setSweepFrom,
                        onSweepTo = store::setSweepTo,
                        onDuration = store::nextDuration,
                        onToggle = ::toggle,
                        onSettings = { screen = Screen.SETTINGS },
                    )
                },
            )
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.MAIN }
                HertzrSettings(settings, store::setOutput, store::setKeepOn, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.MAIN })
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                HertzrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
