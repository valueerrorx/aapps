package solutions.xapient.pulsr

import androidx.compose.runtime.Composable
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.Subdivision
import solutions.xapient.pulsr.ui.PulseScreen
import solutions.xapient.pulsr.ui.PulsrAbout
import solutions.xapient.pulsr.ui.PulsrAccent
import solutions.xapient.pulsr.ui.PulsrSettings

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    ACCENT, IDLE, WALTZ, FAST, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

private val idle = Settings()
private val accent = Settings(bpm = 96, beats = 4, sub = Subdivision.EIGHTH)
private val waltz = Settings(bpm = 72, beats = 3, sub = Subdivision.TRIPLET)
private val fast = Settings(bpm = 220, beats = 7, sub = Subdivision.SIXTEENTH)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(PulsrAccent) {
        when (scene) {
            Scene.ACCENT -> Screen(accent, pulse(accent, 0, 0.12f))
            Scene.IDLE -> Screen(idle)
            Scene.WALTZ -> Screen(waltz, pulse(waltz, 2, 0.4f))
            Scene.FAST -> Screen(fast, pulse(fast, 3, 0.3f))
            Scene.SETTINGS -> PulsrSettings(idle, {}, {}, {})
            Scene.ABOUT -> PulsrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

// Puls als Standbild: Schlag, Phase im Schlag, Tempo
private fun pulse(s: Settings, beat: Int, phase: Float): Pulse {
    val beatMs = 60_000f / s.bpm
    val subPos = phase * s.sub.count
    val si = subPos.toInt()
    return Pulse(
        beat = beat, beats = s.beats, phase = phase, sub = s.sub.count, subIndex = si,
        accent = s.accent && beat == 0, sinceBeatMs = phase * beatMs,
        sinceSubMs = (subPos - si) / s.sub.count * beatMs, beatMs = beatMs,
    )
}

@Composable
private fun Screen(s: Settings, p: Pulse? = null) = PulseScreen(
    s, playing = p != null, pulse = { p }, haptics = null,
    onNudge = { false }, onBpm = {}, onTap = {}, onBeats = {}, onSub = {}, onToggle = {}, onSettings = {}, animate = false,
)

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
