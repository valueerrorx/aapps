package solutions.xapient.thinkr

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues
import solutions.xapient.thinkr.ui.Motion
import solutions.xapient.thinkr.ui.ThinkrAccent

/**
 * Einstieg fuer Swift. [scene] = Screenshot-Zustand (Startargument -aappsScene), [debugTrial] = expire|reset
 * (nur Debug-Builds), [versionName] = CFBundleShortVersionString.
 */
fun MainViewController(bridge: StoreBridge, versionName: String, scene: String?, debugTrial: String?): UIViewController =
    ComposeUIViewController {
        // „Bewegung reduzieren" bei jedem Zurueckkehren neu lesen (wie Android onResume)
        LifecycleResumeEffect(Unit) {
            Motion.reduced = UIAccessibilityIsReduceMotionEnabled()
            onPauseOrDispose {}
        }
        val demo = remember { Scene.of(scene) }
        if (demo != null) {
            SceneContent(demo, versionName)
        } else {
            StoreGated(ThinkrAccent, APP_NAME, bridge, debugTrial) {
                val prefs = remember { Prefs(UserDefaultsKeyValues("thinkr.")) }
                ThinkrApp(prefs, { IosTonePlayer() }, versionName)
            }
        }
    }
