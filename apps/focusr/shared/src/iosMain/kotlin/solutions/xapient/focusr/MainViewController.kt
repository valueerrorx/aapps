package solutions.xapient.focusr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.focusr.ui.FocusrAccent

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
            StoreGated(FocusrAccent, APP_NAME, bridge, debugTrial) {
                val prefs = remember { IosPrefs() }
                // Benachrichtigung fuer einen laufenden Timer sicherheitshalber neu setzen (z. B. nach Update)
                val host = remember { IosHost(prefs).also { it.sync(prefs.timer) } }
                LifecycleResumeEffect(host) {
                    host.active = true
                    host.clearDelivered()
                    onPauseOrDispose { host.active = false }
                }
                FocusrApp(prefs, host, rememberNotificationAccess(prefs), versionName)
            }
        }
    }

// Erst beim ersten Start des Timers fragen; danach liefert dieselbe Abfrage nur den Stand (ohne Dialog)
@Composable
private fun rememberNotificationAccess(prefs: IosPrefs): NotificationAccess {
    var ok by remember { mutableStateOf(false) }
    val center = remember { UNUserNotificationCenter.currentNotificationCenter() }
    val query = remember {
        {
            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
                dispatch_async(dispatch_get_main_queue()) { ok = granted }
            }
        }
    }
    // Recht kann in den Systemeinstellungen geaendert werden
    LifecycleResumeEffect(Unit) {
        if (prefs.askedNotifications) query()
        onPauseOrDispose { }
    }
    return NotificationAccess(ok) { fromSettings ->
        if (!ok) {
            if (!prefs.askedNotifications) {
                prefs.askedNotifications = true
                query()
            } else if (fromSettings) {
                NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                    UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null)
                }
            }
        }
    }
}
