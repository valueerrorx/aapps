package solutions.xapient.nordr

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues
import solutions.xapient.aapps.core.ui.nowMillis
import solutions.xapient.nordr.domain.Wmm

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
            StoreGated(NordrAccent, APP_NAME, bridge, debugTrial) {
                val prefs = remember { Prefs(UserDefaultsKeyValues("nordr."), NSTimeZone.localTimeZone.name) }
                val sensor = remember { IosSensorSource() }
                // Deklination aus dem eingebauten Weltmagnetfeld-Modell, ohne Standort
                NordrApp(prefs, sensor, { Wmm.declination(it.lat, it.lon, nowMillis()) }, versionName)
            }
        }
    }
