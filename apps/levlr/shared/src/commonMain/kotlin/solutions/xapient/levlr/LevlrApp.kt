package solutions.xapient.levlr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LifecycleResumeEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.DisplayRotationEffect
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.levlr.domain.Calibration
import solutions.xapient.levlr.domain.SensorSource
import solutions.xapient.levlr.ui.LevelScreen
import solutions.xapient.levlr.ui.LevelState
import solutions.xapient.levlr.ui.LevlrAbout
import solutions.xapient.levlr.ui.LevlrSettings

const val APP_NAME = "levlr"

val LevlrAccent = Color(0xFFC6F24E)

private enum class Screen { LEVEL, SETTINGS, ABOUT }

// Gemeinsam fuer Android und iOS; Sensor, Speicher und Diagnose kommen von der Plattform
@Composable
fun LevlrApp(
    prefs: Prefs,
    sensor: SensorSource,
    versionName: String,
    diagnostics: (@Composable () -> Unit)? = null,
) {
    var unit by remember { mutableStateOf(prefs.unit) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var calibration by remember { mutableStateOf(prefs.calibration) }
    var screen by rememberSaveable { mutableStateOf(Screen.LEVEL) }
    val state = remember { LevelState(calibration) }

    AappsTheme(LevlrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        when (screen) {
            Screen.LEVEL -> {
                KeepScreenOn()
                // Display-Rotation fuer die Umrechnung Geraet -> Bildschirm
                DisplayRotationEffect { state.quarterTurns = it }

                // Sensor nur, solange die App im Vordergrund ist
                LifecycleResumeEffect(sensor) {
                    sensor.start { v, t -> state.onSample(v, t) }
                    onPauseOrDispose {
                        sensor.stop()
                        state.onPause()
                    }
                }
                LevelScreen(
                    state = state,
                    unit = unit,
                    haptics = haptics,
                    onOpenSettings = { screen = Screen.SETTINGS },
                    onCalibrationDone = { result ->
                        calibration = calibration.with(result.mode, result.bias)
                        prefs.calibration = calibration
                        state.engine.calibration = calibration
                    },
                    sensorMissing = !sensor.isAvailable,
                )
            }
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.LEVEL }
                LevlrSettings(
                    unit = unit,
                    onUnit = { unit = it; prefs.unit = it; haptics.step() },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    calibration = calibration,
                    onCalibrate = {
                        state.startCalibration()
                        screen = Screen.LEVEL
                    },
                    onResetCalibration = {
                        calibration = Calibration()
                        prefs.calibration = calibration
                        state.engine.calibration = calibration
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.LEVEL },
                    diagnostics = diagnostics,
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                LevlrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
