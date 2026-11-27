package solutions.xapient.focusr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.focusr.domain.Scale
import solutions.xapient.focusr.domain.Timer
import solutions.xapient.focusr.ui.FocusrAbout
import solutions.xapient.focusr.ui.FocusrAccent
import solutions.xapient.focusr.ui.FocusrSettings
import solutions.xapient.focusr.ui.TimerScreen

const val APP_NAME = "focusr"

/** Uhr des Timers in ms: Android Laufzeituhr (elapsedRealtime), iOS Wanduhr (laeuft auch im Ruhezustand weiter) */
expect fun timerClock(): Long

/** Abweisen (Start mit 0); Android View-Haptik REJECT, iOS Fehler-Haptik */
@Composable
expect fun rememberReject(enabled: Boolean): () -> Unit

/** Timer und Einstellungen; Android SharedPreferences, iOS UserDefaults */
interface FocusrPrefs {
    var timer: Timer
    var scale: Scale
    var sound: Boolean
    var vibrate: Boolean
    var haptics: Boolean
    var keepOn: Boolean
}

/** Plattform: Ende auch im Hintergrund melden, Uhrzeit anzeigen */
interface FocusrHost {
    /** Alarm bzw. Benachrichtigung passend zum Timer setzen oder loeschen */
    fun sync(timer: Timer)

    /** Uhrzeit in [inMs] Millisekunden, fuer „Endet um" */
    fun endText(inMs: Long): String

    /** Android zeigt im Hintergrund einen laufenden Countdown, iOS nur das Ende */
    val countdownNotification: Boolean
}

/** Benachrichtigungsrecht: [ask] fragt einmal von selbst, aus den Einstellungen oeffnet es die Systemseite */
class NotificationAccess(val allowed: Boolean, val ask: (fromSettings: Boolean) -> Unit)

private enum class Screen { TIMER, SETTINGS, ABOUT }

// Gemeinsam fuer Android und iOS; Speicher, Alarm und Benachrichtigungen kommen von der Plattform
@Composable
fun FocusrApp(prefs: FocusrPrefs, host: FocusrHost, notifications: NotificationAccess, versionName: String) {
    var timer by remember { mutableStateOf(prefs.timer) }
    var scale by remember { mutableStateOf(prefs.scale) }
    var sound by remember { mutableStateOf(prefs.sound) }
    var vibrate by remember { mutableStateOf(prefs.vibrate) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var keepOn by remember { mutableStateOf(prefs.keepOn) }
    var screen by rememberSaveable { mutableStateOf(Screen.TIMER) }

    // Jede Aenderung sofort speichern und den Alarm nachziehen: uebersteht Prozess-Tod
    fun update(t: Timer) {
        timer = t
        prefs.timer = t
        host.sync(t)
    }

    AappsTheme(FocusrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        // Start mit 0: abweisen; Haptics aus core kennt keinen Reject
        val reject = rememberReject(hapticsOn)
        when (screen) {
            Screen.TIMER -> TimerScreen(
                timer = timer,
                scale = scale,
                haptics = haptics,
                onSet = { update(timer.set(it, timerClock())) },
                onStart = {
                    update(timer.start(timerClock()))
                    notifications.ask(false)
                },
                onPause = { update(timer.pause(timerClock())) },
                onReset = { update(timer.reset()) },
                onSettings = { screen = Screen.SETTINGS },
                // Endzeit nur als Anzeige ueber die Wanduhr
                endText = host::endText,
                keepOn = keepOn,
                onReject = reject,
            )
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.TIMER }
                FocusrSettings(
                    scale = scale,
                    onScale = {
                        scale = it
                        prefs.scale = it
                        update(timer.limitTo(it))
                    },
                    sound = sound,
                    onSound = { sound = it; prefs.sound = it; host.sync(timer) },
                    vibrate = vibrate,
                    onVibrate = { vibrate = it; prefs.vibrate = it },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    keepOn = keepOn,
                    onKeepOn = { keepOn = it; prefs.keepOn = it },
                    notificationsMissing = !notifications.allowed,
                    onNotifications = { notifications.ask(true) },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.TIMER },
                    countdownNotification = host.countdownNotification,
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                FocusrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
