package solutions.xapient.hertzr.screenshots

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
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

// Screenshot-Zustaende laut SPEC.md; record: :apps:hertzr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class HertzrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(HertzrAccent) { content() }
        }
    }

    @Composable
    private fun Screen(
        s: Settings,
        playing: Boolean = false,
        live: Double = s.hz,
        headphones: Boolean = false,
        input: InputTarget = InputTarget.NONE,
        text: String = "",
    ) {
        ToneScreen(
            s, playing, 22_000.0, { live }, headphones, haptics = null, actions = ToneActions(),
            animate = false, initialInput = input, initialText = text,
        )
    }

    private val idle = Settings()
    private val square = Settings(hz = 55.0, wave = Wave.SQUARE, levelDb = -14)
    private val loud = Settings(hz = 12_000.0, wave = Wave.SAW, levelDb = -6)
    private val sweep = Settings(mode = Mode.SWEEP, wave = Wave.TRIANGLE, sweepFrom = 20.0, sweepTo = 20_000.0, sweepSec = 10)
    private val sweepAt = SweepSpec(20.0, 20_000.0, 10).hzAt(0.4)

    @Composable private fun Idle() = Screen(idle)
    @Composable private fun Sweep() = Screen(sweep, playing = true, live = sweepAt)

    @Test fun idle() = shot("idle") { Idle() }
    @Test fun idle_de() = shot("idle_de", "+de") { Idle() }
    @Test fun idle_land() = shot("idle_land", "+land") { Idle() }
    @Test fun idle_font200() = shot("idle_font200", fontScale = 2f) { Idle() }
    @Test fun idle_land_font200() = shot("idle_land_font200", "+land", fontScale = 2f) { Idle() }
    @Test fun idle_360dp() = shot("idle_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Idle() }

    @Test fun play_square() = shot("play_square") { Screen(square, playing = true) }
    @Test fun play_loud() = shot("play_loud") { Screen(loud, playing = true, headphones = true) }

    @Test fun sweep() = shot("sweep") { Sweep() }
    @Test fun sweep_de() = shot("sweep_de", "+de") { Sweep() }
    @Test fun sweep_land() = shot("sweep_land", "+land") { Sweep() }
    @Test fun sweep_font200() = shot("sweep_font200", fontScale = 2f) { Sweep() }
    @Test fun sweep_land_font200() = shot("sweep_land_font200", "+land", fontScale = 2f) { Sweep() }

    @Test fun input() = shot("input") { Screen(idle, input = InputTarget.HZ, text = "1.5k") }

    @Composable private fun Settings_() = HertzrSettings(idle, {}, {}, {}, {})

    @Test fun settings() = shot("settings") { Settings_() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings_() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings_() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings_() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings_() }

    @Test fun about() = shot("about") { HertzrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { HertzrAbout("0.1.0") {} }
}
