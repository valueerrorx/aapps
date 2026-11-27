package solutions.xapient.hudr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.ui.HudScreen
import solutions.xapient.hudr.ui.HudrAbout
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.HudrSettings
import solutions.xapient.hudr.ui.LocState
import solutions.xapient.hudr.ui.SpeedScreen

// Feste Zustaende fuer App-Store-Screenshots (Simulator faehrt nicht), wie die Roborazzi-Tests auf Android
internal enum class Scene {
    DRIVE, NEAR, OVER, STOPPED, SEARCHING, LOST, HUD, HUD_OVER, LOC_ASK, LOC_COARSE, LOC_BLOCKED, GPS_OFF,
    SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// iOS nennt keine Satelliten: 0/0 blendet die Anzeige aus
private fun fix(speed: Float, max: Float, avg: Float, dist: Double, trend: Float = Float.NaN, alert: Alert = Alert.NONE, phase: GpsPhase = GpsPhase.FIX) =
    Reading(phase, if (phase == GpsPhase.FIX) speed else Float.NaN, speed > 0f, 4.2f, 0, 0, max, avg, dist, alert, trend)

private val drive = fix(87.4f, 112f, 64f, 23.42, trend = 96f)
private val near = fix(47f, 52f, 38f, 4.8, trend = 50f, alert = Alert.NEAR)
private val over = fix(58f, 61f, 41f, 6.1, trend = 55f, alert = Alert.OVER)
private val stopped = fix(0f, 71f, 43f, 12.7)
private val lost = fix(52f, 74f, 49f, 9.3, phase = GpsPhase.LOST)
private val limit50 = Settings(limitOn = true, limit = 50)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(HudrAccent) {
        when (scene) {
            Scene.DRIVE -> Main(drive)
            Scene.NEAR -> Main(near, limit50)
            Scene.OVER -> Main(over, limit50)
            Scene.STOPPED -> Main(stopped)
            Scene.SEARCHING -> Main(Reading.SEARCHING)
            Scene.LOST -> Main(lost)
            Scene.HUD -> Hud(drive)
            Scene.HUD_OVER -> Hud(over, limit50)
            Scene.LOC_ASK -> Main(Reading.SEARCHING, loc = LocState.ASK)
            Scene.LOC_COARSE -> Main(Reading.SEARCHING, loc = LocState.COARSE)
            Scene.LOC_BLOCKED -> Main(Reading.SEARCHING, loc = LocState.BLOCKED)
            Scene.GPS_OFF -> Main(Reading.SEARCHING, loc = LocState.GPS_OFF)
            Scene.SETTINGS -> HudrSettings(limit50, {}, null, {}, {})
            Scene.ABOUT -> HudrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Main(r: Reading, s: Settings = Settings(), loc: LocState = LocState.READY) {
    SpeedScreen(
        { r }, remember { mutableIntStateOf(0) }, loc, s, null,
        onHud = {}, onReset = {}, onLoc = {}, onSettings = {}, animate = false,
    )
}

@Composable
private fun Hud(r: Reading, s: Settings = Settings()) {
    HudScreen({ r }, remember { mutableIntStateOf(0) }, s, null, onExit = {}, animate = false)
}

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
