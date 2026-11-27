package solutions.xapient.loudr

import androidx.compose.runtime.Composable
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.domain.TimeWeighting
import solutions.xapient.loudr.ui.LoudrAbout
import solutions.xapient.loudr.ui.LoudrAccent
import solutions.xapient.loudr.ui.LoudrSettings
import solutions.xapient.loudr.ui.MeterScreen
import solutions.xapient.loudr.ui.MicState
import kotlin.math.sin

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    TALK, QUIET, LOUD, PAUSED, MIC_ASK, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// Verlauf: ruhiger Grund mit Ausschlaegen, deterministisch
private fun history(base: Float, swing: Float, n: Int = 600) = FloatArray(n) { i ->
    val t = i.toFloat()
    base + swing * (0.55f * sin(t / 23f) + 0.3f * sin(t / 5.3f) + 0.15f * sin(t / 1.7f)) +
        if (i % 97 in 40..55) swing * 0.8f else 0f
}

private fun reading(level: Float, min: Float, max: Float, leq: Float, h: FloatArray, live: Boolean = true) =
    Reading(level, level - 1f, leq, min, max, min + 2f, max - 3f, h, live, false)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(LoudrAccent) {
        when (scene) {
            Scene.TALK -> Meter(reading(63.4f, 41f, 74f, 60.8f, history(58f, 9f)))
            Scene.QUIET -> Meter(reading(33f, 29f, 41f, 34f, history(33f, 4f)))
            Scene.LOUD -> Meter(Reading(93f, 90f, 88f, 80f, 102f, 84f, 99f, history(86f, 7f, 400), true), s = Settings(TimeWeighting.SLOW, 2f))
            Scene.PAUSED -> Meter(reading(71f, 44f, 83f, 66f, history(66f, 10f).also { it[380] = Float.NaN }, live = false), measuring = false)
            Scene.MIC_ASK -> Meter(null, MicState.ASK, measuring = false)
            Scene.SETTINGS -> LoudrSettings(Settings(offset = 1.5f), {}, { 64.5f }, null, {}, {})
            Scene.ABOUT -> LoudrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Meter(r: Reading?, mic: MicState = MicState.READY, measuring: Boolean = true, s: Settings = Settings()) {
    MeterScreen(
        { r ?: Reading.EMPTY }, mic, measuring, s, null,
        onToggle = {}, onReset = {}, onMic = {}, onSettings = {}, animate = false,
    )
}

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen("loudr", StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
