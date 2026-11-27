package solutions.xapient.nordr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import solutions.xapient.nordr.domain.NorthRef
import solutions.xapient.nordr.domain.Region
import solutions.xapient.nordr.domain.SensorSource
import solutions.xapient.nordr.ui.CompassScreen
import solutions.xapient.nordr.ui.CompassState
import solutions.xapient.nordr.ui.NordrAbout
import solutions.xapient.nordr.ui.NordrSettings
import solutions.xapient.nordr.ui.RegionPicker

const val APP_NAME = "nordr"

val NordrAccent = Color(0xFF8AD8FF)

private enum class Screen { COMPASS, SETTINGS, REGION, ABOUT }

// Gemeinsam fuer Android und iOS; Sensor, Speicher, Deklination (jetzt, je Region) und Diagnose kommen von der Plattform
@Composable
fun NordrApp(
    prefs: Prefs,
    sensor: SensorSource,
    declinationOf: (Region) -> Float,
    versionName: String,
    diagnostics: (@Composable () -> Unit)? = null,
) {
    var northRef by remember { mutableStateOf(prefs.northRef) }
    var region by remember { mutableStateOf(prefs.region) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var screen by rememberSaveable { mutableStateOf(Screen.COMPASS) }
    val state = remember { CompassState().apply { engine.restoreCourse(prefs.course) } }
    // Deklination je Region einmal rechnen, ueber Bildschirmwechsel hinweg
    val declinations = remember { mutableMapOf<String, Float>() }
    val cachedDeclination = { r: Region -> declinations.getOrPut(r.id) { declinationOf(r) } }
    val regionDeclination = remember(region) { cachedDeclination(region) }

    // rechtweisend = magnetisch + Deklination der Region
    LaunchedEffect(northRef, regionDeclination) {
        state.setDeclination(if (northRef == NorthRef.TRUE) regionDeclination else 0f)
    }

    AappsTheme(NordrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        when (screen) {
            Screen.COMPASS -> {
                KeepScreenOn()
                // Display-Rotation fuer die Blickrichtung
                DisplayRotationEffect { state.quarterTurns = it }

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
                BackEffect { screen = Screen.COMPASS }
                NordrSettings(
                    northRef = northRef,
                    onNorthRef = { northRef = it; prefs.northRef = it; haptics.step() },
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
                    diagnostics = diagnostics,
                )
            }
            Screen.REGION -> {
                BackEffect { screen = Screen.SETTINGS }
                RegionPicker(
                    selected = region,
                    declinationOf = cachedDeclination,
                    onSelect = {
                        region = it
                        prefs.region = it
                        haptics.press()
                        screen = Screen.SETTINGS
                    },
                    onBack = { screen = Screen.SETTINGS },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                NordrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
