package solutions.xapient.pulsr.screenshots

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
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.Subdivision
import solutions.xapient.pulsr.ui.PulseScreen
import solutions.xapient.pulsr.ui.PulsrAbout
import solutions.xapient.pulsr.ui.PulsrAccent
import solutions.xapient.pulsr.ui.PulsrSettings

// Screenshot-Zustaende laut SPEC.md; record: :apps:pulsr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PulsrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(PulsrAccent) { content() }
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
    private fun Screen(s: Settings, p: Pulse? = null) {
        PulseScreen(
            s, playing = p != null, pulse = { p }, haptics = null,
            onNudge = {}, onBpm = {}, onTap = {}, onBeats = {}, onSub = {}, onToggle = {}, onSettings = {}, animate = false,
        )
    }

    private val idle = Settings()
    private val accent = Settings(bpm = 96, beats = 4, sub = Subdivision.EIGHTH)
    private val waltz = Settings(bpm = 72, beats = 3, sub = Subdivision.TRIPLET)
    private val fast = Settings(bpm = 220, beats = 7, sub = Subdivision.SIXTEENTH)

    @Composable private fun Idle() = Screen(idle)
    @Composable private fun Accent() = Screen(accent, pulse(accent, 0, 0.12f))

    @Test fun idle() = shot("idle") { Idle() }
    @Test fun idle_de() = shot("idle_de", "+de") { Idle() }
    @Test fun idle_land() = shot("idle_land", "+land") { Idle() }
    @Test fun idle_font200() = shot("idle_font200", fontScale = 2f) { Idle() }
    @Test fun idle_land_font200() = shot("idle_land_font200", "+land", fontScale = 2f) { Idle() }
    @Test fun idle_360dp() = shot("idle_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Idle() }

    @Test fun play_accent() = shot("play_accent") { Accent() }
    @Test fun play_accent_de() = shot("play_accent_de", "+de") { Accent() }
    @Test fun play_accent_land() = shot("play_accent_land", "+land") { Accent() }
    @Test fun play_accent_font200() = shot("play_accent_font200", fontScale = 2f) { Accent() }
    @Test fun play_accent_land_font200() = shot("play_accent_land_font200", "+land", fontScale = 2f) { Accent() }

    @Test fun play_beat3() = shot("play_beat3") { Screen(waltz, pulse(waltz, 2, 0.4f)) }
    @Test fun play_fast() = shot("play_fast") { Screen(fast, pulse(fast, 3, 0.3f)) }

    @Composable private fun Settings_() = PulsrSettings(idle, {}, {}, {})

    @Test fun settings() = shot("settings") { Settings_() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings_() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings_() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings_() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings_() }

    @Test fun about() = shot("about") { PulsrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { PulsrAbout("0.1.0") {} }
}
