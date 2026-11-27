package solutions.xapient.focusr

import androidx.compose.runtime.Composable
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.focusr.domain.Mode
import solutions.xapient.focusr.domain.Scale
import solutions.xapient.focusr.domain.Timer
import solutions.xapient.focusr.ui.FocusrAbout
import solutions.xapient.focusr.ui.FocusrAccent
import solutions.xapient.focusr.ui.FocusrSettings
import solutions.xapient.focusr.ui.TimerScreen

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    IDLE, RUNNING, DRAG, LAST_MINUTE, PAUSED, DONE, SETTINGS, ZERO, SCALE120, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

private const val NOW = 1_700_000_000_000L
private const val MIN = Timer.MINUTE

private fun running(left: Long, set: Long) = Timer(set, Mode.RUNNING, NOW + left, 0)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(FocusrAccent) {
        when (scene) {
            Scene.IDLE -> Screen(Timer(25 * MIN))
            Scene.RUNNING -> Screen(running(17 * MIN + 42_000, 25 * MIN))
            Scene.DRAG -> Screen(Timer(25 * MIN), drag = 40 * MIN)
            Scene.LAST_MINUTE -> Screen(running(42_000, 5 * MIN), pulse = 0.8f)
            Scene.PAUSED -> Screen(Timer(25 * MIN, Mode.PAUSED, 0, 9 * MIN + 15_000))
            Scene.DONE -> Screen(Timer(25 * MIN, Mode.RUNNING, NOW - 1000, 0))
            Scene.SETTINGS -> FocusrSettings(
                Scale.MIN60, {}, true, {}, true, {}, true, {}, true, {},
                notificationsMissing = true, onNotifications = {}, onAbout = {}, onBack = {},
                countdownNotification = false,
            )
            Scene.ZERO -> Screen(Timer(0))
            Scene.SCALE120 -> Screen(running(95 * MIN, 110 * MIN), Scale.MIN120)
            Scene.ABOUT -> FocusrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Screen(t: Timer, scale: Scale = Scale.MIN60, drag: Long? = null, pulse: Float = 0f) = TimerScreen(
    t, scale, haptics = null, onSet = {}, onStart = {}, onPause = {}, onReset = {}, onSettings = {},
    endText = { "14:35" }, clock = { NOW }, animate = false, dragPreview = drag, pulseAt = pulse,
)

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
