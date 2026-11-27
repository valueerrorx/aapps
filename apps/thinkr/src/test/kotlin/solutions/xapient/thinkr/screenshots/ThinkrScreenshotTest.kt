package solutions.xapient.thinkr.screenshots

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
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
import solutions.xapient.thinkr.domain.Daily
import solutions.xapient.thinkr.domain.echo.EchoGame
import solutions.xapient.thinkr.domain.echo.EchoState
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
    private val levels = listOf(6, 3, 4, 5, 2, 7, 4, 3, 5)

    // Lernpfad mit der echten Tagesauswahl; done = wie viele davon schon erledigt
    @Composable
    private fun Home(done: Int = 0) = HomeScreen(
        today = today,
        games = Games.all.mapIndexed { i, def ->
            val pick = Daily.pick(today, Games.ids)
            val daily = def.id in pick
            GameItem(def.id, stringResource(def.name), def.icon, levels[i], 0, daily, daily && pick.indexOf(def.id) < done)
        },
        streak = if (done == 3) 5 else 4,
        week = if (done == 3) weekDone else week,
        haptics = null, onPlay = {}, onGames = {}, onSettings = {},
    )

    @Test fun home() = shot("home") { Home() }
    @Test fun home_de() = shot("home_de", "+de") { Home() }
    @Test fun home_land() = shot("home_land", "+land") { Home() }
    @Test fun home_font200() = shot("home_font200", fontScale = 2f) { Home() }
    @Test fun home_360dp() = shot("home_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Home() }
    @Test fun home_partly() = shot("home_partly") { Home(done = 1) }
    @Test fun home_partly_land() = shot("home_partly_land", "+land") { Home(done = 2) }
    @Test fun home_done() = shot("home_done") { Home(done = 3) }

    @Composable
    private fun GamesList() = GamesScreen(
        Games.all.mapIndexed { i, def ->
            val daily = def.id in Daily.pick(today, Games.ids)
            GameItem(def.id, stringResource(def.name), def.icon, listOf(6, 3, 4, 5, 2, 7, 4, 3, 5)[i], listOf(4280, 1840, 2310, 3650, 0, 2120, 3380, 1460, 5210)[i], daily = daily, doneToday = daily && i == 0)
        },
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

    private val best = ResultView(Games.Echo.name, ThinkrIcons.Echo, 4280, 6, 5, 88, 4280, newBest = true, dailyComplete = false, hasNext = false)

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
        relaxed = false, onRelaxed = {}, safeColors = false, onSafeColors = {}, haptics = true, onHaptics = {}, sound = false, onSound = {},
        levels = Games.all.mapIndexed { i, def -> LevelItem(def.id, stringResource(def.name), listOf(6, 3, 4, 5, 1, 7, 4, 3, 5)[i]) }, onReset = {}, onAbout = {}, onBack = {},
    )

    @Test fun settings() = shot("settings") { Settings() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { ThinkrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { ThinkrAbout("0.1.0") {} }
}
