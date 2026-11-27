package solutions.xapient.thinkr.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.thinkr.Games
import solutions.xapient.thinkr.domain.switches.Net
import solutions.xapient.thinkr.domain.switches.NetBook
import solutions.xapient.thinkr.domain.switches.Pilot
import solutions.xapient.thinkr.domain.switches.SwitchesGame
import solutions.xapient.thinkr.domain.switches.SwitchesState
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ResultScreen
import solutions.xapient.thinkr.ui.ResultView
import solutions.xapient.thinkr.ui.ThinkrAccent
import solutions.xapient.thinkr.ui.ThinkrIcons
import solutions.xapient.thinkr.ui.switches.SwitchesBoard

// Weichen-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SwitchesScreenshotTest {

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

    // Mitten im Spiel: alles richtig, eine Perle absichtlich daneben, kurz danach festgehalten
    private fun play(seed: Long, level: Int, until: Long, wrong: Set<Int> = emptySet()): SwitchesState =
        Pilot.play(SwitchesGame.start(seed, level, false), until, wrong = wrong)

    private fun levelOne() = play(3, 1, 23_350)
    private fun eight() = play(17, 6, 31_200, wrong = setOf(6))
    private fun ten() = play(29, 10, 27_400)

    @Composable private fun Play(s: SwitchesState) = Games.Switches.Frozen(s, Phase.PLAYING)

    @Test fun switches_play() = shot("switches_play") { Play(levelOne()) }
    @Test fun switches_play_land() = shot("switches_play_land", "+land") { Play(levelOne()) }
    @Test fun switches_play_de() = shot("switches_play_de", "+de") { Play(levelOne()) }
    @Test fun switches_play_font200() = shot("switches_play_font200", fontScale = 2f) { Play(levelOne()) }
    @Test fun switches_play_360dp() = shot("switches_play_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Play(eight()) }
    @Test fun switches_eight() = shot("switches_eight") { Play(eight()) }
    @Test fun switches_eight_land() = shot("switches_eight_land", "+land") { Play(eight()) }
    @Test fun switches_ten() = shot("switches_ten") { Play(ten()) }
    @Test fun switches_ten_land() = shot("switches_ten_land", "+land") { Play(ten()) }
    @Test fun switches_countdown() = shot("switches_countdown") { Games.Switches.Frozen(SwitchesGame.start(3, 1, false), Phase.COUNTDOWN, count = 3) }
    @Test fun switches_countdown_de_font200() = shot("switches_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Switches.Frozen(SwitchesGame.start(17, 6, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun switches_paused() = shot("switches_paused") { Games.Switches.Frozen(levelOne(), Phase.PAUSED) }

    private val result = ResultView(
        Games.Switches.name, ThinkrIcons.Switches, 1840, 4, 3, 93, 1840,
        newBest = true, dailyComplete = false, hasNext = false, tally = true, hits = 27, total = 29,
    )

    @Test fun result_switches() = shot("result_switches") { ResultScreen(result, null, {}, {}, {}, animate = false) }
    @Test fun result_switches_de() = shot("result_switches_de", "+de") { ResultScreen(result, null, {}, {}, {}, animate = false) }

    // Alle Netze der Sammlung auf einen Blick (Grundvariante), zum Pruefen der Handarbeit
    @Test fun switches_nets() = shot("switches_nets", "w1000dp-h1400dp-hdpi") {
        val plans = NetBook.plans.toSortedMap().flatMap { (g, list) -> list.mapIndexed { i, rows -> "$g·${'a' + i}" to rows } }
        val base = SwitchesGame.start(1, 1, false)
        AappsScaffold {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                plans.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        row.forEach { (name, rows) ->
                            val net = Net.parse(rows, 0)
                            val s = base.copy(
                                net = net,
                                colors = List(net.gateCount) { it },
                                on = List(net.switches.size) { false },
                                flippedAt = List(net.switches.size) { SwitchesGame.LONG_AGO },
                            )
                            Box(Modifier.weight(1f).fillMaxSize()) {
                                SwitchesBoard(s, live = false, count = 0) {}
                                BasicText(name, Modifier.padding(4.dp), style = Aapps.type.label.copy(color = Aapps.colors.fgMuted))
                            }
                        }
                        repeat(5 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
