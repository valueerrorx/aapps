package solutions.xapient.sleepr

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues

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
            StoreGated(SleeprAccent, APP_NAME, bridge, debugTrial) {
                val mix = remember { Mix(UserDefaultsKeyValues("sleepr.mixer.")) }
                val display = remember { Display(UserDefaultsKeyValues("sleepr.app.")) }
                val player = remember { IosNoisePlayer(mix) }
                SleeprApp(mix, display, player, versionName)
            }
        }
    }
