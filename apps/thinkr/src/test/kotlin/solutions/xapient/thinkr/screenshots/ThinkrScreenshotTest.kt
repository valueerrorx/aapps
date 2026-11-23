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
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.GameId
import solutions.xapient.thinkr.domain.echo.EchoGame
import solutions.xapient.thinkr.domain.echo.EchoState
import solutions.xapient.thinkr.ui.DailyItem
import solutions.xapient.thinkr.ui.GameItem
import solutions.xapient.thinkr.ui.GamesScreen
import solutions.xapient.thinkr.ui.HomeScreen
import solutions.xapient.thinkr.ui.LevelItem
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ResultScreen
import solutions.xapient.thinkr.ui.ResultView
import solutions.xapient.thinkr.ui.ThinkrAbout
import solutions.xapient.thinkr.ui.ThinkrAccent
import solutions.xapient.thinkr.ui.ThinkrIcons
import solutions.xapient.thinkr.ui.ThinkrSettings

// Screenshot-Zustaende laut SPEC.md; record: :apps:thinkr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ThinkrScreenshotTest {

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

    // Mittwoch; Serie ueber Montag und Dienstag
    private val today = 20261125
    private val week = listOf(true, true, false, false, false, false, false)
    private val weekDone = listOf(true, true, true, false, false, false, false)

    @Composable
    private fun Home(done: Boolean = false) = HomeScreen(
        today = today,
        items = listOf(DailyItem(GameId.ECHO, "Echo", ThinkrIcons.Echo, 6, done)),
        streak = if (done) 5 else 4,
        week = if (done) weekDone else week,
        haptics = null, onPlay = {}, onGames = {}, onSettings = {},
    )

    @Test fun home() = shot("home") { Home() }
    @Test fun home_de() = shot("home_de", "+de") { Home() }
    @Test fun home_land() = shot("home_land", "+land") { Home() }
    @Test fun home_font200() = shot("home_font200", fontScale = 2f) { Home() }
    @Test fun home_360dp() = shot("home_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Home() }
    @Test fun home_done() = shot("home_done") { Home(done = true) }

    // Layout-Vorschau mit drei Kugeln: Platzhalter, bis weitere Spiele da sind
    @Composable
    private fun Home3() = HomeScreen(
        today = today,
        items = listOf(
            DailyItem(GameId.ECHO, "Echo", ThinkrIcons.Echo, 6, true),
            DailyItem(GameId.SWITCHES, "Echo", ThinkrIcons.Echo, 3, false),
            DailyItem(GameId.STARS, "Echo", ThinkrIcons.Echo, 1, false),
        ),
        streak = 0, week = List(7) { false }, haptics = null, onPlay = {}, onGames = {}, onSettings = {},
    )

    @Test fun preview_home3() = shot("preview_home3") { Home3() }
    @Test fun preview_home3_land() = shot("preview_home3_land", "+land") { Home3() }

    @Composable
    private fun GamesList() = GamesScreen(
        listOf(GameItem(GameId.ECHO, "Echo", ThinkrIcons.Echo, 6, 4280, daily = true, doneToday = false)),
        onPlay = {}, onBack = {},
    )

    @Test fun games() = shot("games") { GamesList() }
    @Test fun games_de() = shot("games_de", "+de") { GamesList() }
    @Test fun games_land() = shot("games_land", "+land") { GamesList() }
    @Test fun games_font200() = shot("games_font200", fontScale = 2f) { GamesList() }

    // 1-back mitten im Spiel: 3 richtig, 1 falsch, 10 richtig = Multiplikator 3
    private fun echoPlay(): EchoState {
        var s = EchoGame.start(11, 4, false)
        s = EchoGame.tick(s, s.dueAt)
        repeat(14) { i ->
            val right = i != 3
            val truth = s.deck.matches[s.index]
            s = EchoGame.input(s, if (right) truth else !truth, s.shownAt + 700)
        }
        return EchoGame.tick(s, s.shownAt + 450)
    }

    // 2-back: zwei Merk-Karten, dann fuenf Antworten
    private fun echoTwoBack(): EchoState {
        var s = EchoGame.start(5, 11, false)
        s = EchoGame.tick(s, s.dueAt)
        s = EchoGame.tick(s, s.dueAt)
        repeat(5) { s = EchoGame.input(s, s.deck.matches[s.index], s.shownAt + 900) }
        return EchoGame.tick(s, s.shownAt + 1400)
    }

    @Composable private fun EchoPlay() = Games.Echo.Frozen(echoPlay(), Phase.PLAYING)

    @Test fun echo_play() = shot("echo_play") { EchoPlay() }
    @Test fun echo_play_de() = shot("echo_play_de", "+de") { EchoPlay() }
    @Test fun echo_play_land() = shot("echo_play_land", "+land") { EchoPlay() }
    @Test fun echo_play_font200() = shot("echo_play_font200", fontScale = 2f) { EchoPlay() }
    @Test fun echo_2back() = shot("echo_2back") { Games.Echo.Frozen(echoTwoBack(), Phase.PLAYING) }
    @Test fun echo_countdown() = shot("echo_countdown") { Games.Echo.Frozen(EchoGame.start(11, 4, false), Phase.COUNTDOWN, count = 3) }
    @Test fun echo_countdown_de_font200() = shot("echo_countdown_de_font200", "+de", fontScale = 2f) {
        Games.Echo.Frozen(EchoGame.start(5, 11, false), Phase.COUNTDOWN, count = 2)
    }
    @Test fun echo_paused() = shot("echo_paused") { Games.Echo.Frozen(echoPlay(), Phase.PAUSED) }

    private val best = ResultView(R.string.game_echo,ThinkrIcons.Echo, 4280, 6, 5, 88, 4280, newBest = true, dailyComplete = false, hasNext = false)

    @Composable private fun ResultBest() = ResultScreen(best, null, {}, {}, {}, animate = false)

    @Test fun result_best() = shot("result_best") { ResultBest() }
    @Test fun result_best_de() = shot("result_best_de", "+de") { ResultBest() }
    @Test fun result_best_land() = shot("result_best_land", "+land") { ResultBest() }
    @Test fun result_best_font200() = shot("result_best_font200", fontScale = 2f) { ResultBest() }
    @Test fun result_best_shine() = shot("result_best_shine") { ResultScreen(best, null, {}, {}, {}, animate = false, shineAt = 0.45f) }
    @Test fun result_daily() = shot("result_daily") {
        ResultScreen(
            best.copy(score = 2950, level = 6, prevLevel = 6, accuracy = 81, newBest = false, dailyComplete = true),
            null, {}, {}, {}, animate = false,
        )
    }

    @Composable
    private fun Settings() = ThinkrSettings(
        relaxed = false, onRelaxed = {}, haptics = true, onHaptics = {}, sound = false, onSound = {},
        levels = listOf(LevelItem(GameId.ECHO, "Echo", 6)), onReset = {}, onAbout = {}, onBack = {},
    )

    @Test fun settings() = shot("settings") { Settings() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { ThinkrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { ThinkrAbout("0.1.0") {} }
}
