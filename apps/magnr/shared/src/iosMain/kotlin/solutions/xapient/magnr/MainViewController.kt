package solutions.xapient.magnr

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues
import solutions.xapient.magnr.ui.MagnrAccent

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
            StoreGated(MagnrAccent, APP_NAME, bridge, debugTrial) {
                val prefs = remember { Prefs(UserDefaultsKeyValues("magnr.")) }
                MagnrApp(
                    prefs = prefs,
                    newSource = { IosFieldSource() },
                    newBeeper = { IosBeeper() },
                    // Zeit seit Geraetestart wie die CoreMotion-Zeitstempel
                    elapsedMillis = { (NSProcessInfo.processInfo.systemUptime * 1000).toLong() },
                    versionName = versionName,
                )
            }
        }
    }
