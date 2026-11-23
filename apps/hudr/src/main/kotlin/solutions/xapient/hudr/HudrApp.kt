package solutions.xapient.hudr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.hudr.domain.Speedo
import solutions.xapient.hudr.ui.HudScreen
import solutions.xapient.hudr.ui.HudrAbout
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.HudrSettings
import solutions.xapient.hudr.ui.LocState
import solutions.xapient.hudr.ui.SpeedScreen
import solutions.xapient.hudr.ui.locState

private enum class Screen { MAIN, SETTINGS, ABOUT }

private const val FINE = Manifest.permission.ACCESS_FINE_LOCATION
private const val COARSE = Manifest.permission.ACCESS_COARSE_LOCATION
// Anfrage ohne Dialog kommt binnen weniger Millisekunden zurueck
private const val INSTANT_NS = 300_000_000L

@Composable
fun HudrApp(
    newGps: (Context) -> GpsSource = ::AndroidGps,
    now: () -> Long = SystemClock::elapsedRealtimeNanos,
    beep: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val prefs = remember { Prefs(context) }
    var settings by remember { mutableStateOf(prefs.settings) }
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    var hud by rememberSaveable { mutableStateOf(false) }
    // Fahrtwerte ueberstehen Prozess-Tod
    val speedo = rememberSaveable(saver = Saver<Speedo, DoubleArray>({ it.save() }, { s -> Speedo().apply { restore(s) } })) { Speedo() }
    val gps = remember { newGps(context) }
    // version: neu zeichnen (nur in der Zeichenphase gelesen); fixes: Zeitgeber fuer "Signal verloren"
    val version = remember { mutableIntStateOf(0) }
    val fixes = remember { mutableIntStateOf(0) }

    fun has(p: String) = context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
    // "nie wieder fragen": nach echter Ablehnung (Begruendung war schon gefragt) zeigt das System keine mehr; weggetippt bleibt fragbar
    fun blockedNow(): Boolean {
        if (has(FINE)) return false
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, FINE) } == true
        if (rationale) prefs.rationaleSeen = true
        return prefs.rationaleSeen && !rationale
    }
    var fine by remember { mutableStateOf(has(FINE)) }
    var coarse by remember { mutableStateOf(has(COARSE)) }
    var blocked by remember { mutableStateOf(blockedNow()) }
    var gpsOn by remember { mutableStateOf(gps.enabled()) }
    var upgradeRefused by remember { mutableStateOf(prefs.upgradeRefused) }
    fun refresh() {
        fine = has(FINE)
        coarse = has(COARSE)
        blocked = blockedNow()
        gpsOn = gps.enabled()
        if (fine && upgradeRefused) {
            upgradeRefused = false
            prefs.upgradeRefused = false
        }
    }
    // Zeitpunkt der Anfrage: kommt sie ohne Dialog sofort zurueck, hat das System das Upgrade gesperrt
    val askedAt = remember { longArrayOf(0L) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        prefs.askedLocation = true
        refresh()
        if (!fine && coarse && now() - askedAt[0] < INSTANT_NS) {
            upgradeRefused = true
            prefs.upgradeRefused = true
        }
    }
    fun request() {
        askedAt[0] = now()
        ask.launch(arrayOf(FINE, COARSE))
    }
    // beim allerersten Start einmal von selbst fragen
    LaunchedEffect(Unit) {
        if (!fine && !prefs.askedLocation) request()
    }
    // Rueckkehr aus den App- oder Standort-Einstellungen
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }
    // Standort zur Laufzeit an/aus (Schnelleinstellungen), solange sichtbar
    LifecycleStartEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = refresh()
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }

    val loc = locState(fine, coarse, gpsOn, blocked, upgradeRefused)
    val ready = loc == LocState.READY

    // Ton beim Ueberschreiten; ToneGenerator braucht keine Berechtigung
    val tone = remember { lazy { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }.getOrNull() } }
    DisposableEffect(Unit) {
        onDispose { if (tone.isInitialized()) tone.value?.release() }
    }
    val current by rememberUpdatedState(settings)
    DisposableEffect(gps, speedo) {
        gps.onFix = { f ->
            val s = current
            if (speedo.onFix(f, s.unit, s.activeLimit) && s.sound) {
                beep?.invoke() ?: tone.value?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            }
            fixes.intValue++
            version.intValue++
        }
        // nur bei geaenderter Satellitenzahl neu zeichnen
        gps.onSatellites = { used, visible ->
            if (speedo.onSatellites(used, visible)) version.intValue++
        }
        gps.onEnabled = { gpsOn = it }
        onDispose {
            gps.onFix = null
            gps.onSatellites = null
            gps.onEnabled = null
        }
    }
    // Standort nur zwischen ON_START und ON_STOP, nie im Hintergrund
    LifecycleStartEffect(gps, ready) {
        if (ready) {
            speedo.restart()
            version.intValue++
            if (!gps.start()) gpsOn = gps.enabled()
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

    // HUD: Leisten weg, Drehung gesperrt (flach am Armaturenbrett), auf Wunsch volle Helligkeit; beim Verlassen zurueck
    val window = activity?.window
    DisposableEffect(window, hud && ready, settings.bright) {
        val on = hud && ready
        val orientation = activity?.requestedOrientation
        if (on) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        if (window != null && on) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            if (settings.bright) window.attributes = window.attributes.apply { screenBrightness = 1f }
        }
        onDispose {
            if (on && orientation != null) activity.requestedOrientation = orientation
            if (window != null && on) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
            }
        }
    }

    fun onLoc() {
        val intent = when (loc) {
            LocState.ASK, LocState.COARSE -> return request()
            LocState.BLOCKED -> Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            LocState.GPS_OFF -> Intent(AndroidSettings.ACTION_LOCATION_SOURCE_SETTINGS)
            LocState.READY -> return
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // keine Einstellungs-App: nichts zu tun
        }
    }
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
                BackHandler { hud = false }
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
                onLoc = ::onLoc,
                onSettings = { screen = Screen.SETTINGS },
            )
            screen == Screen.SETTINGS -> {
                BackHandler { screen = Screen.MAIN }
                HudrSettings(settings, ::change, haptics, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.MAIN })
            }
            else -> {
                BackHandler { screen = Screen.SETTINGS }
                HudrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
