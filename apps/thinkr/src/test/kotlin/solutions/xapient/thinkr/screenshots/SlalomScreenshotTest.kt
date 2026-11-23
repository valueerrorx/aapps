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
import solutions.xapient.thinkr.domain.slalom.SlalomGame
import solutions.xapient.thinkr.domain.slalom.SlalomRules
import solutions.xapient.thinkr.domain.slalom.SlalomState
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrAccent

// Slalom-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SlalomScreenshotTest {

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

    // fahren wie ein guter Spieler bis until; dodge = letzter Wechsel kurz vor dem Ende
    private fun drive(seed: Long, level: Int, until: Long, crashAt: Long = -1): SlalomState {
        var s = SlalomGame.start(seed, level, false)
        var t = 0L
        while (t < until && !(crashAt >= 0 && s.crashes > 0)) {
            t += 16
            s = SlalomGame.tick(s, t)
            val row = s.rows.firstOrNull { it.z > s.z } ?: continue
            val wantCrash = crashAt in 0..t && s.crashes == 0
            val target = if (wantCrash) row.blocked.first() else row.safe
            if (s.lane != target && t - s.laneAt > SlalomRules.LANE_MS * 2) s = SlalomGame.input(s, if (target > s.lane) 1 else -1, t)
        }
        return s
    }

    // kurz nach einem Spurwechsel: Gleiter neigt sich
    private fun banking(seed: Long, level: Int, until: Long): SlalomState {
        val s = drive(seed, level, until)
        val dir = if (s.lane == 0) 1 else -1
        return SlalomGame.tick(SlalomGame.input(s, dir, s.round.now), s.round.now + 90)
    }

    @Composable private fun Play(s: SlalomState) = Games.Slalom.Frozen(s, Phase.PLAYING)

    @Test fun slalom_play() = shot("slalom_play") { Play(banking(4, 3, 9_000)) }
    @Test fun slalom_play_de() = shot("slalom_play_de", "+de") { Play(banking(4, 3, 9_000)) }
    @Test fun slalom_play_land() = shot("slalom_play_land", "+land") { Play(banking(4, 3, 9_000)) }
    @Test fun slalom_play_360dp() = shot("slalom_play_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(drive(6, 7, 14_000)) }
    @Test fun slalom_fast() = shot("slalom_fast") { Play(drive(8, 12, 41_000)) }
    @Test fun slalom_crash() = shot("slalom_crash") { Play(drive(5, 6, 30_000, crashAt = 6_000).let { SlalomGame.tick(it, it.crashAt + 120) }) }
    @Test fun slalom_countdown() = shot("slalom_countdown") { Games.Slalom.Frozen(SlalomGame.start(4, 3, false), Phase.COUNTDOWN, count = 3) }
    @Test fun slalom_countdown_de_font200() = shot("slalom_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Slalom.Frozen(SlalomGame.start(4, 3, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun slalom_paused() = shot("slalom_paused") { Games.Slalom.Frozen(drive(4, 3, 9_000), Phase.PAUSED) }
}
