package solutions.xapient.pulsr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.pulsr.ui.PulseScreen
import solutions.xapient.pulsr.ui.PulsrAbout
import solutions.xapient.pulsr.ui.PulsrAccent
import solutions.xapient.pulsr.ui.PulsrSettings

const val APP_NAME = "pulsr"

private enum class Screen { MAIN, SETTINGS, ABOUT }

// toggle: Start/Stopp der Plattform (Android fragt vorher das Benachrichtigungs-Recht)
@Composable
fun PulsrApp(store: PulseStore, versionName: String, toggle: () -> Unit = store::toggle) {
    val settings by store.settings.collectAsState()
    val playing by store.playing.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }

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
                onTap = { store.tap(it) },
                onBeats = store::setBeats,
                onSub = { store.set(settings.copy(sub = it)) },
                onToggle = toggle,
                onSettings = { screen = Screen.SETTINGS },
            )
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.MAIN }
                PulsrSettings(settings, store::set, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.MAIN })
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                PulsrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
