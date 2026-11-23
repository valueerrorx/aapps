package solutions.xapient.thinkr.screenshots

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
import solutions.xapient.thinkr.Games
import solutions.xapient.thinkr.domain.glance.GlanceGame
import solutions.xapient.thinkr.domain.glance.GlanceInput
import solutions.xapient.thinkr.domain.glance.GlancePhase
import solutions.xapient.thinkr.domain.glance.GlanceState
import solutions.xapient.thinkr.domain.glance.Spots
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrAccent

// Rundblick-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class GlanceScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(ThinkrAccent) { content() }
        }
    }

    // in 60-Hz-Frames laufen, bis want gilt
    private fun until(s: GlanceState, want: (GlanceState) -> Boolean): GlanceState {
        var x = s
        var t = x.round.now.toDouble()
        while (!want(x)) {
            t += 1000.0 / 60
            x = GlanceGame.tick(x, t.toLong())
        }
        return x
    }

    // ein paar Aufgaben richtig loesen
    private fun warm(seed: Long, level: Int, solved: Int): GlanceState {
        var s = GlanceGame.start(seed, level, false)
        repeat(solved) {
            val x = until(s) { it.phase() == GlancePhase.ANSWER }
            val a = GlanceGame.input(x, GlanceInput.Center(x.trial.mark), x.round.now + 500)
            val b = GlanceGame.input(a, GlanceInput.Spot(x.trial.spot), x.round.now + 900)
            s = until(b) { it.trial.no > b.trial.no }
        }
        return s
    }

    private fun flash(level: Int = 9) = until(warm(5, level, 3)) { it.phase() == GlancePhase.FLASH }
    private fun asking() = until(warm(5, 9, 3)) { it.phase() == GlancePhase.ANSWER }.let {
        GlanceGame.input(it, GlanceInput.Center(it.trial.mark), it.round.now + 400)
    }

    private fun solved(): GlanceState {
        val x = asking()
        return GlanceGame.tick(GlanceGame.input(x, GlanceInput.Spot(x.trial.spot), x.round.now + 300), x.round.now + 700)
    }

    private fun missed(): GlanceState {
        val x = asking()
        val wrong = (x.trial.spot + 3) % Spots.count(x.rings)
        return GlanceGame.tick(GlanceGame.input(x, GlanceInput.Spot(wrong), x.round.now + 300), x.round.now + 700)
    }

    @Composable private fun Play(s: GlanceState) = Games.Glance.Frozen(s, Phase.PLAYING)

    @Test fun glance_fix() = shot("glance_fix") { Play(warm(5, 9, 3)) }
    @Test fun glance_flash() = shot("glance_flash") { Play(flash()) }
    @Test fun glance_flash_decoys() = shot("glance_flash_decoys") { Play(flash(15)) }
    @Test fun glance_flash_land() = shot("glance_flash_land", "+land") { Play(flash(15)) }
    @Test fun glance_mask() = shot("glance_mask") { Play(until(flash()) { it.phase() == GlancePhase.MASK }) }
    @Test fun glance_answer() = shot("glance_answer") { Play(asking()) }
    @Test fun glance_answer_de() = shot("glance_answer_de", "+de") { Play(asking()) }
    @Test fun glance_answer_360dp() = shot("glance_answer_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(asking()) }
    @Test fun glance_answer_font200() = shot("glance_answer_font200", fontScale = 2f) { Play(asking()) }
    @Test fun glance_solved() = shot("glance_solved") { Play(solved()) }
    @Test fun glance_missed() = shot("glance_missed") { Play(missed()) }
    @Test fun glance_countdown() = shot("glance_countdown") { Games.Glance.Frozen(GlanceGame.start(5, 9, false), Phase.COUNTDOWN, count = 3) }
    @Test fun glance_countdown_de_font200() = shot("glance_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Glance.Frozen(GlanceGame.start(5, 9, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun glance_paused() = shot("glance_paused") { Games.Glance.Frozen(asking(), Phase.PAUSED) }
}
