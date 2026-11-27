package solutions.xapient.hertzr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.hertzr.ui.HertzrAbout
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.HertzrSettings
import solutions.xapient.hertzr.ui.ToneActions
import solutions.xapient.hertzr.ui.ToneScreen

enum class Screen { MAIN, SETTINGS, ABOUT }

// Oberflaeche fuer Android und iOS; Start/Stopp (Rechte, Dienst) macht die Plattform ueber onToggle
@Composable
fun HertzrContent(
    store: ToneStore,
    rate: Int,
    versionName: String,
    onToggle: () -> Unit,
    start: Screen = Screen.MAIN,
) {
    val settings by store.settings.collectAsState()
    val playing by store.playing.collectAsState()
    val headphones by store.headphones.collectAsState()
    var screen by rememberSaveable { mutableStateOf(start) }

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
                        onToggle = onToggle,
                        onSettings = { screen = Screen.SETTINGS },
                    )
                },
            )
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.MAIN }
                HertzrSettings(settings, store::setOutput, store::setKeepOn, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.MAIN })
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                HertzrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
