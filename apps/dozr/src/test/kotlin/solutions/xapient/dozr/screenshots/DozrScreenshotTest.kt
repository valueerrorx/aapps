package solutions.xapient.dozr.screenshots

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
import solutions.xapient.dozr.domain.FadeLength
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.domain.Sleep
import solutions.xapient.dozr.domain.Sleep.Companion.MINUTE
import solutions.xapient.dozr.ui.DozrAbout
import solutions.xapient.dozr.ui.DozrAccent
import solutions.xapient.dozr.ui.DozrSettings
import solutions.xapient.dozr.ui.SleepScreen
import java.util.Locale

// Screenshot-Zustaende laut SPEC.md; record: :apps:dozr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class DozrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private val now = 1_000_000_000L
    private val fade = FadeLength.M1.ms

    // Feste Uhr: jetzt = 23:14
    private fun wall(at: Long): String {
        val m = (23 * 60 + 14 + Math.floorDiv(at - now + 59_999, MINUTE)).mod(24 * 60)
        return String.format(Locale.ROOT, "%d:%02d", m / 60, m % 60)
    }

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(DozrAccent) { content() }
        }
    }

    @Composable
    private fun Screen(s: Sleep, drag: Int? = null, fadeMs: Long = fade) {
        SleepScreen(
            s, fadeMs, haptics = null, onChoose = {}, onStart = {}, onStop = {}, onExtend = {}, onSettings = {},
            wallText = ::wall, clock = { now }, animate = false, dragPreview = drag,
        )
    }

    // Lauf mit Restzeit left von set Minuten
    private fun running(left: Long, setMin: Int) = Sleep(setMin, Mode.RUNNING, now + left - setMin * MINUTE, now + left)

    @Composable private fun Idle() = Screen(Sleep(30))
    @Composable private fun Running() = Screen(running(23 * MINUTE + 41_000, 45))
    @Composable private fun Fading() = Screen(running(24_000, 30))
    @Composable private fun Done() = Screen(Sleep(30, Mode.DONE, now - 31 * MINUTE, now - MINUTE))

    @Composable
    private fun Settings() = DozrSettings(
        FadeLength.M1, {}, true, {}, true, {},
        notificationsMissing = true, onNotifications = {}, onAddTile = {}, onAbout = {}, onBack = {},
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

    @Test fun fading() = shot("fading") { Fading() }
    @Test fun fading_de() = shot("fading_de", "+de") { Fading() }
    @Test fun fading_land() = shot("fading_land", "+land") { Fading() }
    @Test fun fading_font200() = shot("fading_font200", fontScale = 2f) { Fading() }
    @Test fun fading_land_font200() = shot("fading_land_font200", "+land", fontScale = 2f) { Fading() }

    @Test fun done() = shot("done") { Done() }
    @Test fun done_de() = shot("done_de", "+de") { Done() }
    @Test fun done_land() = shot("done_land", "+land") { Done() }
    @Test fun done_font200() = shot("done_font200", fontScale = 2f) { Done() }
    @Test fun done_land_font200() = shot("done_land_font200", "+land", fontScale = 2f) { Done() }

    @Test fun drag() = shot("drag") { Screen(Sleep(30), drag = 90) }
    // Store DE wie EN
    @Test fun drag_de() = shot("drag_de", "+de") { Screen(Sleep(30), drag = 90) }
    @Test fun running_land_de() = shot("running_land_de", "+de-land") { Running() }
    @Test fun long() = shot("long") { Screen(Sleep(120)) }

    @Test fun settings() = shot("settings") { Settings() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { DozrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { DozrAbout("0.1.0") {} }
}
