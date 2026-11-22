package solutions.xapient.magnr.screenshots

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
import solutions.xapient.magnr.Settings
import solutions.xapient.magnr.domain.FieldMeter
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.domain.Reading
import solutions.xapient.magnr.domain.Sensitivity
import solutions.xapient.magnr.ui.DetectorScreen
import solutions.xapient.magnr.ui.DetectorState
import solutions.xapient.magnr.ui.Hint
import solutions.xapient.magnr.ui.MagnrAbout
import solutions.xapient.magnr.ui.MagnrAccent
import solutions.xapient.magnr.ui.MagnrSettings
import kotlin.math.exp
import kotlin.math.sin

// Screenshot-Zustaende laut SPEC.md; record: :apps:magnr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class MagnrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(MagnrAccent) { content() }
        }
    }

    // Verlauf: ruhiges Rauschen, dann ein Schwenk ueber einen Fund (Glocke), deterministisch
    private fun sweep(peakAt: Int, height: Float, n: Int = 200, noise: Float = 0.03f) = FloatArray(n) { i ->
        val d = (i - peakAt) / 14f
        val bump = height * exp(-d * d)
        (bump + noise * (0.5f + 0.5f * sin(i / 3.1f) * sin(i / 7.7f))).coerceIn(0f, 1f)
    }

    private val near = Reading(
        total = 69.2f, baseline = 47.9f, dev = 21.3f, strength = Sensitivity.MEDIUM.strength(21.3f),
        level = Level.NEAR, peak = 24.6f, history = sweep(120, 0.62f).let { a -> FloatArray(a.size) { maxOf(a[it], sweep(206, 0.56f)[it]) } },
    )
    private val quiet = Reading(
        total = 48.2f, baseline = 47.9f, dev = 0.3f, strength = 0f, level = Level.QUIET, peak = 0.6f, history = sweep(60, 0.18f),
    )
    private val strong = Reading(
        total = 61.4f, baseline = 48.0f, dev = 13.4f, strength = 1f, level = Level.STRONG, peak = 14.1f,
        history = sweep(196, 1f).also { for (i in 0 until 120) it[i] = sweep(70, 0.45f)[i] },
    )
    private val saturated = Reading(
        total = 4912f, baseline = 48.1f, dev = 4863.9f, strength = 1f, level = Level.STRONG, peak = 4863.9f,
        history = sweep(199, 1f), saturated = true,
    )
    // pausiert, mit Luecke einer frueheren Pause
    private val paused = Reading(
        total = 52.3f, baseline = 47.9f, dev = 4.4f, strength = Sensitivity.MEDIUM.strength(4.4f),
        level = Level.QUIET, peak = 4.4f, history = sweep(110, 0.7f).also { for (i in 60..64) it[i] = Float.NaN },
    )

    private fun state(r: Reading?, hint: Hint? = null) = DetectorState(FieldMeter()).apply {
        fixed = r
        when (hint) {
            Hint.INTRO -> introVisible = true
            Hint.CALIBRATE -> showCalibration()
            null -> Unit
        }
    }

    @Composable
    private fun Screen(
        r: Reading?,
        live: Boolean = true,
        sensitivity: Sensitivity = Sensitivity.MEDIUM,
        sound: Boolean = false,
        hint: Hint? = null,
        available: Boolean = true,
    ) {
        DetectorScreen(
            state(r, hint), sensitivity, available, live, sound, keepOn = true, haptics = null,
            onPause = {}, onZero = {}, onSound = {}, onSettings = {}, onIntro = {}, hintPhase = 0.3f,
        )
    }

    @Composable private fun Near() = Screen(near)
    @Composable private fun Intro() = Screen(quiet, hint = Hint.INTRO)
    @Composable private fun NoSensor() = Screen(null, live = false, available = false)

    @Test fun near() = shot("near") { Near() }
    @Test fun near_de() = shot("near_de", "+de") { Near() }
    @Test fun near_land() = shot("near_land", "+land") { Near() }
    @Test fun near_font200() = shot("near_font200", fontScale = 2f) { Near() }
    @Test fun near_land_font200() = shot("near_land_font200", "+land", fontScale = 2f) { Near() }
    @Test fun near_360dp() = shot("near_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Near() }

    @Test fun quiet() = shot("quiet") { Screen(quiet) }
    @Test fun strong() = shot("strong") { Screen(strong, sensitivity = Sensitivity.HIGH, sound = true) }
    @Test fun saturated() = shot("saturated") { Screen(saturated, sound = true) }
    @Test fun paused() = shot("paused") { Screen(paused, live = false) }
    @Test fun paused_land() = shot("paused_land", "+land") { Screen(paused, live = false) }

    @Test fun intro() = shot("intro") { Intro() }
    @Test fun intro_de() = shot("intro_de", "+de") { Intro() }
    @Test fun intro_land() = shot("intro_land", "+land") { Intro() }
    @Test fun intro_font200() = shot("intro_font200", fontScale = 2f) { Intro() }
    @Test fun calibrate() = shot("calibrate") { Screen(quiet, hint = Hint.CALIBRATE) }

    @Test fun no_sensor() = shot("no_sensor") { NoSensor() }
    @Test fun no_sensor_land() = shot("no_sensor_land", "+land") { NoSensor() }

    @Composable private fun Settings_() = MagnrSettings(Settings(sound = true), {}, {}, {}, {}, {})

    @Test fun settings() = shot("settings") { Settings_() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings_() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings_() }

    @Test fun about() = shot("about") { MagnrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { MagnrAbout("0.1.0") {} }
    @Test fun about_font200() = shot("about_font200", fontScale = 2f) { MagnrAbout("0.1.0") {} }
}
