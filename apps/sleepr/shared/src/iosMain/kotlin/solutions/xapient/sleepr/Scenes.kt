package solutions.xapient.sleepr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.sleepr.ui.MixerScreen
import solutions.xapient.sleepr.ui.SleeprSettings

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    IDLE, IDLE_BOTTOM, PLAYING, OCEAN, PLAIN, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// Spielt nicht wirklich, zeigt nur den Zustand "laeuft" mit Restzeit
private class DemoPlayer(mix: Mix, override val playing: Boolean) : NoisePlayer {
    override val renderer = if (playing) NoiseRenderer(mix, 1L).also { it.render(FloatArray(0), FloatArray(0), 0) } else null
    override fun play() {}
    override fun stop() {}
}

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(SleeprAccent) {
        val mix = remember {
            Mix().apply {
                when (scene) {
                    Scene.PLAYING -> { applyPreset(preset("focus")); timer = 30 }
                    Scene.OCEAN -> { applyPreset(preset("ocean")); timer = 45 }
                    else -> {}
                }
            }
        }
        val display = remember { Display().apply { if (scene == Scene.PLAIN) { setWaves(false); setMixes(false) } } }
        val player = remember { DemoPlayer(mix, scene == Scene.PLAYING || scene == Scene.OCEAN) }
        when (scene) {
            Scene.IDLE, Scene.PLAYING, Scene.OCEAN, Scene.PLAIN -> MixerScreen(mix, display, player, {}, {})
            Scene.IDLE_BOTTOM -> MixerScreen(mix, display, player, {}, {}, scrollToEnd = true)
            Scene.SETTINGS -> SleeprSettings(mix, display, {}, {}, {})
            Scene.ABOUT -> SleeprAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

private fun preset(id: String) = Mix.PRESETS.first { it.id == id }

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
