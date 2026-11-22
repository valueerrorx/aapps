package solutions.xapient.roundr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.roundr.domain.Codec
import solutions.xapient.roundr.domain.Workout
import solutions.xapient.roundr.ui.EditorScreen
import solutions.xapient.roundr.ui.HomeScreen
import solutions.xapient.roundr.ui.LibraryScreen
import solutions.xapient.roundr.ui.RoundrAbout
import solutions.xapient.roundr.ui.RoundrAccent
import solutions.xapient.roundr.ui.RoundrSettings
import solutions.xapient.roundr.ui.RunScreen

private enum class Screen { HOME, LIBRARY, EDITOR, SETTINGS, ABOUT }

@Composable
fun RoundrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val store = remember { RunStore.get(context) }
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

    // Recht fuer die Benachrichtigung einmal erfragen; der Timer laeuft auch ohne
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun start(w: Workout) {
        store.start(w)
        TimerService.start(context)
        if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotify &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            prefs.askedNotify = true
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Nach Prozess-Tod: laufenden Lauf wieder an den Dienst haengen
    val active = run
    LaunchedEffect(active?.id) {
        if (active != null && !active.finished(store.now())) TimerService.start(context)
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
                onStart = { haptics.confirm(); start(workout) },
            )
            Screen.LIBRARY -> {
                BackHandler { screen = Screen.HOME }
                LibraryScreen(
                    current = workout,
                    saved = saved,
                    onPick = { choose(it); haptics.tick(); screen = Screen.HOME },
                    onDelete = { w ->
                        saved = saved - w
                        prefs.saved = saved
                    },
                    onBack = { screen = Screen.HOME },
                )
            }
            Screen.EDITOR -> {
                BackHandler { screen = Screen.HOME }
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
                BackHandler { screen = Screen.HOME }
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
                BackHandler { screen = Screen.SETTINGS }
                RoundrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
