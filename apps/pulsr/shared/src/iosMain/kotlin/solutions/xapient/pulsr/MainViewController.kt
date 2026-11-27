package solutions.xapient.pulsr

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues
import solutions.xapient.pulsr.ui.PulsrAccent

// ein Metronom pro Prozess (wie Android), ueberlebt das Neuaufbauen der Oberflaeche
private val store: PulseStore by lazy {
    val prefs = Prefs(UserDefaultsKeyValues("pulsr."))
    PulseStore({ prefs.settings }, { prefs.settings = it }, IosAudioEngine())
}

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
            StoreGated(PulsrAccent, APP_NAME, bridge, debugTrial) {
                PulsrApp(store, versionName)
            }
        }
    }
