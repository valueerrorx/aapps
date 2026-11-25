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
import solutions.xapient.thinkr.domain.ink.InkGame
import solutions.xapient.thinkr.domain.ink.InkState
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrAccent

// Tinte-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class InkScreenshotTest {

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

    // n Antworten (Nummern in wrong falsch), dann kurz nach dem Hereinklappen festgehalten
    private fun play(level: Int, n: Int, wrong: Set<Int> = emptySet(), safe: Boolean = false, after: Long = 600, seed: Long = 21): InkState {
        var s = InkGame.start(seed, level, false, safe)
        repeat(n) { i -> s = InkGame.input(s, if (i in wrong) !s.card.match else s.card.match, s.shownAt + 800) }
        return InkGame.tick(s, s.shownAt + after)
    }

    @Composable private fun Play(s: InkState) = Games.Ink.Frozen(s, Phase.PLAYING)

    @Test fun ink_play() = shot("ink_play") { Play(play(3, 11)) }
    @Test fun ink_play_de() = shot("ink_play_de", "+de") { Play(play(3, 11)) }
    @Test fun ink_play_land() = shot("ink_play_land", "+land") { Play(play(3, 11)) }
    @Test fun ink_play_font200() = shot("ink_play_font200", fontScale = 2f) { Play(play(3, 11)) }
    @Test fun ink_play_360dp() = shot("ink_play_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(play(7, 6, seed = 4)) }
    // sechs Farben, gerade falsch beantwortet: alte Karte wackelt bernstein hinaus
    @Test fun ink_wrong() = shot("ink_wrong") { Play(play(7, 6, wrong = setOf(5), after = -40, seed = 4)) }
    @Test fun ink_tinted() = shot("ink_tinted") { Play(play(12, 9, seed = 8)) }
    @Test fun ink_tinted_de() = shot("ink_tinted_de", "+de") { Play(play(12, 9, seed = 8)) }
    @Test fun ink_colorblind() = shot("ink_colorblind") { Play(play(12, 5, safe = true, seed = 8)) }
    @Test fun ink_countdown() = shot("ink_countdown") { Games.Ink.Frozen(InkGame.start(21, 3, false), Phase.COUNTDOWN, count = 3) }
    @Test fun ink_countdown_de_font200() = shot("ink_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Ink.Frozen(InkGame.start(21, 3, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun ink_paused() = shot("ink_paused") { Games.Ink.Frozen(play(3, 11), Phase.PAUSED) }
}
