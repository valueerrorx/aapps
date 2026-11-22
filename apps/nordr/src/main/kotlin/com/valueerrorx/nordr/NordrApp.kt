package com.valueerrorx.nordr

import android.content.Context
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.valueerrorx.aapps.core.ui.AappsTheme
import com.valueerrorx.aapps.core.ui.KeepScreenOn
import com.valueerrorx.aapps.core.ui.rememberHaptics
import com.valueerrorx.nordr.domain.NorthRef
import com.valueerrorx.nordr.domain.Region
import com.valueerrorx.nordr.sensor.AndroidDeclination
import com.valueerrorx.nordr.sensor.AndroidSensorSource
import com.valueerrorx.nordr.ui.CompassScreen
import com.valueerrorx.nordr.ui.CompassState
import com.valueerrorx.nordr.ui.DiagnosticsRow
import com.valueerrorx.nordr.ui.NordrAbout
import com.valueerrorx.nordr.ui.NordrSettings
import com.valueerrorx.nordr.ui.RegionPicker

val NordrAccent = Color(0xFF8AD8FF)

private enum class Screen { COMPASS, SETTINGS, REGION, ABOUT }

private fun declinationOf(region: Region): Float =
    AndroidDeclination.declination(region.lat, region.lon, System.currentTimeMillis())

@Composable
fun NordrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var northRef by remember { mutableStateOf(prefs.northRef) }
    var region by remember { mutableStateOf(prefs.region) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var screen by rememberSaveable { mutableStateOf(Screen.COMPASS) }
    val state = remember { CompassState().apply { engine.restoreCourse(prefs.course) } }
    val sensor = remember {
        AndroidSensorSource(context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
    }
    val regionDeclination = remember(region) { declinationOf(region) }

    // rechtweisend = magnetisch + Deklination der Region
    LaunchedEffect(northRef, regionDeclination) {
        state.setDeclination(if (northRef == NorthRef.TRUE) regionDeclination else 0f)
    }

    AappsTheme(NordrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        when (screen) {
            Screen.COMPASS -> {
                KeepScreenOn()
                // Display-Rotation fuer die Blickrichtung; auch 90->270 ohne Config-Wechsel
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

                // Sensoren nur zwischen onResume und onPause
                LifecycleResumeEffect(sensor) {
                    sensor.start(state.listener)
                    onPauseOrDispose {
                        sensor.stop()
                        state.onPause()
                    }
                }
                CompassScreen(
                    state = state,
                    northRef = northRef,
                    haptics = haptics,
                    onOpenSettings = { screen = Screen.SETTINGS },
                    onCourseChanged = { prefs.course = it },
                    sensorMissing = !sensor.isAvailable,
                )
            }
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.COMPASS }
                NordrSettings(
                    northRef = northRef,
                    onNorthRef = { northRef = it; prefs.northRef = it; haptics.tick() },
                    region = region,
                    declination = regionDeclination,
                    onRegion = { screen = Screen.REGION },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    onShowCalibration = {
                        state.showHint()
                        screen = Screen.COMPASS
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.COMPASS },
                    diagnostics = if (BuildConfig.DEBUG) ({ DiagnosticsRow() }) else null,
                )
            }
            Screen.REGION -> {
                BackHandler { screen = Screen.SETTINGS }
                val declinations = remember { mutableMapOf<String, Float>() }
                RegionPicker(
                    selected = region,
                    declinationOf = { r -> declinations.getOrPut(r.id) { declinationOf(r) } },
                    onSelect = {
                        region = it
                        prefs.region = it
                        haptics.tick()
                        screen = Screen.SETTINGS
                    },
                    onBack = { screen = Screen.SETTINGS },
                )
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                NordrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
