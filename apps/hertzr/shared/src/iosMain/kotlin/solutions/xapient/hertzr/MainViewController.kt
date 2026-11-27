package solutions.xapient.hertzr

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.ui.HertzrAccent

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
            StoreGated(HertzrAccent, "hertzr", bridge, debugTrial) {
                val dec = decimalSeparator()
                val store = remember { iosStore(dec) }
                HertzrContent(store, IosToneEngine.RATE, versionName, onToggle = store::toggle)
            }
        }
    }

// Ein Generator pro Prozess; Einstellungen in UserDefaults, Schutz und Sperrbildschirm ueber IosAudioGuard
private fun iosStore(decimal: Char): ToneStore = ToneStore.current ?: run {
    val prefs = Prefs(UserDefaultsKeyValues("hertzr."))
    ToneStore({ prefs.settings }, { prefs.settings = it }, IosToneEngine(), Freq.maxFor(IosToneEngine.RATE)).also {
        ToneStore.install(it)
        IosAudioGuard(it, decimal)
    }
}
