package solutions.xapient.magnr

import android.content.Context
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.magnr.domain.Beeper
import solutions.xapient.magnr.domain.FieldMeter
import solutions.xapient.magnr.domain.FieldSource
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.sensor.AndroidFieldSource
import solutions.xapient.magnr.sensor.AudioBeeper
import solutions.xapient.magnr.ui.DetectorScreen
import solutions.xapient.magnr.ui.DetectorState
import solutions.xapient.magnr.ui.MagnrAbout
import solutions.xapient.magnr.ui.MagnrAccent
import solutions.xapient.magnr.ui.MagnrSettings

private enum class Screen { MAIN, SETTINGS, ABOUT }

private fun androidSource(context: Context): FieldSource =
    AndroidFieldSource(context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)

@Composable
fun MagnrApp(
    newSource: (Context) -> FieldSource = ::androidSource,
    newBeeper: () -> Beeper = ::AudioBeeper,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var settings by remember { mutableStateOf(prefs.settings) }
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    var paused by rememberSaveable { mutableStateOf(false) }
    val source = remember { newSource(context) }
    val beeper = remember { newBeeper() }
    // Grundwert und Verlauf ueberstehen Prozess-Tod
    val meter = rememberSaveable(
        saver = Saver<FieldMeter, FloatArray>({ it.save() }, { s -> FieldMeter().apply { restore(s) } }),
    ) { FieldMeter() }
    val state = remember(meter) {
        DetectorState(meter) { source.maxRange }.apply { introVisible = !prefs.introSeen }
    }
    val available = source.isAvailable
    val live = available && !paused

    LaunchedEffect(settings.sensitivity, settings.drift) {
        meter.changeSensitivity(settings.sensitivity)
        meter.drift = settings.drift
    }
    // Einfuehrung einmal weggetippt: gemerkt
    LaunchedEffect(state.introVisible) {
        if (!state.introVisible) prefs.introSeen = true
    }
    // Sensor nur zwischen ON_START und ON_STOP, nie im Hintergrund
    LifecycleStartEffect(source, paused) {
        if (!paused) source.start(state.listener)
        onStopOrDispose {
            source.stop()
            state.pause()
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
        // Haptik beim Steigen der Stufe
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
                BackHandler { screen = Screen.MAIN }
                MagnrSettings(
                    settings,
                    {
                        if (it.sensitivity != settings.sensitivity) haptics.tick()
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
                BackHandler { screen = Screen.SETTINGS }
                MagnrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
