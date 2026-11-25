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
import solutions.xapient.thinkr.domain.drops.DropsGame
import solutions.xapient.thinkr.domain.drops.DropsKeys
import solutions.xapient.thinkr.domain.drops.DropsState
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrAccent

// Tropfen-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class DropsScreenshotTest {

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

    // spielen: jeden Tropfen loesen, sobald er halb gefallen ist; misses Tropfen fallen lassen
    private fun play(seed: Long, level: Int, until: Long, misses: Int = 0): DropsState {
        var s = DropsGame.start(seed, level, false)
        var t = 0L
        while (t < until) {
            t += 50
            s = DropsGame.tick(s, t)
            // erst ein paar ins Wasser fallen lassen
            if (s.splashes < misses) continue
            val d = s.drops.firstOrNull { it.progress(t) > 0.55f } ?: continue
            d.sum.answer.toString().forEach { s = DropsGame.input(s, it.digitToInt(), t) }
            if (s.drops.contains(d)) s = DropsGame.input(s, DropsKeys.OK, t)
        }
        return s
    }

    // mitten im Spiel, gerade einer geplatzt, eine Ziffer getippt
    private fun playing(): DropsState {
        val s = play(4, 6, 14_000, misses = 1)
        val d = s.drops.maxByOrNull { it.progress(s.round.now) } ?: return s
        return DropsGame.tick(DropsGame.input(s, d.sum.answer.toString().first().digitToInt(), s.round.now), s.round.now + 120)
    }

    private fun busy() = play(9, 12, 20_000, misses = 2).let { DropsGame.tick(it, it.round.now + 200) }

    @Composable private fun Play(s: DropsState) = Games.Drops.Frozen(s, Phase.PLAYING)

    @Test fun drops_play() = shot("drops_play") { Play(playing()) }
    @Test fun drops_play_de() = shot("drops_play_de", "+de") { Play(playing()) }
    @Test fun drops_play_land() = shot("drops_play_land", "+land") { Play(playing()) }
    @Test fun drops_play_font200() = shot("drops_play_font200", fontScale = 2f) { Play(playing()) }
    @Test fun drops_play_360dp() = shot("drops_play_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(busy()) }
    @Test fun drops_busy() = shot("drops_busy") { Play(busy()) }
    @Test fun drops_countdown() = shot("drops_countdown") { Games.Drops.Frozen(DropsGame.start(4, 6, false), Phase.COUNTDOWN, count = 3) }
    @Test fun drops_countdown_de_font200() = shot("drops_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Drops.Frozen(DropsGame.start(4, 6, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun drops_paused() = shot("drops_paused") { Games.Drops.Frozen(playing(), Phase.PAUSED) }
}
