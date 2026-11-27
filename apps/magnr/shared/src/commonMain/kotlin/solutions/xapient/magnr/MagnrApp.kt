package solutions.xapient.magnr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.magnr.domain.Beeper
import solutions.xapient.magnr.domain.FieldMeter
import solutions.xapient.magnr.domain.FieldSource
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.ui.DetectorScreen
import solutions.xapient.magnr.ui.DetectorState
import solutions.xapient.magnr.ui.MagnrAbout
import solutions.xapient.magnr.ui.MagnrAccent
import solutions.xapient.magnr.ui.MagnrSettings

const val APP_NAME = "magnr"

private enum class Screen { MAIN, SETTINGS, ABOUT }

// Gemeinsam fuer Android und iOS; Sensor, Ton, Speicher und Uhr (ms seit Geraetestart, monoton) kommen von der Plattform
@Composable
fun MagnrApp(
    prefs: Prefs,
    newSource: () -> FieldSource,
    newBeeper: () -> Beeper,
    elapsedMillis: () -> Long,
    versionName: String,
) {
    var settings by remember { mutableStateOf(prefs.settings) }
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    var paused by rememberSaveable { mutableStateOf(false) }
    val source = remember { newSource() }
    val beeper = remember { newBeeper() }
    // Grundwert und Verlauf ueberstehen Prozess-Tod
    val meter = rememberSaveable(
        saver = Saver<FieldMeter, FloatArray>({ it.save() }, { s -> FieldMeter().apply { restore(s) } }),
    ) { FieldMeter() }
    val state = remember(meter) {
        DetectorState(meter) { source.maxRange }.apply { introVisible = !prefs.introSeen }
    }
    val available = source.isAvailable
    // gemessen wird nur auf dem Hauptschirm: in Einstellungen/Info kein Sensor, kein Ton, keine Haptik
    val live = available && !paused && screen == Screen.MAIN

    LaunchedEffect(settings.sensitivity, settings.drift) {
        meter.changeSensitivity(settings.sensitivity)
        meter.drift = settings.drift
    }
    // Einfuehrung einmal weggetippt: gemerkt
    LaunchedEffect(state.introVisible) {
        if (!state.introVisible) prefs.introSeen = true
    }
    // Sensor nur zwischen ON_START und ON_STOP, nie im Hintergrund; Grundwert bleibt bei kurzer Pause
    LifecycleStartEffect(source, live) {
        if (live) {
            state.resume(elapsedMillis())
            source.start(state.listener)
        }
        onStopOrDispose {
            source.stop()
            state.pause(elapsedMillis())
        }
    }
    // Ton ebenso, und nur wenn eingeschaltet
    LifecycleStartEffect(beeper, live, settings.sound) {
        if (live && settings.sound) beeper.start()
        onStopOrDispose { beeper.stop() }
    }
    DisposableEffect(state, beeper) {
        state.onStrength = { beeper.strength = it }
        onDispose { state.onStrength = null }
    }
    fun change(s: Settings) {
        settings = s
        prefs.settings = s
    }

    AappsTheme(MagnrAccent) {
        val haptics = rememberHaptics(settings.haptics)
        // Steigen der Stufe ist ein Ereignis: Vibrator, nicht Touch-Haptik
        DisposableEffect(state, haptics) {
            state.onRise = { l -> if (l == Level.STRONG) haptics.confirm() else haptics.tick() }
            onDispose { state.onRise = null }
        }
        when (screen) {
            Screen.MAIN -> DetectorScreen(
                state = state,
                sensitivity = settings.sensitivity,
                available = available,
                live = live,
                sound = settings.sound,
                keepOn = settings.keepOn,
                haptics = haptics,
                onPause = { paused = !paused },
                onZero = {
                    state.tare()
                    paused = false
                },
                onSound = { change(settings.copy(sound = !settings.sound)) },
                onSettings = { screen = Screen.SETTINGS },
                onIntro = { state.introVisible = true },
            )
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.MAIN }
                MagnrSettings(
                    settings,
                    {
                        if (it.sensitivity != settings.sensitivity) haptics.step()
                        change(it)
                    },
                    onIntro = {
                        state.introVisible = true
                        screen = Screen.MAIN
                    },
                    onCalibrate = {
                        state.showCalibration()
                        screen = Screen.MAIN
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.MAIN },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                MagnrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
