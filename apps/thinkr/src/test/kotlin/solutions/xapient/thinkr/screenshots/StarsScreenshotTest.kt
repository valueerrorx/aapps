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
import solutions.xapient.thinkr.domain.stars.StarsGame
import solutions.xapient.thinkr.domain.stars.StarsState
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrAccent

// Sternbild-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class StarsScreenshotTest {

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

    // zwei perfekte Durchgaenge vorweg, damit Punkte und Kette stimmen
    private fun warm(seed: Long, level: Int): StarsState {
        var s = StarsGame.start(seed, level, false)
        repeat(2) {
            s = StarsGame.tick(s, s.recallAt)
            var t = s.round.now
            for (c in s.pass.stars) s = StarsGame.input(s, c, ++t)
            s = StarsGame.tick(s, s.nextAt)
        }
        return s
    }

    // Sterne leuchten gerade
    private fun show(level: Int = 4) = warm(5, level).let { StarsGame.tick(it, it.showAt + 500) }

    // Abfragen: zwei gefunden, ein Fehler
    private fun recall(): StarsState {
        var s = warm(5, 4)
        s = StarsGame.tick(s, s.recallAt)
        val empty = (0 until s.pass.side * s.pass.side).first { it !in s.pass.stars }
        s = StarsGame.input(s, s.pass.stars[0], s.round.now + 400)
        s = StarsGame.input(s, empty, s.round.now + 500)
        s = StarsGame.input(s, s.pass.stars[2], s.round.now + 450)
        return StarsGame.tick(s, s.round.now + 300)
    }

    // perfekt: Sternbild-Linien fertig gewachsen
    private fun perfect(): StarsState {
        var s = warm(9, 6)
        s = StarsGame.tick(s, s.recallAt)
        var t = s.round.now
        for (c in s.pass.stars) s = StarsGame.input(s, c, t + 250).also { t += 250 }
        return StarsGame.tick(s, s.pass.endAt + 1_100)
    }

    // zwei Fehler: verpasste Sterne gestrichelt
    private fun failed(): StarsState {
        var s = warm(13, 5)
        s = StarsGame.tick(s, s.recallAt)
        val empty = (0 until s.pass.side * s.pass.side).filter { it !in s.pass.stars }
        s = StarsGame.input(s, s.pass.stars[1], s.round.now + 500)
        s = StarsGame.input(s, empty[3], s.round.now + 400)
        s = StarsGame.input(s, empty[7], s.round.now + 400)
        return StarsGame.tick(s, s.pass.endAt + 600)
    }

    @Composable private fun Play(s: StarsState) = Games.Stars.Frozen(s, Phase.PLAYING)

    @Test fun stars_show() = shot("stars_show") { Play(show()) }
    @Test fun stars_show_land() = shot("stars_show_land", "+land") { Play(show()) }
    @Test fun stars_show_360dp() = shot("stars_show_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(show(8)) }
    @Test fun stars_recall() = shot("stars_recall") { Play(recall()) }
    @Test fun stars_recall_de() = shot("stars_recall_de", "+de") { Play(recall()) }
    @Test fun stars_recall_font200() = shot("stars_recall_font200", fontScale = 2f) { Play(recall()) }
    @Test fun stars_perfect() = shot("stars_perfect") { Play(perfect()) }
    @Test fun stars_failed() = shot("stars_failed") { Play(failed()) }
    @Test fun stars_big() = shot("stars_big") { Play(show(13)) }
    @Test fun stars_countdown() = shot("stars_countdown") { Games.Stars.Frozen(StarsGame.start(5, 4, false), Phase.COUNTDOWN, count = 3) }
    @Test fun stars_countdown_de_font200() = shot("stars_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Stars.Frozen(StarsGame.start(5, 4, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun stars_paused() = shot("stars_paused") { Games.Stars.Frozen(recall(), Phase.PAUSED) }
}
