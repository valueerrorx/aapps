package solutions.xapient.dividr

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.dividr.ui.DividrAccent

/**
 * Einstieg fuer Swift. [host] = Kamera und Teilen (Swift), [scene] = Screenshot-Zustand (Startargument -aappsScene),
 * [debugTrial] = expire|reset (nur Debug-Builds), [versionName] = CFBundleShortVersionString.
 */
fun MainViewController(bridge: StoreBridge, host: DividrHost, versionName: String, scene: String?, debugTrial: String?): UIViewController =
    ComposeUIViewController {
        CompositionLocalProvider(LocalDividrHost provides host) {
            val demo = remember { Scene.of(scene) }
            if (demo != null) {
                SceneContent(demo, versionName)
            } else {
                StoreGated(DividrAccent, APP_NAME, bridge, debugTrial) {
                    val store = remember { IosStore() }
                    DividrApp(store, versionName)
                }
            }
        }
    }
