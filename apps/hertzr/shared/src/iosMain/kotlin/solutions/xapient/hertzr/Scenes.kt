package solutions.xapient.hertzr

import androidx.compose.runtime.Composable
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.hertzr.domain.Mode
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.domain.SweepSpec
import solutions.xapient.hertzr.domain.Wave
import solutions.xapient.hertzr.ui.HertzrAbout
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.HertzrSettings
import solutions.xapient.hertzr.ui.InputTarget
import solutions.xapient.hertzr.ui.ToneActions
import solutions.xapient.hertzr.ui.ToneScreen

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    IDLE, SQUARE, LOUD, SWEEP, INPUT, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

private val idle = Settings()
private val square = Settings(hz = 55.0, wave = Wave.SQUARE, levelDb = -14)
private val loud = Settings(hz = 12_000.0, wave = Wave.SAW, levelDb = -6)
private val sweep = Settings(mode = Mode.SWEEP, wave = Wave.TRIANGLE, sweepFrom = 20.0, sweepTo = 20_000.0, sweepSec = 10)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(HertzrAccent) {
        when (scene) {
            Scene.IDLE -> Tone(idle)
            Scene.SQUARE -> Tone(square, playing = true)
            Scene.LOUD -> Tone(loud, playing = true, headphones = true)
            Scene.SWEEP -> Tone(sweep, playing = true, live = SweepSpec(20.0, 20_000.0, 10).hzAt(0.4), progress = 0.4f)
            Scene.INPUT -> Tone(idle, input = InputTarget.HZ, text = "1500")
            Scene.SETTINGS -> HertzrSettings(idle, {}, {}, {}, {})
            Scene.ABOUT -> HertzrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Tone(
    s: Settings,
    playing: Boolean = false,
    live: Double = s.hz,
    headphones: Boolean = false,
    input: InputTarget = InputTarget.NONE,
    text: String = "",
    progress: Float? = null,
) {
    ToneScreen(
        s, playing, 22_000.0, { live }, headphones, haptics = null, actions = ToneActions(),
        animate = false, initialInput = input, initialText = text, progress = { progress },
    )
}

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen("hertzr", StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
