package solutions.xapient.focusr.screenshots

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
import solutions.xapient.focusr.domain.Mode
import solutions.xapient.focusr.domain.Scale
import solutions.xapient.focusr.domain.Timer
import solutions.xapient.focusr.ui.FocusrAbout
import solutions.xapient.focusr.ui.FocusrAccent
import solutions.xapient.focusr.ui.FocusrSettings
import solutions.xapient.focusr.ui.TimerScreen

// Screenshot-Zustaende laut SPEC.md; record: :apps:focusr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TimerScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private val now = 1_700_000_000_000L
    private val min = Timer.MINUTE

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(FocusrAccent) { content() }
        }
    }

    @Composable
    private fun Screen(t: Timer, scale: Scale = Scale.MIN60, drag: Long? = null, pulse: Float = 0f) {
        TimerScreen(
            t, scale, haptics = null, onSet = {}, onStart = {}, onPause = {}, onReset = {}, onSettings = {},
            endText = { "14:35" }, clock = { now }, animate = false, dragPreview = drag, pulseAt = pulse,
        )
    }

    private fun running(left: Long, set: Long) = Timer(set, Mode.RUNNING, now + left, 0)

    @Composable private fun Idle() = Screen(Timer(25 * min))
    @Composable private fun Running() = Screen(running(17 * min + 42_000, 25 * min))
    @Composable private fun Paused() = Screen(Timer(25 * min, Mode.PAUSED, 0, 9 * min + 15_000))
    @Composable private fun Done() = Screen(Timer(25 * min, Mode.RUNNING, now - 1000, 0))

    @Composable
    private fun Settings() = FocusrSettings(
        Scale.MIN60, {}, true, {}, true, {}, true, {}, true, {},
        notificationsMissing = true, onNotifications = {}, onAbout = {}, onBack = {},
    )

    @Test fun idle() = shot("idle") { Idle() }
    @Test fun idle_de() = shot("idle_de", "+de") { Idle() }
    @Test fun idle_land() = shot("idle_land", "+land") { Idle() }
    @Test fun idle_font200() = shot("idle_font200", fontScale = 2f) { Idle() }
    @Test fun idle_land_font200() = shot("idle_land_font200", "+land", fontScale = 2f) { Idle() }

    @Test fun running() = shot("running") { Running() }
    @Test fun running_de() = shot("running_de", "+de") { Running() }
    @Test fun running_land() = shot("running_land", "+land") { Running() }
    @Test fun running_font200() = shot("running_font200", fontScale = 2f) { Running() }
    @Test fun running_land_font200() = shot("running_land_font200", "+land", fontScale = 2f) { Running() }
    @Test fun running_360dp() = shot("running_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Running() }

    @Test fun paused() = shot("paused") { Paused() }
    @Test fun paused_de() = shot("paused_de", "+de") { Paused() }
    @Test fun paused_land() = shot("paused_land", "+land") { Paused() }
    @Test fun paused_font200() = shot("paused_font200", fontScale = 2f) { Paused() }
    @Test fun paused_land_font200() = shot("paused_land_font200", "+land", fontScale = 2f) { Paused() }

    @Test fun done() = shot("done") { Done() }
    @Test fun done_de() = shot("done_de", "+de") { Done() }
    @Test fun done_land() = shot("done_land", "+land") { Done() }
    @Test fun done_font200() = shot("done_font200", fontScale = 2f) { Done() }
    @Test fun done_land_font200() = shot("done_land_font200", "+land", fontScale = 2f) { Done() }

    @Test fun last_minute() = shot("last_minute") { Screen(running(42_000, 5 * min), pulse = 0.8f) }
    @Test fun drag() = shot("drag") { Screen(Timer(25 * min), drag = 40 * min) }
    @Test fun zero() = shot("zero") { Screen(Timer(0)) }
    @Test fun zero_de_font200() = shot("zero_de_font200", "+de", fontScale = 2f) { Screen(Timer(0)) }
    @Test fun scale5() = shot("scale5") { Screen(running(3 * min + 20_000, 4 * min), Scale.MIN5) }
    @Test fun scale20() = shot("scale20") { Screen(running(12 * min + 40_000, 15 * min), Scale.MIN20) }
    @Test fun scale120() = shot("scale120") { Screen(running(95 * min, 110 * min), Scale.MIN120) }

    @Test fun settings() = shot("settings") { Settings() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { FocusrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { FocusrAbout("0.1.0") {} }
}
