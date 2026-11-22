package solutions.xapient.dozr

import android.Manifest
import android.app.Activity
import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.ui.DozrAbout
import solutions.xapient.dozr.ui.DozrAccent
import solutions.xapient.dozr.ui.DozrSettings
import solutions.xapient.dozr.ui.SleepScreen
import java.util.Date

private enum class Screen { TIMER, SETTINGS, ABOUT }

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

// startRequests: Zaehler der Start-Wuensche aus der Kachel
@Composable
fun DozrApp(startRequests: Int = 0) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val store = remember { SleepStore.get(context) }
    val sleep by store.state.collectAsState()
    var fade by remember { mutableStateOf(prefs.fade) }
    var shake by remember { mutableStateOf(prefs.shake) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var screen by rememberSaveable { mutableStateOf(Screen.TIMER) }
    var notificationsOk by remember { mutableStateOf(notificationsAllowed(context)) }

    // Recht kann in den Systemeinstellungen geaendert werden; Lautstaerke von einem haengengebliebenen Ende zurueck
    LifecycleResumeEffect(Unit) {
        notificationsOk = notificationsAllowed(context)
        SleepService.restorePending(context)
        onPauseOrDispose { }
    }

    // Dienst haengt sich an jeden Lauf, auch nach Prozess-Tod (App ist hier im Vordergrund)
    LaunchedEffect(sleep.mode) {
        if (sleep.mode == Mode.RUNNING && !SleepService.alive) SleepService.start(context)
    }

    fun rationale(): Boolean =
        Build.VERSION.SDK_INT >= 33 && context.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true

    fun openNotificationSettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        } catch (_: ActivityNotFoundException) {
            // keine Einstellungsseite: nichts tun
        }
    }

    // Ablehnung erkennen: Hinweis war da und ist nach dem Nein weg = System fragt nie mehr
    var rationaleBefore by remember { mutableStateOf(false) }
    var startAfterAsk by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOk = notificationsAllowed(context)
        if (!granted && rationaleBefore && !rationale()) prefs.notifyBlocked = true
        if (startAfterAsk) {
            startAfterAsk = false
            store.start()
            SleepService.start(context)
        }
    }

    fun ask(thenStart: Boolean) {
        rationaleBefore = rationale()
        startAfterAsk = thenStart
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Vor dem Start fragen, solange nicht erteilt und nicht schon einmal abgelehnt; weggetippt = beim naechsten Start wieder
    fun start() {
        SleepService.restorePending(context)
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed(context) && !rationale() && !prefs.notifyBlocked) {
            ask(thenStart = true)
            return
        }
        store.start()
        SleepService.start(context)
    }

    // Kachel-Start, wenn das System den Dienst aus der Kachel abgelehnt hat
    LaunchedEffect(startRequests) {
        if (startRequests > 0 && store.state.value.mode != Mode.RUNNING) start()
    }

    AappsTheme(DozrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        when (screen) {
            Screen.TIMER -> SleepScreen(
                sleep = sleep,
                fadeMs = fade.ms,
                haptics = haptics,
                onChoose = store::choose,
                onStart = ::start,
                onStop = store::stop,
                onExtend = store::extend,
                onSettings = { screen = Screen.SETTINGS },
                wallText = { at ->
                    DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + (at - store.now())))
                },
            )
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.TIMER }
                DozrSettings(
                    fade = fade,
                    onFade = { fade = it; prefs.fade = it },
                    shake = shake,
                    onShake = { shake = it; prefs.shake = it },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    notificationsMissing = !notificationsOk,
                    onNotifications = {
                        if (Build.VERSION.SDK_INT >= 33 && !prefs.notifyBlocked) ask(thenStart = false) else openNotificationSettings()
                    },
                    onAddTile = if (Build.VERSION.SDK_INT >= 33) {
                        { addTile(context) }
                    } else {
                        null
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.TIMER },
                )
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                DozrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}

// System-Dialog "Kachel hinzufuegen" (ab Android 13)
private fun addTile(context: Context) {
    if (Build.VERSION.SDK_INT < 33) return
    val sbm = context.getSystemService(StatusBarManager::class.java) ?: return
    sbm.requestAddTileService(
        DozrTile.component(context),
        context.getString(R.string.app_name),
        Icon.createWithResource(context, R.drawable.ic_stat_dozr),
        ContextCompat.getMainExecutor(context),
    ) { }
}
