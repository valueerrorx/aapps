package solutions.xapient.hudr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.hudr.domain.Speedo
import solutions.xapient.hudr.ui.HudScreen
import solutions.xapient.hudr.ui.HudrAbout
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.HudrSettings
import solutions.xapient.hudr.ui.LocState
import solutions.xapient.hudr.ui.SpeedScreen

const val APP_NAME = "hudr"

private enum class Screen { MAIN, SETTINGS, ABOUT }

/**
 * Gemeinsam fuer Android und iOS. Von der Plattform: Standort-Recht ([loc], [onLoc]), GPS, Uhr in ns (monoton, wie die
 * Fix-Zeitstempel), Warnton, HUD-Fenster ([hudWindow]: Leisten, Drehung, Helligkeit) und [onGpsFailed] (Start abgelehnt).
 */
@Composable
fun HudrApp(
    prefs: Prefs,
    gps: GpsSource,
    loc: LocState,
    onLoc: () -> Unit,
    now: () -> Long,
    beep: () -> Unit,
    versionName: String,
    onGpsFailed: () -> Unit = {},
    hudWindow: @Composable (on: Boolean, bright: Boolean) -> Unit = { _, _ -> },
) {
    var settings by remember { mutableStateOf(prefs.settings) }
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    var hud by rememberSaveable { mutableStateOf(false) }
    // Fahrtwerte ueberstehen Prozess-Tod
    val speedo = rememberSaveable(saver = Saver<Speedo, DoubleArray>({ it.save() }, { s -> Speedo().apply { restore(s) } })) { Speedo() }
    // version: neu zeichnen (nur in der Zeichenphase gelesen); fixes: Zeitgeber fuer "Signal verloren"
    val version = remember { mutableIntStateOf(0) }
    val fixes = remember { mutableIntStateOf(0) }
    val ready = loc == LocState.READY

    val current by rememberUpdatedState(settings)
    val sound by rememberUpdatedState(beep)
    DisposableEffect(gps, speedo) {
        gps.onFix = { f ->
            val s = current
            if (speedo.onFix(f, s.unit, s.activeLimit) && s.sound) sound()
            fixes.intValue++
            version.intValue++
        }
        // nur bei geaenderter Satellitenzahl neu zeichnen
        gps.onSatellites = { used, visible ->
            if (speedo.onSatellites(used, visible)) version.intValue++
        }
        onDispose {
            gps.onFix = null
            gps.onSatellites = null
        }
    }
    // Standort nur zwischen ON_START und ON_STOP, nie im Hintergrund
    LifecycleStartEffect(gps, ready) {
        if (ready) {
            speedo.restart()
            version.intValue++
            if (!gps.start()) onGpsFailed()
        }
        onStopOrDispose { gps.stop() }
    }
    // einmal neu zeichnen, wenn der letzte Fix veraltet; danach Ruhe
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        snapshotFlow { fixes.intValue }.collectLatest {
            delay(Speedo.STALE_NS / 1_000_000 + 100)
            version.intValue++
        }
    }

    // HUD: Leisten weg, Drehung gesperrt, auf Wunsch volle Helligkeit; beim Verlassen zurueck
    hudWindow(hud && ready, settings.bright)

    fun change(s: Settings) {
        settings = s
        prefs.settings = s
        version.intValue++
    }
    val reading = { speedo.reading(now(), settings.unit, settings.activeLimit) }

    AappsTheme(HudrAccent) {
        val haptics = rememberHaptics()
        when {
            screen == Screen.MAIN && hud && ready -> {
                BackEffect { hud = false }
                HudScreen(reading, version, settings, haptics, onExit = { hud = false })
            }
            screen == Screen.MAIN -> SpeedScreen(
                reading = reading,
                version = version,
                loc = loc,
                settings = settings,
                haptics = haptics,
                onHud = { hud = true },
                onReset = {
                    speedo.reset()
                    version.intValue++
                },
                onLoc = onLoc,
                onSettings = { screen = Screen.SETTINGS },
            )
            screen == Screen.SETTINGS -> {
                BackEffect { screen = Screen.MAIN }
                HudrSettings(settings, ::change, haptics, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.MAIN })
            }
            else -> {
                BackEffect { screen = Screen.SETTINGS }
                HudrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
