package solutions.xapient.roundr

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.UIKit.UIViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.roundr.domain.Workout
import solutions.xapient.roundr.ui.RoundrAccent

// Ein Lauf pro Prozess
private var shared: Pair<RunStore, RunDriver>? = null

private fun runtime(prefs: IosPrefs): Pair<RunStore, RunDriver> = shared ?: run {
    val store = RunStore({ prefs.run }, { prefs.run = it })
    (store to RunDriver(store, prefs)).also { shared = it }
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
            StoreGated(RoundrAccent, APP_NAME, bridge, debugTrial) {
                val prefs = remember { IosPrefs() }
                val (store, driver) = remember { runtime(prefs) }
                LifecycleResumeEffect(driver) {
                    driver.active = true
                    onPauseOrDispose { driver.active = false }
                }
                val start = remember(store) { startAsking(prefs, store) }
                RoundrApp(prefs, store, versionName, start = start, iosNote = true)
            }
        }
    }

// Mitteilungen einmal VOR dem ersten Start erfragen; gestartet wird danach, der Timer laeuft auch ohne
private fun startAsking(prefs: IosPrefs, store: RunStore): (Workout) -> Unit = { w ->
    if (prefs.askedNotify) {
        store.start(w)
    } else {
        prefs.askedNotify = true
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ ->
                dispatch_async(dispatch_get_main_queue()) { store.start(w) }
            }
    }
}
