package solutions.xapient.levlr

import android.content.Context
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.levlr.domain.Calibration
import solutions.xapient.levlr.sensor.AndroidSensorSource
import solutions.xapient.levlr.ui.DiagnosticsRow
import solutions.xapient.levlr.ui.LevelScreen
import solutions.xapient.levlr.ui.LevelState
import solutions.xapient.levlr.ui.LevlrAbout
import solutions.xapient.levlr.ui.LevlrSettings

val LevlrAccent = Color(0xFFC6F24E)

private enum class Screen { LEVEL, SETTINGS, ABOUT }

@Composable
fun LevlrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var unit by remember { mutableStateOf(prefs.unit) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var calibration by remember { mutableStateOf(prefs.calibration) }
    var screen by rememberSaveable { mutableStateOf(Screen.LEVEL) }
    val state = remember { LevelState(calibration) }
    val sensor = remember {
        AndroidSensorSource(context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
    }

    AappsTheme(LevlrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        when (screen) {
            Screen.LEVEL -> {
                KeepScreenOn()
                // Display-Rotation fuer die Umrechnung; auch 90->270 ohne Config-Wechsel
                val view = LocalView.current
                DisposableEffect(view) {
                    val dm = context.getSystemService(DisplayManager::class.java)
                    val update = { state.quarterTurns = view.display?.rotation ?: 0 }
                    update()
                    val listener = object : DisplayManager.DisplayListener {
                        override fun onDisplayChanged(displayId: Int) = update()
                        override fun onDisplayAdded(displayId: Int) = Unit
                        override fun onDisplayRemoved(displayId: Int) = Unit
                    }
                    dm?.registerDisplayListener(listener, null)
                    onDispose { dm?.unregisterDisplayListener(listener) }
                }

                // Sensor nur zwischen onResume und onPause
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
                BackHandler { screen = Screen.LEVEL }
                LevlrSettings(
                    unit = unit,
                    onUnit = { unit = it; prefs.unit = it; haptics.tick() },
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
                    diagnostics = if (BuildConfig.DEBUG) ({ DiagnosticsRow() }) else null,
                )
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                LevlrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
