package solutions.xapient.tunr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
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
import solutions.xapient.tunr.audio.IosMic
import solutions.xapient.tunr.audio.IosTone
import solutions.xapient.tunr.domain.MicAccess
import solutions.xapient.tunr.ui.TunrAccent

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
            StoreGated(TunrAccent, "tunr", bridge, debugTrial) {
                TunrIos(versionName)
            }
        }
    }

// iOS-Rahmen wie TunrApp auf Android: einmal von selbst fragen, abgelehnt = Einstellungen-App (iOS fragt nicht erneut)
@Composable
private fun TunrIos(versionName: String) {
    val prefs = remember { Prefs(UserDefaultsKeyValues("tunr.")) }
    val mic = remember { IosMic() }
    val tone = remember { IosTone() }
    fun current(): MicAccess = when {
        IosMic.granted() -> MicAccess.GRANTED
        AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionDenied -> MicAccess.BLOCKED
        else -> MicAccess.ASK
    }
    var access by remember { mutableStateOf(current()) }
    fun request() {
        prefs.askedMic = true
        AVAudioApplication.requestRecordPermissionWithCompletionHandler { _ ->
            dispatch_async(dispatch_get_main_queue()) { access = current() }
        }
    }
    // zurueck aus der Einstellungen-App: neu pruefen
    LifecycleStartEffect(Unit) {
        access = current()
        onStopOrDispose { }
    }
    // beim ersten Oeffnen einmal fragen; das Werkzeug kann ohne Mikrofon nichts
    LaunchedEffect(Unit) {
        if (!prefs.askedMic && access == MicAccess.ASK) request()
    }
    TunrContent(prefs, mic, tone, access, versionName, onMicAction = {
        when (access) {
            MicAccess.ASK -> request()
            MicAccess.BLOCKED -> NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null)
            }
            MicAccess.GRANTED -> Unit
        }
    })
}
