package solutions.xapient.tunr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.tunr.audio.Mic
import solutions.xapient.tunr.audio.TonePlayer
import solutions.xapient.tunr.domain.MicAccess
import solutions.xapient.tunr.domain.Notes
import solutions.xapient.tunr.domain.Settings
import solutions.xapient.tunr.ui.InstrumentScreen
import solutions.xapient.tunr.ui.MicUi
import solutions.xapient.tunr.ui.TunerScreen
import solutions.xapient.tunr.ui.TunerState
import solutions.xapient.tunr.ui.TunrAbout
import solutions.xapient.tunr.ui.TunrAccent
import solutions.xapient.tunr.ui.TunrSettings

private enum class Screen { TUNER, INSTRUMENT, SETTINGS, ABOUT }

// Oberflaeche fuer Android und iOS; Mikrofon-Recht fragt die Plattform (onMicAction bei ASK/BLOCKED)
@Composable
fun TunrContent(prefs: Prefs, mic: Mic, tone: TonePlayer, access: MicAccess, versionName: String, onMicAction: () -> Unit) {
    var settings by remember { mutableStateOf(prefs.settings) }
    var screen by rememberSaveable { mutableStateOf(Screen.TUNER) }
    // festgehaltene Saite uebersteht Drehung und Prozess-Tod
    var locked by rememberSaveable { mutableIntStateOf(-1) }
    val state = remember { TunerState(settings.a4, settings.instrument) }

    fun change(s: Settings) {
        val next = s.sane()
        if (next.instrument != settings.instrument) locked = -1
        settings = next
        prefs.settings = next
    }

    LaunchedEffect(settings.a4, settings.instrument) { state.configure(settings.a4, settings.instrument) }
    LaunchedEffect(locked, settings.instrument) { state.lock(locked) }
    var micError by remember { mutableStateOf(false) }

    AappsTheme(TunrAccent) {
        val haptics = rememberHaptics(settings.haptics)
        when (screen) {
            Screen.TUNER -> {
                // Referenzton nur am Stimm-Bildschirm und im Vordergrund
                LifecycleStartEffect(tone) {
                    onStopOrDispose { tone.stop() }
                }
                val listening = access == MicAccess.GRANTED && !micError
                if (listening) {
                    KeepScreenOn()
                    // Mikrofon nur im Vordergrund: ON_START bis ON_STOP
                    LifecycleStartEffect(mic) {
                        if (!mic.start(state::push) { micError = true; state.stop() }) micError = true
                        onStopOrDispose {
                            mic.stop()
                            state.stop()
                        }
                    }
                }
                TunerScreen(
                    state = state,
                    settings = settings,
                    mic = when {
                        access == MicAccess.ASK -> MicUi.ASK
                        access == MicAccess.BLOCKED -> MicUi.BLOCKED
                        micError -> MicUi.ERROR
                        else -> MicUi.ON
                    },
                    locked = locked,
                    haptics = haptics,
                    onLock = { locked = it },
                    onPlay = { i ->
                        settings.instrument.strings.getOrNull(i)?.let { midi ->
                            tone.play(Notes.hz(midi, settings.a4.toFloat()))
                            state.reference(i)
                        }
                    },
                    onInstrument = { screen = Screen.INSTRUMENT },
                    onSettings = { screen = Screen.SETTINGS },
                    onMicAction = {
                        // auch gesperrt erst fragen: kommt sofort ein Nein, oeffnen sich die App-Infos
                        when (access) {
                            MicAccess.ASK, MicAccess.BLOCKED -> onMicAction()
                            MicAccess.GRANTED -> micError = false
                        }
                    },
                )
            }
            Screen.INSTRUMENT -> {
                BackEffect { screen = Screen.TUNER }
                InstrumentScreen(settings.instrument, settings.notation, onSelect = {
                    haptics.press()
                    change(settings.copy(instrument = it))
                    screen = Screen.TUNER
                }, onBack = { screen = Screen.TUNER })
            }
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.TUNER }
                TunrSettings(settings, ::change, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.TUNER })
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                TunrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
