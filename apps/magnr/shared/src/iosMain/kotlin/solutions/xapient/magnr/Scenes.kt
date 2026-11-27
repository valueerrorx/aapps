package solutions.xapient.magnr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.magnr.domain.FieldMeter
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.domain.Reading
import solutions.xapient.magnr.domain.Sensitivity
import solutions.xapient.magnr.ui.DetectorScreen
import solutions.xapient.magnr.ui.DetectorState
import solutions.xapient.magnr.ui.Hint
import solutions.xapient.magnr.ui.MagnrAbout
import solutions.xapient.magnr.ui.MagnrAccent
import solutions.xapient.magnr.ui.MagnrSettings
import kotlin.math.exp
import kotlin.math.sin

// Feste Zustaende fuer App-Store-Screenshots (Simulator hat kein Magnetometer), wie die Roborazzi-Tests auf Android
internal enum class Scene {
    NEAR, QUIET, STRONG, SATURATED, PAUSED, INTRO, CALIBRATE, NO_SENSOR, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// Verlauf: ruhiges Rauschen, dann ein Schwenk ueber einen Fund (Glocke), deterministisch
private fun sweep(peakAt: Int, height: Float, n: Int = 200, noise: Float = 0.03f) = FloatArray(n) { i ->
    val d = (i - peakAt) / 14f
    val bump = height * exp(-d * d)
    (bump + noise * (0.5f + 0.5f * sin(i / 3.1f) * sin(i / 7.7f))).coerceIn(0f, 1f)
}

private val near = Reading(
    total = 69.2f, baseline = 47.9f, dev = 21.3f, strength = Sensitivity.MEDIUM.strength(21.3f),
    level = Level.NEAR, peak = 24.6f, history = sweep(120, 0.62f).let { a -> FloatArray(a.size) { maxOf(a[it], sweep(206, 0.56f)[it]) } },
)
private val quiet = Reading(
    total = 48.2f, baseline = 47.9f, dev = 0.3f, strength = 0f, level = Level.QUIET, peak = 0.6f, history = sweep(60, 0.18f),
)
private val strong = Reading(
    total = 61.4f, baseline = 48.0f, dev = 13.4f, strength = 1f, level = Level.STRONG, peak = 14.1f,
    history = sweep(196, 1f).also { for (i in 0 until 120) it[i] = sweep(70, 0.45f)[i] },
)
private val saturated = Reading(
    total = 4912f, baseline = 48.1f, dev = 4863.9f, strength = 1f, level = Level.STRONG, peak = 4863.9f,
    history = sweep(199, 1f), saturated = true,
)
// pausiert, mit Luecke einer frueheren Pause
private val paused = Reading(
    total = 52.3f, baseline = 47.9f, dev = 4.4f, strength = Sensitivity.MEDIUM.strength(4.4f),
    level = Level.QUIET, peak = 4.4f, history = sweep(110, 0.7f).also { for (i in 60..64) it[i] = Float.NaN },
)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(MagnrAccent) {
        when (scene) {
            Scene.NEAR -> Detector(near)
            Scene.QUIET -> Detector(quiet)
            Scene.STRONG -> Detector(strong, sensitivity = Sensitivity.HIGH, sound = true)
            Scene.SATURATED -> Detector(saturated, sound = true)
            Scene.PAUSED -> Detector(paused, live = false)
            Scene.INTRO -> Detector(quiet, hint = Hint.INTRO)
            Scene.CALIBRATE -> Detector(quiet, hint = Hint.CALIBRATE)
            Scene.NO_SENSOR -> Detector(null, live = false, available = false)
            Scene.SETTINGS -> MagnrSettings(Settings(sound = true), {}, {}, {}, {}, {})
            Scene.ABOUT -> MagnrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Detector(
    r: Reading?,
    live: Boolean = true,
    sensitivity: Sensitivity = Sensitivity.MEDIUM,
    sound: Boolean = false,
    hint: Hint? = null,
    available: Boolean = true,
) {
    val state = remember {
        DetectorState(FieldMeter()).apply {
            fixed = r
            when (hint) {
                Hint.INTRO -> introVisible = true
                Hint.CALIBRATE -> showCalibration()
                null -> Unit
            }
        }
    }
    DetectorScreen(
        state, sensitivity, available, live, sound, keepOn = false, haptics = null,
        onPause = {}, onZero = {}, onSound = {}, onSettings = {}, onIntro = {}, hintPhase = 0.3f,
    )
}

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
