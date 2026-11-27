package solutions.xapient.levlr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.levlr.domain.AngleUnit
import solutions.xapient.levlr.domain.Bias
import solutions.xapient.levlr.domain.Calibration
import solutions.xapient.levlr.domain.LevelMode
import solutions.xapient.levlr.domain.LevelReading
import solutions.xapient.levlr.ui.LevelScreen
import solutions.xapient.levlr.ui.LevelState
import solutions.xapient.levlr.ui.LevlrAbout
import solutions.xapient.levlr.ui.LevlrSettings

// Feste Zustaende fuer App-Store-Screenshots (Simulator hat keinen Lagesensor), wie die Roborazzi-Tests auf Android
internal enum class Scene {
    EDGE_TILTED, EDGE_LEVEL, FLAT_TILTED, FLAT_LEVEL, HELD, EDGE_MM, CALIBRATE, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(LevlrAccent) {
        when (scene) {
            Scene.EDGE_TILTED -> Level(remember { edgeTilted() })
            Scene.EDGE_MM -> Level(remember { edgeTilted() }, AngleUnit.MM_PER_M)
            Scene.EDGE_LEVEL -> Level(remember { reading(LevelReading(LevelMode.EDGE, edgeAngle = 0.02f, isLevel = true)) }, shine = 0.45f)
            Scene.FLAT_TILTED -> Level(remember { flatTilted() })
            Scene.FLAT_LEVEL -> Level(remember { reading(LevelReading.FLAT_LEVEL) }, shine = 0.35f)
            Scene.HELD -> Level(remember { reading(LevelReading(LevelMode.EDGE, edgeAngle = -12.5f)).apply { toggleHold() } })
            Scene.CALIBRATE -> Level(remember { reading(LevelReading(LevelMode.EDGE, edgeAngle = 0.8f)).apply { startCalibration() } })
            Scene.SETTINGS -> LevlrSettings(
                unit = AngleUnit.DEGREE, onUnit = {}, haptics = true, onHaptics = {},
                calibration = Calibration(flat = Bias(0.1f, 0f)), onCalibrate = {}, onResetCalibration = {},
                onAbout = {}, onBack = {},
            )
            Scene.ABOUT -> LevlrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Level(state: LevelState, unit: AngleUnit = AngleUnit.DEGREE, shine: Float = -1f) =
    LevelScreen(state, unit, null, {}, {}, animate = false, shineOverride = shine)

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}

private fun reading(r: LevelReading) = LevelState().apply { show(r) }

// Kante 3,4°, Fluessigkeit schwappt noch nach
private fun edgeTilted() = reading(LevelReading(LevelMode.EDGE, edgeAngle = 3.4f)).apply {
    physics.surface.pose(-3.4f + 6f, -24f)
    physics.bubble.pose(physics.bubble.target + 0.05f, -0.9f)
}

private fun flatTilted() = reading(LevelReading(LevelMode.FLAT, flatX = 2.1f, flatY = -1.3f)).apply {
    physics.bubbleX.pose(physics.bubbleX.target - 0.04f, 0.8f)
}
