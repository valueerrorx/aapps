package solutions.xapient.loudr.screenshots

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
import solutions.xapient.loudr.Settings
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.domain.TimeWeighting
import solutions.xapient.loudr.ui.LoudrAbout
import solutions.xapient.loudr.ui.LoudrAccent
import solutions.xapient.loudr.ui.LoudrSettings
import solutions.xapient.loudr.ui.MeterScreen
import solutions.xapient.loudr.ui.MicState
import kotlin.math.sin

// Screenshot-Zustaende laut SPEC.md; record: :apps:loudr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class LoudrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(LoudrAccent) { content() }
        }
    }

    // Verlauf: ruhiger Grund mit Ausschlaegen, deterministisch
    private fun history(base: Float, swing: Float, n: Int = 600) = FloatArray(n) { i ->
        val t = i.toFloat()
        base + swing * (0.55f * sin(t / 23f) + 0.3f * sin(t / 5.3f) + 0.15f * sin(t / 1.7f)) +
            if (i % 97 in 40..55) swing * 0.8f else 0f
    }

    private fun reading(level: Float, min: Float, max: Float, leq: Float, h: FloatArray, live: Boolean = true, over: Boolean = false) =
        Reading(level, level - 1f, leq, min, max, min + 2f, max - 3f, h, live, over)

    private val talk = reading(63.4f, 41f, 74f, 60.8f, history(58f, 9f))
    private val quiet = reading(33f, 29f, 41f, 34f, history(33f, 4f))
    private val loud = Reading(93f, 90f, 88f, 80f, 102f, 84f, 99f, history(86f, 7f, 400), true)
    // mit Luecke einer frueheren Pause
    private val paused = reading(71f, 44f, 83f, 66f, history(66f, 10f).also { it[380] = Float.NaN }, live = false)
    private val overload = reading(109f, 78f, 112f, 101f, history(100f, 6f), over = true)

    @Composable
    private fun Screen(r: Reading?, mic: MicState = MicState.READY, measuring: Boolean = true, s: Settings = Settings()) {
        MeterScreen(
            { r ?: Reading.EMPTY }, mic, measuring, s, null,
            onToggle = {}, onReset = {}, onMic = {}, onSettings = {}, animate = false,
        )
    }

    @Composable private fun Talk() = Screen(talk)
    @Composable private fun Ask() = Screen(null, MicState.ASK, measuring = false)

    @Test fun talk() = shot("talk") { Talk() }
    @Test fun talk_de() = shot("talk_de", "+de") { Talk() }
    @Test fun talk_land() = shot("talk_land", "+land") { Talk() }
    @Test fun talk_font200() = shot("talk_font200", fontScale = 2f) { Talk() }
    @Test fun talk_land_font200() = shot("talk_land_font200", "+land", fontScale = 2f) { Talk() }
    @Test fun talk_360dp() = shot("talk_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Talk() }

    @Test fun quiet() = shot("quiet") { Screen(quiet) }
    @Test fun loud() = shot("loud") { Screen(loud, s = Settings(TimeWeighting.SLOW, 2f)) }
    @Test fun loud_land() = shot("loud_land", "+land") { Screen(loud, s = Settings(TimeWeighting.SLOW, 2f)) }
    @Test fun paused() = shot("paused") { Screen(paused, measuring = false) }
    @Test fun overload() = shot("overload") { Screen(overload) }
    // Store DE wie EN
    @Test fun quiet_de() = shot("quiet_de", "+de") { Screen(quiet) }
    @Test fun loud_de() = shot("loud_de", "+de") { Screen(loud, s = Settings(TimeWeighting.SLOW, 2f)) }
    @Test fun paused_de() = shot("paused_de", "+de") { Screen(paused, measuring = false) }
    @Test fun talk_land_de() = shot("talk_land_de", "+de-land") { Talk() }

    @Test fun mic_ask() = shot("mic_ask") { Ask() }
    @Test fun mic_ask_de() = shot("mic_ask_de", "+de") { Ask() }
    @Test fun mic_ask_land() = shot("mic_ask_land", "+land") { Ask() }
    @Test fun mic_ask_font200() = shot("mic_ask_font200", fontScale = 2f) { Ask() }
    @Test fun mic_ask_land_font200() = shot("mic_ask_land_font200", "+land", fontScale = 2f) { Ask() }
    @Test fun mic_blocked() = shot("mic_blocked") { Screen(null, MicState.BLOCKED, measuring = false) }
    @Test fun mic_failed() = shot("mic_failed") { Screen(null, MicState.FAILED, measuring = false) }
    @Test fun mic_silenced() = shot("mic_silenced") { Screen(talk, MicState.SILENCED) }
    @Test fun mic_blocked_de_land_font200() = shot("mic_blocked_de_land_font200", "+de-land", fontScale = 2f) { Screen(null, MicState.BLOCKED, measuring = false) }

    @Composable private fun Settings_() = LoudrSettings(Settings(offset = 1.5f), {}, { 64.5f }, null, {}, {})

    @Test fun settings() = shot("settings") { Settings_() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings_() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings_() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings_() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings_() }

    @Test fun about() = shot("about") { LoudrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { LoudrAbout("0.1.0") {} }
    @Test fun about_font200() = shot("about_font200", fontScale = 2f) { LoudrAbout("0.1.0") {} }
}
