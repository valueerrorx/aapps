package solutions.xapient.loudr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues
import solutions.xapient.loudr.ui.LoudrAccent
import solutions.xapient.loudr.ui.MicState

/**
 * Einstieg fuer Swift. [scene] = Screenshot-Zustand (Startargument -aappsScene), [debugTrial] = expire|reset
 * (nur Debug-Builds), [versionName] = CFBundleShortVersionString.
 */
fun MainViewController(bridge: StoreBridge, versionName: String, scene: String?, debugTrial: String?): UIViewController =
    ComposeUIViewController {
        val demo = remember { Scene.of(scene) }
        if (demo != null) {
            SceneContent(demo, versionName)
        } else {
            StoreGated(LoudrAccent, "loudr", bridge, debugTrial) {
                LoudrIos(versionName)
            }
        }
    }

// iOS-Rahmen wie LoudrApp auf Android: einmal von selbst fragen, abgelehnt = Einstellungen-App, Mikrofon nur im Vordergrund
@Composable
private fun LoudrIos(versionName: String) {
    val prefs = remember { Prefs(UserDefaultsKeyValues("loudr.")) }
    val meter = remember { IosMicMeter() }
    var paused by rememberSaveable { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var measuring by remember { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(IosMicMeter.granted()) }
    fun denied() = AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionDenied
    var blocked by remember { mutableStateOf(denied()) }

    fun ask() {
        prefs.askedMic = true
        AVAudioApplication.requestRecordPermissionWithCompletionHandler { ok ->
            dispatch_async(dispatch_get_main_queue()) {
                hasMic = ok
                blocked = !ok && denied()
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!hasMic && !blocked && !prefs.askedMic) ask()
    }
    // Rueckkehr aus der Einstellungen-App
    LifecycleResumeEffect(Unit) {
        hasMic = IosMicMeter.granted()
        blocked = denied()
        onPauseOrDispose { }
    }
    DisposableEffect(meter) {
        meter.onFailure = { failed = true }
        onDispose { meter.onFailure = null }
    }
    LifecycleStartEffect(meter, hasMic, paused, failed) {
        measuring = hasMic && !paused && !failed && meter.start()
        if (hasMic && !paused && !failed && !measuring) failed = true
        onStopOrDispose {
            meter.stop()
            measuring = false
        }
    }

    val mic = when {
        !hasMic && blocked -> MicState.BLOCKED
        !hasMic -> MicState.ASK
        failed -> MicState.FAILED
        else -> MicState.READY
    }
    fun onMic() {
        when (mic) {
            MicState.ASK -> ask()
            MicState.BLOCKED -> NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null)
            }
            MicState.FAILED -> failed = false
            MicState.READY, MicState.SILENCED -> Unit
        }
    }
    LoudrContent(meter, prefs, mic, measuring, versionName, onToggle = { paused = !paused }, onMic = ::onMic)
}
