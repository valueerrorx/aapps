package solutions.xapient.roundr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.roundr.domain.Codec
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Workout
import solutions.xapient.roundr.ui.EditorScreen
import solutions.xapient.roundr.ui.HomeScreen
import solutions.xapient.roundr.ui.LibraryScreen
import solutions.xapient.roundr.ui.RoundrAbout
import solutions.xapient.roundr.ui.RoundrAccent
import solutions.xapient.roundr.ui.RoundrSettings
import solutions.xapient.roundr.ui.RunScreen

const val APP_NAME = "roundr"

// Gespeicherte Einstellungen; Android SharedPreferences, iOS NSUserDefaults
interface RoundrPrefs {
    var workout: Workout
    var saved: List<Workout>
    var sound: Boolean
    var vibrate: Boolean
    var keepOn: Boolean
}

private enum class Screen { HOME, LIBRARY, EDITOR, SETTINGS, ABOUT }

/**
 * start: Plattform startet den Lauf (Android fragt vorher einmal nach dem Benachrichtigungsrecht).
 * onRun: bei jedem neuen, noch laufenden Lauf (Android startet den Dienst).
 */
@Composable
fun RoundrApp(
    prefs: RoundrPrefs,
    store: RunStore,
    versionName: String,
    start: (Workout) -> Unit = store::start,
    onRun: (Run) -> Unit = {},
    iosNote: Boolean = false,
) {
    val run by store.state.collectAsState()
    var workout by remember { mutableStateOf(prefs.workout) }
    var saved by remember { mutableStateOf(prefs.saved) }
    var sound by remember { mutableStateOf(prefs.sound) }
    var vibrate by remember { mutableStateOf(prefs.vibrate) }
    var keepOn by remember { mutableStateOf(prefs.keepOn) }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    fun choose(w: Workout) {
        workout = w
        prefs.workout = w
    }

    val active = run
    LaunchedEffect(active?.id) {
        if (active != null && !active.finished(store.now())) onRun(active)
    }

    AappsTheme(RoundrAccent) {
        val haptics = rememberHaptics()
        if (active != null) {
            RunScreen(
                run = active,
                clock = store::now,
                haptics = haptics,
                onToggle = store::toggle,
                onSkip = store::skip,
                onBack = store::back,
                onStop = store::clear,
                onAgain = { start(active.workout) },
                onClose = store::clear,
                keepOn = keepOn,
            )
            return@AappsTheme
        }
        when (screen) {
            Screen.HOME -> HomeScreen(
                workout = workout,
                onLibrary = { screen = Screen.LIBRARY },
                onEdit = { screen = Screen.EDITOR },
                onSettings = { screen = Screen.SETTINGS },
                onStart = { haptics.press(); start(workout) },
            )
            Screen.LIBRARY -> {
                BackEffect { screen = Screen.HOME }
                LibraryScreen(
                    current = workout,
                    saved = saved,
                    onPick = { choose(it); haptics.press(); screen = Screen.HOME },
                    onDelete = { w ->
                        saved = saved - w
                        prefs.saved = saved
                    },
                    onBack = { screen = Screen.HOME },
                )
            }
            Screen.EDITOR -> {
                BackEffect { screen = Screen.HOME }
                EditorScreen(
                    workout = workout,
                    savedCount = saved.size,
                    haptics = haptics,
                    onChange = { choose(it) },
                    onSave = { name ->
                        val list = Codec.save(saved, workout, name)
                        if (list != null) {
                            saved = list
                            prefs.saved = list
                            choose(list.first())
                        }
                        list != null
                    },
                    onBack = { screen = Screen.HOME },
                )
            }
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.HOME }
                RoundrSettings(
                    sound = sound,
                    onSound = { sound = it; prefs.sound = it },
                    vibrate = vibrate,
                    onVibrate = { vibrate = it; prefs.vibrate = it },
                    keepOn = keepOn,
                    onKeepOn = { keepOn = it; prefs.keepOn = it },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.HOME },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                RoundrAbout(versionName, iosNote) { screen = Screen.SETTINGS }
            }
        }
    }
}
