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
import solutions.xapient.thinkr.domain.bounce.BounceGame
import solutions.xapient.thinkr.domain.bounce.BounceRules
import solutions.xapient.thinkr.domain.bounce.BounceState
import solutions.xapient.thinkr.domain.bounce.Optics
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrAccent

// Bande-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class BounceScreenshotTest {

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

    // ein paar Aufgaben richtig loesen, dann die naechste bis zum Zeigen
    private fun warm(seed: Long, level: Int, solved: Int): BounceState {
        var s = BounceGame.start(seed, level, false)
        repeat(solved) {
            val x = BounceGame.tick(s, s.hideAt)
            val a = BounceGame.input(x, x.shot.trace.exit, x.round.now + 600)
            s = BounceGame.tick(a, a.nextAt)
        }
        return s
    }

    private fun showing() = warm(3, 4, 2).let { BounceGame.tick(it, it.shot.startAt + BounceRules.LEAD_MS + 900) }
    private fun asking() = warm(3, 4, 2).let { BounceGame.tick(it, it.hideAt + 400) }

    // richtig getippt, das Licht ist unterwegs
    private fun solving(): BounceState {
        val x = asking()
        val a = BounceGame.input(x, x.shot.trace.exit, x.round.now + 200)
        return BounceGame.tick(a, a.shot.saidAt + (a.shot.runMs * 0.7).toLong())
    }

    // falsch getippt, Aufloesung steht
    private fun wrong(): BounceState {
        val x = asking()
        val bad = Optics.ports(x.shot.side).first { it != x.shot.trace.exit && it != x.shot.entry && it.edge == x.shot.trace.exit.edge }
        val a = BounceGame.input(x, bad, x.round.now + 200)
        return BounceGame.tick(a, a.doneAt + 500)
    }

    private fun big() = warm(8, 13, 1).let { BounceGame.tick(it, it.shot.startAt + BounceRules.LEAD_MS + 700) }

    @Composable private fun Play(s: BounceState) = Games.Bounce.Frozen(s, Phase.PLAYING)

    @Test fun bounce_show() = shot("bounce_show") { Play(showing()) }
    @Test fun bounce_show_land() = shot("bounce_show_land", "+land") { Play(showing()) }
    @Test fun bounce_show_360dp() = shot("bounce_show_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(big()) }
    @Test fun bounce_answer() = shot("bounce_answer") { Play(asking()) }
    @Test fun bounce_answer_de() = shot("bounce_answer_de", "+de") { Play(asking()) }
    @Test fun bounce_solve() = shot("bounce_solve") { Play(solving()) }
    @Test fun bounce_wrong() = shot("bounce_wrong") { Play(wrong()) }
    @Test fun bounce_big() = shot("bounce_big") { Play(big()) }
    @Test fun bounce_countdown() = shot("bounce_countdown") { Games.Bounce.Frozen(BounceGame.start(3, 4, false), Phase.COUNTDOWN, count = 3) }
    @Test fun bounce_countdown_de_font200() = shot("bounce_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Bounce.Frozen(BounceGame.start(3, 4, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun bounce_paused() = shot("bounce_paused") { Games.Bounce.Frozen(asking(), Phase.PAUSED) }
}
