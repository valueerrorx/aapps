package solutions.xapient.roundr.screenshots

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
import solutions.xapient.roundr.domain.Presets
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Session
import solutions.xapient.roundr.domain.Timeline
import solutions.xapient.roundr.domain.Workout
import solutions.xapient.roundr.ui.DoneScreen
import solutions.xapient.roundr.ui.EditorScreen
import solutions.xapient.roundr.ui.HomeScreen
import solutions.xapient.roundr.ui.LibraryScreen
import solutions.xapient.roundr.ui.RoundrAbout
import solutions.xapient.roundr.ui.RoundrAccent
import solutions.xapient.roundr.ui.RoundrSettings
import solutions.xapient.roundr.ui.RunScreen

// Screenshot-Zustaende laut SPEC.md; record: :apps:roundr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class RoundrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(RoundrAccent) { content() }
        }
    }

    private val tabata = Presets.all[0]
    private val tabata3 = Presets.all[1]
    private val boxing = Presets.all[4]

    // Uhr steht bei 0, Lauf seit 0: verstrichen = e
    @Composable
    private fun Running(w: Workout, e: Long, running: Boolean = true, confirm: Boolean = false) {
        val run = Run(w, Session(e, if (running) 0L else null), 1)
        RunScreen(
            run, clock = { 0L }, haptics = null, onToggle = {}, onSkip = {}, onBack = {}, onStop = {}, onAgain = {}, onClose = {},
            keepOn = false, animate = false, confirmStop = confirm,
        )
    }

    // Runde 3, Aktiv, 12 von 20 s uebrig
    @Composable private fun Work() = Running(tabata, 78_000)

    @Test fun run_work() = shot("run_work") { Work() }
    @Test fun run_work_de() = shot("run_work_de", "+de") { Work() }
    @Test fun run_work_land() = shot("run_work_land", "+land") { Work() }
    @Test fun run_work_font200() = shot("run_work_font200", fontScale = 2f) { Work() }
    @Test fun run_work_land_font200() = shot("run_work_land_font200", "+land", fontScale = 2f) { Work() }

    @Test fun run_rest() = shot("run_rest") { Running(tabata, 68_000) }
    @Test fun run_rest_land() = shot("run_rest_land", "+land") { Running(tabata, 68_000) }
    @Test fun run_prep() = shot("run_prep") { Running(tabata, 3_000) }
    @Test fun run_setrest() = shot("run_setrest") { Running(tabata3, 255_000) }
    @Test fun run_last3() = shot("run_last3") { Running(tabata, 88_000) }
    @Test fun run_paused() = shot("run_paused") { Running(tabata, 78_000, running = false) }
    @Test fun run_paused_de() = shot("run_paused_de", "+de") { Running(tabata, 78_000, running = false, confirm = true) }
    @Test fun run_long() = shot("run_long") { Running(boxing, 29_000) }
    // Store DE wie EN
    @Test fun run_rest_de() = shot("run_rest_de", "+de") { Running(tabata, 68_000) }
    @Test fun run_last3_de() = shot("run_last3_de", "+de") { Running(tabata, 88_000) }
    @Test fun run_work_land_de() = shot("run_work_land_de", "+de-land") { Work() }

    @Composable private fun Done() = DoneScreen(tabata, {}, {})

    @Test fun done() = shot("done") { Done() }
    @Test fun done_de() = shot("done_de", "+de") { Done() }
    @Test fun done_land() = shot("done_land", "+land") { Done() }
    @Test fun done_font200() = shot("done_font200", fontScale = 2f) { Done() }
    @Test fun done_land_font200() = shot("done_land_font200", "+land", fontScale = 2f) { Done() }

    @Composable private fun Home() = HomeScreen(tabata, {}, {}, {}, {})

    @Test fun home() = shot("home") { Home() }
    @Test fun home_de() = shot("home_de", "+de") { Home() }
    @Test fun home_360dp() = shot("home_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Home() }
    @Test fun home_land() = shot("home_land", "+land") { Home() }
    @Test fun home_font200() = shot("home_font200", fontScale = 2f) { Home() }
    @Test fun home_land_font200() = shot("home_land_font200", "+land", fontScale = 2f) { Home() }

    private val saved = listOf(Workout("Quiz", 5, 45, 15, 6), Workout("Sprint", 10, 60, 30, 5, 2, 120))

    @Composable private fun Library() = LibraryScreen(tabata, saved, {}, {}, {}, confirmName = "Sprint")

    @Test fun library() = shot("library") { Library() }
    @Test fun library_de() = shot("library_de", "+de") { Library() }
    @Test fun library_land() = shot("library_land", "+land") { Library() }
    @Test fun library_font200() = shot("library_font200", fontScale = 2f) { Library() }
    @Test fun library_land_font200() = shot("library_land_font200", "+land", fontScale = 2f) { Library() }

    @Composable private fun Editor() = EditorScreen(tabata3, saved.size, null, {}, { true }, {})

    @Test fun editor() = shot("editor") { Editor() }
    @Test fun editor_de() = shot("editor_de", "+de") { Editor() }
    @Test fun editor_land() = shot("editor_land", "+land") { Editor() }
    @Test fun editor_font200() = shot("editor_font200", fontScale = 2f) { Editor() }
    @Test fun editor_land_font200() = shot("editor_land_font200", "+land", fontScale = 2f) { Editor() }

    @Composable private fun Settings() = RoundrSettings(true, {}, true, {}, true, {}, {}, {})

    @Test fun settings() = shot("settings") { Settings() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { RoundrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { RoundrAbout("0.1.0") {} }

    // Sanity: Zeitpunkte oben passen zur SPEC
    @Test fun statesMatchSpec() {
        val t = Timeline(tabata)
        check(t.at(78_000)!!.remainingMs == 12_000L && t.at(78_000)!!.segment.round == 3)
        check(Timeline(boxing).at(29_000)!!.remainingMs == 161_000L)
    }
}
