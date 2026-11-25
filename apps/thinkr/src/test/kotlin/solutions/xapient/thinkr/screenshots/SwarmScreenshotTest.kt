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
import solutions.xapient.thinkr.domain.swarm.Flock
import solutions.xapient.thinkr.domain.swarm.SwarmGame
import solutions.xapient.thinkr.domain.swarm.SwarmState
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrAccent

// Schwarm-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SwarmScreenshotTest {

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

    // richtig antworten, bis ein passender Schwarm dran ist (mindestens min Antworten), dann after ms festhalten
    private fun play(seed: Long, level: Int, min: Int, after: Long = 900, want: (Flock) -> Boolean): SwarmState {
        var s = SwarmGame.start(seed, level, false)
        var n = 0
        while (n < min || !want(s.flock)) {
            s = SwarmGame.input(s, s.flock.answer, s.shownAt + 650)
            n++
        }
        return SwarmGame.tick(s, s.shownAt + after)
    }

    private fun mixed() = play(6, 5, 9) { !it.congruent }
    private fun hollow() = play(12, 12, 6, after = 1_400) { it.hollow && it.drift != it.point && !it.congruent }

    // gerade richtig gewischt: alter Schwarm zieht davon
    private fun leaving(): SwarmState {
        val s = play(3, 8, 7) { !it.congruent }
        return SwarmGame.tick(SwarmGame.input(s, s.flock.answer, s.round.now), s.round.now + 160)
    }

    @Composable private fun Play(s: SwarmState) = Games.Swarm.Frozen(s, Phase.PLAYING)

    @Test fun swarm_play() = shot("swarm_play") { Play(mixed()) }
    @Test fun swarm_play_land() = shot("swarm_play_land", "+land") { Play(mixed()) }
    @Test fun swarm_play_de() = shot("swarm_play_de", "+de") { Play(mixed()) }
    @Test fun swarm_play_360dp() = shot("swarm_play_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(play(9, 9, 5) { it.size == 9 }) }
    @Test fun swarm_hollow() = shot("swarm_hollow") { Play(hollow()) }
    @Test fun swarm_hollow_land() = shot("swarm_hollow_land", "+land") { Play(hollow()) }
    @Test fun swarm_leaving() = shot("swarm_leaving") { Play(leaving()) }
    @Test fun swarm_countdown() = shot("swarm_countdown") { Games.Swarm.Frozen(SwarmGame.start(6, 5, false), Phase.COUNTDOWN, count = 3) }
    @Test fun swarm_countdown_hollow_de_font200() = shot("swarm_countdown_hollow_de_font200", "+de", fontScale = 2f) {
        Games.Swarm.Frozen(SwarmGame.start(12, 12, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun swarm_paused() = shot("swarm_paused") { Games.Swarm.Frozen(mixed(), Phase.PAUSED) }
}
