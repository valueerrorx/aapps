package solutions.xapient.levlr.screenshots

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import solutions.xapient.aapps.core.billing.Access
import solutions.xapient.aapps.core.billing.GateState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.ThanksScreen
import solutions.xapient.levlr.LevlrAccent
import solutions.xapient.levlr.domain.AngleUnit
import solutions.xapient.levlr.domain.Bias
import solutions.xapient.levlr.domain.Calibration
import solutions.xapient.levlr.domain.LevelMode
import solutions.xapient.levlr.domain.LevelReading
import solutions.xapient.levlr.domain.Vec3
import solutions.xapient.levlr.ui.LevelScreen
import solutions.xapient.levlr.ui.LevelState
import solutions.xapient.levlr.ui.LevlrAbout
import solutions.xapient.levlr.ui.LevlrSettings
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot-Zustaende laut SPEC.md; record: ./gradlew :apps:levlr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class LevelScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(LevlrAccent) { content() }
        }
    }

    @Composable
    private fun Level(state: LevelState, unit: AngleUnit = AngleUnit.DEGREE, shine: Float = -1f) {
        LevelScreen(state, unit, null, {}, {}, animate = false, shineOverride = shine)
    }

    // Kante 3,4°, Fluessigkeit schwappt noch nach
    private fun edgeTilted() = LevelState().apply {
        show(LevelReading(LevelMode.EDGE, edgeAngle = 3.4f))
        physics.surface.pose(-3.4f + 6f, -24f)
        physics.bubble.pose(physics.bubble.target + 0.05f, -0.9f)
    }

    private fun flatTilted() = LevelState().apply {
        show(LevelReading(LevelMode.FLAT, flatX = 2.1f, flatY = -1.3f))
        physics.bubbleX.pose(physics.bubbleX.target - 0.04f, 0.8f)
    }

    @Test fun edge_tilted() = shot("edge_tilted") { Level(edgeTilted()) }

    @Test fun edge_tilted_de() = shot("edge_tilted_de", "+de") { Level(edgeTilted()) }

    @Test fun edge_tilted_360dp() =
        shot("edge_tilted_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Level(edgeTilted()) }

    @Test fun edge_tilted_land() = shot("edge_tilted_land", "+land") { Level(edgeTilted()) }

    @Test fun edge_tilted_font130() = shot("edge_tilted_font130", fontScale = 1.3f) { Level(edgeTilted()) }

    @Test fun edge_tilted_font200() = shot("edge_tilted_font200", fontScale = 2f) { Level(edgeTilted()) }

    @Test fun edge_mm() = shot("edge_mm") { Level(edgeTilted(), AngleUnit.MM_PER_M) }

    @Test fun edge_level() = shot("edge_level") {
        Level(LevelState().apply { show(LevelReading(LevelMode.EDGE, edgeAngle = 0.02f, isLevel = true)) }, shine = 0.45f)
    }

    @Test fun flat_tilted() = shot("flat_tilted") { Level(flatTilted()) }

    @Test fun flat_tilted_land() = shot("flat_tilted_land", "+land") { Level(flatTilted()) }

    @Test fun flat_level() = shot("flat_level") {
        Level(LevelState().apply { show(LevelReading.FLAT_LEVEL) }, shine = 0.35f)
    }

    @Test fun held() = shot("held") {
        Level(LevelState().apply { show(LevelReading(LevelMode.EDGE, edgeAngle = -12.5f)); toggleHold() })
    }

    // Store DE wie EN
    @Test fun edge_level_de() = shot("edge_level_de", "+de") {
        Level(LevelState().apply { show(LevelReading(LevelMode.EDGE, edgeAngle = 0.02f, isLevel = true)) }, shine = 0.45f)
    }

    @Test fun flat_tilted_de() = shot("flat_tilted_de", "+de") { Level(flatTilted()) }

    @Test fun flat_level_de() = shot("flat_level_de", "+de") {
        Level(LevelState().apply { show(LevelReading.FLAT_LEVEL) }, shine = 0.35f)
    }

    @Test fun held_de() = shot("held_de", "+de") {
        Level(LevelState().apply { show(LevelReading(LevelMode.EDGE, edgeAngle = -12.5f)); toggleHold() })
    }

    @Test fun edge_mm_de() = shot("edge_mm_de", "+de") { Level(edgeTilted(), AngleUnit.MM_PER_M) }

    @Test fun edge_tilted_land_de() = shot("edge_tilted_land_de", "+de-land") { Level(edgeTilted()) }

    @Test fun calibrate_step1() = shot("calibrate_step1") {
        Level(LevelState().apply { show(LevelReading(LevelMode.EDGE, edgeAngle = 0.8f)); startCalibration() })
    }

    private fun calibrateStep2() = LevelState().apply {
        show(LevelReading(LevelMode.FLAT, flatX = -0.6f, flatY = 0.4f))
        startCalibration()
        val raw = Vec3(-0.1f, 0.07f, 9.81f)
        calibration!!.apply {
            startMeasuring(LevelMode.FLAT)
            repeat(80) { offer(raw, 0.02f, LevelMode.FLAT) }
            startMeasuring(LevelMode.FLAT)
            repeat(30) { offer(raw, 0.02f, LevelMode.FLAT) }
        }
    }

    @Test fun calibrate_step2() = shot("calibrate_step2") { Level(calibrateStep2()) }

    @Test fun calibrate_step2_de() = shot("calibrate_step2_de", "+de") { Level(calibrateStep2()) }

    @Composable
    private fun Settings() = LevlrSettings(
        unit = AngleUnit.DEGREE, onUnit = {}, haptics = true, onHaptics = {},
        calibration = Calibration(flat = Bias(0.1f, 0f)), onCalibrate = {}, onResetCalibration = {},
        onAbout = {}, onBack = {},
    )

    @Test fun settings() = shot("settings") { Settings() }

    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }

    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { LevlrAbout("0.1.0") {} }

    @Test fun about_de() = shot("about_de", "+de") { LevlrAbout("0.1.0") {} }

    // Dankes-Bildschirm nach der Testphase (core/ui ThanksScreen, gleich in allen Apps)
    @Composable
    private fun Thanks(price: String?, pending: Boolean = false) =
        ThanksScreen("levlr", GateState(Access.EXPIRED, price = price, pending = pending), {}, {})

    @Test fun thanks() = shot("thanks") { Thanks("€1.49") }

    @Test fun thanks_de() = shot("thanks_de", "+de") { Thanks("1,49 €") }

    @Test fun thanks_font200() = shot("thanks_font200", fontScale = 2f) { Thanks("€1.49") }

    @Test fun thanks_land() = shot("thanks_land", "+land") { Thanks("€1.49") }

    @Test fun thanks_offline_de() = shot("thanks_offline_de", "+de") { Thanks(null) }

    @Test fun thanks_pending() = shot("thanks_pending") { Thanks("€1.49", pending = true) }
}
