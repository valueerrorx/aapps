package solutions.xapient.tunr.screenshots

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
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Notation
import solutions.xapient.tunr.domain.Notes
import solutions.xapient.tunr.domain.Reading
import solutions.xapient.tunr.domain.Settings
import solutions.xapient.tunr.ui.InstrumentScreen
import solutions.xapient.tunr.ui.MicUi
import solutions.xapient.tunr.ui.TunerScreen
import solutions.xapient.tunr.ui.TunerState
import solutions.xapient.tunr.ui.TunrAbout
import solutions.xapient.tunr.ui.TunrAccent
import solutions.xapient.tunr.ui.TunrSettings
import kotlin.math.pow

// Screenshot-Zustaende laut SPEC.md; record: :apps:tunr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TunrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(TunrAccent) { content() }
        }
    }

    private fun tone(midi: Int, cents: Float, string: Int = -1) = Reading(
        active = true, heard = true, hz = (Notes.hz(midi, 440f) * 2.0.pow(cents / 1200.0)).toFloat(),
        target = midi, cents = cents, string = string, inTune = kotlin.math.abs(cents) <= 5f, locks = 1,
    )

    @Composable
    private fun Screen(s: Settings, r: Reading = Reading(), mic: MicUi = MicUi.ON, strobe: Float = 0f, shine: Float = 1f, locked: Int = -1) {
        val state = TunerState(s.a4, s.instrument).apply { show(r, strobe, shine) }
        TunerScreen(state, s, mic, locked, null, {}, {}, {}, {}, animate = false)
    }

    private val chromatic = Settings()
    private val guitar = Settings(instrument = Instrument.GUITAR)

    @Composable private fun Listen() = Screen(chromatic)
    @Composable private fun InTune() = Screen(chromatic, tone(69, 1f), strobe = 4f, shine = 0.35f)
    @Composable private fun FlatGuitar() = Screen(guitar, tone(45, -23f, string = 1), strobe = 21f)

    @Test fun listen() = shot("listen") { Listen() }
    @Test fun listen_360dp() = shot("listen_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Listen() }

    @Test fun in_tune() = shot("in_tune") { InTune() }
    @Test fun in_tune_de() = shot("in_tune_de", "+de") { InTune() }
    @Test fun in_tune_land() = shot("in_tune_land", "+land") { InTune() }
    @Test fun in_tune_font200() = shot("in_tune_font200", fontScale = 2f) { InTune() }
    @Test fun in_tune_land_font200() = shot("in_tune_land_font200", "+land", fontScale = 2f) { InTune() }
    @Test fun in_tune_360dp() = shot("in_tune_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { InTune() }

    @Test fun flat_guitar() = shot("flat_guitar") { FlatGuitar() }
    @Test fun flat_guitar_de() = shot("flat_guitar_de", "+de") { Screen(guitar.copy(notation = Notation.GERMAN), tone(45, -23f, string = 1), strobe = 21f, locked = 1) }
    @Test fun flat_guitar_land() = shot("flat_guitar_land", "+land") { FlatGuitar() }
    @Test fun flat_guitar_font200() = shot("flat_guitar_font200", fontScale = 2f) { FlatGuitar() }
    @Test fun flat_guitar_land_font200() = shot("flat_guitar_land_font200", "+land", fontScale = 2f) { FlatGuitar() }

    @Test fun sharp() = shot("sharp") { Screen(chromatic, tone(54, 38f), strobe = 40f) }

    @Test fun mic_ask() = shot("mic_ask") { Screen(chromatic, mic = MicUi.ASK) }
    @Test fun mic_blocked() = shot("mic_blocked") { Screen(chromatic, mic = MicUi.BLOCKED) }

    @Test fun instruments() = shot("instruments") { InstrumentScreen(Instrument.GUITAR, Notation.SHARP, {}, {}) }
    @Test fun instruments_de() = shot("instruments_de", "+de") { InstrumentScreen(Instrument.GUITAR, Notation.GERMAN, {}, {}) }

    @Composable private fun Settings_() = TunrSettings(chromatic, {}, {}, {})

    @Test fun settings() = shot("settings") { Settings_() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings_() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings_() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings_() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings_() }

    @Test fun about() = shot("about") { TunrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { TunrAbout("0.1.0") {} }
}
