package solutions.xapient.hertzr

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import solutions.xapient.hertzr.ui.HertzrAbout
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.HertzrSettings
import solutions.xapient.hertzr.ui.ToneActions
import solutions.xapient.hertzr.ui.ToneScreen

private enum class Screen { MAIN, SETTINGS, ABOUT }

@Composable
fun HertzrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val store = remember { ToneStore.get(context) }
    val settings by store.settings.collectAsState()
    val playing by store.playing.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
    val headphones = rememberHeadphones()

    // Recht fuer die Benachrichtigung einmal VOR dem ersten Start erfragen; gespielt wird auch ohne
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { store.start() }
    // liest den Store direkt: die gemerkten Aktionen sehen sonst einen alten Stand
    fun toggle() {
        if (store.playing.value) {
            store.stop()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotify &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            prefs.askedNotify = true
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        store.start()
    }

    // Dienst haelt den Ton bei ausgeschaltetem Bildschirm am Leben
    LaunchedEffect(playing) {
        if (playing) ToneService.start(context)
    }

    AappsTheme(HertzrAccent) {
        val haptics = rememberHaptics()
        when (screen) {
            Screen.MAIN -> ToneScreen(
                settings = settings,
                playing = playing,
                maxHz = store.maxHz,
                live = store::shownHz,
                headphones = headphones,
                haptics = haptics,
                actions = remember(store) {
                    ToneActions(
                        onHz = store::setHz,
                        onNudge = store::nudge,
                        onOctave = store::octave,
                        onWave = store::setWave,
                        onLevel = store::setLevel,
                        onMode = store::setMode,
                        onSweepFrom = store::setSweepFrom,
                        onSweepTo = store::setSweepTo,
                        onDuration = store::nextDuration,
                        onToggle = ::toggle,
                        onSettings = { screen = Screen.SETTINGS },
                    )
                },
            )
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.MAIN }
                HertzrSettings(settings, store::setOutput, store::setKeepOn, onAbout = { screen = Screen.ABOUT }, onBack = { screen = Screen.MAIN })
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                HertzrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}

// Kopfhoerer (Kabel, USB, Bluetooth) angeschlossen? Fuer den Pegel-Hinweis
@Composable
private fun rememberHeadphones(): Boolean {
    val context = LocalContext.current
    var on by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val audio = context.getSystemService(AudioManager::class.java)
        fun check() {
            on = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADPHONE_TYPES }
        }
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = check()
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = check()
        }
        audio.registerAudioDeviceCallback(cb, null)
        check()
        onDispose { audio.unregisterAudioDeviceCallback(cb) }
    }
    return on
}

private val HEADPHONE_TYPES = buildSet {
    add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
    add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
    add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
    add(AudioDeviceInfo.TYPE_USB_HEADSET)
    if (Build.VERSION.SDK_INT >= 31) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
}
