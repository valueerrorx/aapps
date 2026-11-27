package solutions.xapient.loudr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.ui.LoudrAbout
import solutions.xapient.loudr.ui.LoudrAccent
import solutions.xapient.loudr.ui.LoudrSettings
import solutions.xapient.loudr.ui.MeterScreen
import solutions.xapient.loudr.ui.MicState

private enum class Screen { MAIN, SETTINGS, ABOUT }

// Oberflaeche fuer Android und iOS; Mikrofon-Recht und Lebenszyklus macht die Plattform
@Composable
fun LoudrContent(
    meter: LevelMeter,
    prefs: Prefs,
    mic: MicState,
    measuring: Boolean,
    versionName: String,
    onToggle: () -> Unit,
    onMic: () -> Unit,
) {
    var settings by remember { mutableStateOf(prefs.settings) }
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    fun change(s: Settings) {
        settings = s
        prefs.settings = s
    }

    AappsTheme(LoudrAccent) {
        val haptics = rememberHaptics()
        when (screen) {
            Screen.MAIN -> MeterScreen(
                reading = meter::reading,
                mic = mic,
                measuring = measuring,
                settings = settings,
                haptics = haptics,
                onToggle = onToggle,
                onReset = meter::reset,
                onMic = onMic,
                onSettings = { screen = Screen.SETTINGS },
            )
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.MAIN }
                LoudrSettings(
                    settings, ::change,
                    live = { if (measuring) Calibration.apply(meter.reading().level(settings.weighting), settings.offset) else Float.NaN },
                    haptics = haptics,
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.MAIN },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                LoudrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
