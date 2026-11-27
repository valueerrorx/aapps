package solutions.xapient.hudr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.ui.StoreGated
import solutions.xapient.aapps.core.ui.UserDefaultsKeyValues
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.LocState

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
            StoreGated(HudrAccent, APP_NAME, bridge, debugTrial) { IosHudr(versionName) }
        }
    }

// Standort-Recht "Beim Verwenden der App": fragen, genauen Standort fuer die Sitzung anfragen, sonst Einstellungen
@Composable
private fun IosHudr(versionName: String) {
    val prefs = remember { Prefs(UserDefaultsKeyValues("hudr.")) { NSLocale.currentLocale.countryCode ?: "" } }
    val gps = remember { IosGps() }
    val beep = remember { IosBeep() }
    // Stand der Rechte neu lesen: nach Rueckfrage, Rueckkehr aus den Einstellungen, abgelehntem Start
    var checks by remember { mutableIntStateOf(0) }
    DisposableEffect(gps) {
        gps.onAuthorization = { checks++ }
        onDispose { gps.onAuthorization = null }
    }
    LifecycleResumeEffect(Unit) {
        checks++
        onPauseOrDispose { }
    }
    val loc = remember(checks) { gps.locState() }
    // beim allerersten Start einmal von selbst fragen
    LaunchedEffect(Unit) {
        if (gps.notDetermined && !prefs.askedLocation) {
            prefs.askedLocation = true
            gps.requestWhenInUse()
        }
    }
    HudrApp(
        prefs = prefs,
        gps = gps,
        loc = loc,
        onLoc = {
            when (loc) {
                LocState.ASK -> {
                    prefs.askedLocation = true
                    gps.requestWhenInUse()
                }
                LocState.COARSE -> gps.requestPrecise()
                LocState.BLOCKED, LocState.GPS_OFF -> openSettings()
                LocState.READY -> Unit
            }
        },
        now = ::uptimeNanos,
        beep = beep::play,
        versionName = versionName,
        onGpsFailed = { checks++ },
        hudWindow = { on, bright -> HudWindow(on, bright) },
    )
}

// App-Einstellungen; zu den Ortungsdiensten selbst laesst iOS nicht springen
private fun openSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
}

// HUD: auf Wunsch volle Helligkeit, danach der vorige Wert; Leisten und Drehung regelt iOS selbst
@Composable
private fun HudWindow(on: Boolean, bright: Boolean) {
    DisposableEffect(on, bright) {
        val screen = UIScreen.mainScreen
        val before = screen.brightness
        if (on && bright) screen.brightness = 1.0
        onDispose { if (on && bright) screen.brightness = before }
    }
}
