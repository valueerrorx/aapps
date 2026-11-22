package solutions.xapient.pulsr

import android.Manifest
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
import solutions.xapient.pulsr.ui.PulseScreen
import solutions.xapient.pulsr.ui.PulsrAbout
import solutions.xapient.pulsr.ui.PulsrAccent
import solutions.xapient.pulsr.ui.PulsrSettings

private enum class Screen { MAIN, SETTINGS, ABOUT }

@Composable
fun PulsrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val store = remember { PulseStore.get(context) }
    val settings by store.settings.collectAsState()
    val playing by store.playing.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }

    // Recht fuer die Benachrichtigung einmal VOR dem ersten Start erfragen; gespielt wird auch ohne
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { store.start() }
    fun toggle() {
        if (playing) {
            store.stop()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotify &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            prefs.askedNotify = true
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        store.start()
    }

    // Dienst haelt das Spielen bei gesperrtem Bildschirm am Leben
    LaunchedEffect(playing) {
        if (playing) PulseService.start(context)
    }

    AappsTheme(PulsrAccent) {
        val haptics = rememberHaptics()
        when (screen) {
            Screen.MAIN -> PulseScreen(
                settings = settings,
                playing = playing,
                pulse = store::pulse,
                haptics = haptics,
                onNudge = store::nudge,
                onBpm = store::setBpm,
                onTap = { store.tap() },
                onBeats = store::setBeats,
                onSub = { store.set(settings.copy(sub = it)) },
                onToggle = ::toggle,
                onSettings = { screen = Screen.SETTINGS },
            )
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.MAIN }
                PulsrSettings(settings, store::set, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.MAIN })
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                PulsrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
