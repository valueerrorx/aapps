package solutions.xapient.focusr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.focusr.domain.Timer
import solutions.xapient.focusr.ui.FocusrAbout
import solutions.xapient.focusr.ui.FocusrAccent
import solutions.xapient.focusr.ui.FocusrSettings
import solutions.xapient.focusr.ui.TimerScreen
import java.util.Date

private enum class Screen { TIMER, SETTINGS, ABOUT }

@Composable
fun FocusrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var timer by remember { mutableStateOf(prefs.timer) }
    var scale by remember { mutableStateOf(prefs.scale) }
    var sound by remember { mutableStateOf(prefs.sound) }
    var vibrate by remember { mutableStateOf(prefs.vibrate) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var keepOn by remember { mutableStateOf(prefs.keepOn) }
    var screen by rememberSaveable { mutableStateOf(Screen.TIMER) }
    var notificationsOk by remember { mutableStateOf(Notifier.allowed(context)) }

    // Recht kann in den Systemeinstellungen geaendert werden
    LifecycleResumeEffect(Unit) {
        notificationsOk = Notifier.allowed(context)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsOk = Notifier.allowed(context)
    }

    // Jede Aenderung sofort speichern und den Alarm nachziehen: uebersteht Prozess-Tod
    fun update(t: Timer) {
        timer = t
        prefs.timer = t
        EndAlarm.sync(context, t)
    }

    // einmal von selbst fragen, danach nur ueber die Einstellungen
    fun askNotifications(fromSettings: Boolean) {
        if (notificationsOk) return
        if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotifications) {
            prefs.askedNotifications = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (fromSettings) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            } catch (_: ActivityNotFoundException) {
                // keine Einstellungsseite: nichts tun
            }
        }
    }

    AappsTheme(FocusrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        val view = LocalView.current
        // Start mit 0: abweisen; Haptics aus core kennt keinen Reject
        val reject = {
            if (hapticsOn) {
                view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
                )
            }
        }
        when (screen) {
            Screen.TIMER -> TimerScreen(
                timer = timer,
                scale = scale,
                haptics = haptics,
                onSet = { update(timer.set(it, SystemClock.elapsedRealtime())) },
                onStart = {
                    update(timer.start(SystemClock.elapsedRealtime()))
                    askNotifications(fromSettings = false)
                },
                onPause = { update(timer.pause(SystemClock.elapsedRealtime())) },
                onReset = { update(timer.reset()) },
                onSettings = { screen = Screen.SETTINGS },
                // Endzeit nur als Anzeige ueber die Wanduhr
                endText = { DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + it)) },
                keepOn = keepOn,
                onReject = reject,
            )
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.TIMER }
                FocusrSettings(
                    scale = scale,
                    onScale = {
                        scale = it
                        prefs.scale = it
                        update(timer.limitTo(it))
                    },
                    sound = sound,
                    onSound = { sound = it; prefs.sound = it },
                    vibrate = vibrate,
                    onVibrate = { vibrate = it; prefs.vibrate = it },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    keepOn = keepOn,
                    onKeepOn = { keepOn = it; prefs.keepOn = it },
                    notificationsMissing = !notificationsOk,
                    onNotifications = { askNotifications(fromSettings = true) },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.TIMER },
                )
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                FocusrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
