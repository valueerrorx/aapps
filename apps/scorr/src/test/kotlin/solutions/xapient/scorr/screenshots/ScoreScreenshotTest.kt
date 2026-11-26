package solutions.xapient.scorr.screenshots

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.scorr.domain.Game
import solutions.xapient.scorr.domain.Player
import solutions.xapient.scorr.domain.Scoring
import solutions.xapient.scorr.domain.WinRule
import solutions.xapient.scorr.ui.EntryOverlay
import solutions.xapient.scorr.ui.HistoryScreen
import solutions.xapient.scorr.ui.PlayersScreen
import solutions.xapient.scorr.ui.ScoreScreen
import solutions.xapient.scorr.ui.ScorrAbout
import solutions.xapient.scorr.ui.ScorrAccent
import solutions.xapient.scorr.ui.ScorrSettings
import solutions.xapient.scorr.ui.playerColor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot-Zustaende laut SPEC.md; record: :apps:scorr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScoreScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(ScorrAccent) { content() }
        }
    }

    // Zuege im Abstand > Zusammenfass-Fenster; t = Zeit des letzten Zugs
    private fun Game.play(vararg moves: Pair<Int, Int>, endRound: Boolean = false): Game {
        var g = this
        var t = (g.lastAt.takeIf { it != Long.MIN_VALUE } ?: 0L)
        moves.forEach { (p, d) -> t += 10_000; g = g.score(p, d, t) }
        return if (endRound) g.endRound() else g
    }

    // Runde 3, Anna fuehrt mit zwei Rundensiegen
    private fun two() = Game(listOf(Player("Anna", 0), Player("Ben", 2)), rounds = true)
        .play(0 to 7, 1 to 5, endRound = true)
        .play(0 to 9, 1 to 6, endRound = true)
        .play(0 to 8, 1 to 3)

    private fun four() = Game(listOf(Player("Lena", 1), Player("Max", 2), Player("Sophie", 3), Player("Jonas", 4)), rounds = true)
        .play(0 to 12, 1 to 9, 2 to 15, 3 to 4, endRound = true)
        .play(0 to 10, 1 to 14, 2 to 11, 3 to 6, endRound = true)
        .play(0 to 9, 1 to 8, 2 to 13, 3 to 5, endRound = true)
        .play(0 to 11, 1 to 6, 2 to 12, 3 to 3)

    private fun eight() = Game((0 until 8).map { Player("", it) })
        .play(0 to 14, 1 to 9, 2 to -3, 3 to 22, 4 to 7, 5 to 11, 6 to 0, 7 to 5)

    // Wertung Punkte gesamt: Ziel 100 sofort erreicht
    private fun finishedGame() = Game(listOf(Player("Lena", 1), Player("Max", 2), Player("Sophie", 3)), rounds = true)
        .withScoring(Scoring.POINTS)
        .withTarget(100)
        .play(0 to 34, 1 to 28, 2 to 30, endRound = true)
        .play(0 to 31, 1 to 40, 2 to 22, endRound = true)
        .play(0 to 39, 1 to 25, 2 to 27)

    // Wertung Rundensiege: Ziel 2 Siege nach Runde 3 erreicht
    private fun finishedByWins() = Game(listOf(Player("Lena", 1), Player("Max", 2), Player("Sophie", 3)), rounds = true)
        .withTarget(2)
        .play(0 to 34, 1 to 28, 2 to 30, endRound = true)
        .play(0 to 31, 1 to 40, 2 to 22, endRound = true)
        .play(0 to 39, 1 to 25, 2 to 27, endRound = true)

    @Composable
    private fun Board(
        g: Game, scrub: Pair<Int, Int>? = null, shine: Float = -1f, confirm: Boolean = false,
        overlay: @Composable () -> Unit = {},
    ) {
        ScoreScreen(
            g, haptics = null, onScore = { _, _ -> }, onEnter = {}, onUndo = {}, onNextRound = {}, onNewGame = {},
            onHistory = {}, onSettings = {}, clock = { g.lastAt + 400 }, animate = false, scrubPreview = scrub, shineAt = shine,
            confirmNew = confirm, overlay = overlay,
        )
    }

    // Zug-Anzeige nur bei board4: sonst ist der letzte Zug schon alt
    private fun Game.settled() = copy(lastAt = Long.MIN_VALUE)

    @Composable private fun Two() = Board(two().settled())

    @Test fun board() = shot("board") { Two() }
    @Test fun board_de() = shot("board_de", "+de") { Two() }
    @Test fun board_360dp() = shot("board_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Two() }
    @Test fun board_land() = shot("board_land", "+land") { Two() }
    @Test fun board_font130() = shot("board_font130", fontScale = 1.3f) { Two() }
    @Test fun board_font200() = shot("board_font200", fontScale = 2f) { Two() }
    @Test fun board_land_font200() = shot("board_land_font200", "+land", fontScale = 2f) { Two() }

    @Composable private fun Four() = Board(four().play(3 to 1).let { it.score(3, 2, it.lastAt + 300) })

    @Test fun board4() = shot("board4") { Four() }
    @Test fun board4_land() = shot("board4_land", "+land") { Four() }

    @Composable private fun Eight() = Board(eight().settled())

    @Test fun board8() = shot("board8") { Eight() }
    @Test fun board8_land() = shot("board8_land", "+land") { Eight() }

    // Grenzwerte: laengste Zahl darf nie abgeschnitten werden
    @Test fun board8_land_extreme() = shot("board8_land_extreme", "+land") {
        Board(eight().play(1 to -1_000_000, 3 to 999_999).settled())
    }

    // kompakte Kacheln mit Runden: unten nur die Siege
    @Test fun board8_rounds() = shot("board8_rounds") {
        Board(eight().withRounds(true).endRound().play(3 to 4, 5 to 2).settled())
    }

    @Test fun scrub() = shot("scrub") { Board(two().settled(), scrub = 1 to 7) }

    // Store DE wie EN
    @Test fun board4_de() = shot("board4_de", "+de") { Four() }
    @Test fun scrub_de() = shot("scrub_de", "+de") { Board(two().settled(), scrub = 1 to 7) }
    @Test fun board8_rounds_de() = shot("board8_rounds_de", "+de") {
        Board(eight().withRounds(true).endRound().play(3 to 4, 5 to 2).settled())
    }

    // Ruhezustand nach dem Glanz; winner_shine zeigt den Glanz mitten im Lauf
    @Composable private fun Winner() = Board(finishedGame().settled())

    @Test fun winner_shine() = shot("winner_shine") { Board(finishedGame().settled(), shine = 0.45f) }
    @Test fun winner_again_land_font200_de() = shot("winner_again_land_font200_de", "+de-land", fontScale = 2f) {
        Board(finishedGame().settled(), confirm = true)
    }
    @Test fun board_land_font200_de() = shot("board_land_font200_de", "+de-land", fontScale = 2f) { Two() }

    @Test fun winner() = shot("winner") { Winner() }
    @Test fun winner_de() = shot("winner_de", "+de") { Winner() }
    @Test fun winner_land() = shot("winner_land", "+land") { Winner() }
    @Test fun winner_font200() = shot("winner_font200", fontScale = 2f) { Winner() }
    @Test fun winner_land_font200() = shot("winner_land_font200", "+land", fontScale = 2f) { Winner() }
    @Test fun winner_wins() = shot("winner_wins") { Board(finishedByWins().settled()) }

    @Composable
    private fun Entry() {
        val g = four().settled()
        Board(g) {
            EntryOverlay("Sophie", playerColor(3), g.tileValues[2], {}, {}, initial = "37", focus = false)
        }
    }

    @Test fun entry() = shot("entry") { Entry() }
    @Test fun entry_de() = shot("entry_de", "+de") { Entry() }
    @Test fun entry_land() = shot("entry_land", "+land") { Entry() }
    @Test fun entry_font200() = shot("entry_font200", fontScale = 2f) { Entry() }
    @Test fun entry_land_font200() = shot("entry_land_font200", "+land", fontScale = 2f) { Entry() }

    @Composable private fun History() = HistoryScreen(four().settled()) {}

    @Test fun history() = shot("history") { History() }
    @Test fun history_de() = shot("history_de", "+de") { History() }
    @Test fun history_land() = shot("history_land", "+land") { History() }
    @Test fun history_font200() = shot("history_font200", fontScale = 2f) { History() }
    @Test fun history_land_font200() = shot("history_land_font200", "+land", fontScale = 2f) { History() }
    @Test fun history_eight() = shot("history_eight") { HistoryScreen(eight().withRounds(true).settled().endRound().play(3 to 4)) {} }

    @Composable
    private fun Players() {
        val g = four().settled().addPlayer()
        PlayersScreen(g, { _, _ -> }, { _, _ -> }, {}, {}, {}, openColors = 1)
    }

    @Test fun players() = shot("players") { Players() }
    @Test fun players_de() = shot("players_de", "+de") { Players() }
    @Test fun players_land() = shot("players_land", "+land") { Players() }
    @Test fun players_font200() = shot("players_font200", fontScale = 2f) { Players() }
    @Test fun players_land_font200() = shot("players_land_font200", "+land", fontScale = 2f) { Players() }

    @Composable
    private fun Settings() {
        ScorrSettings(
            playerNames = "Lena, Max, Sophie, Jonas", target = 3, onTarget = {}, rule = WinRule.MOST, onRule = {},
            rounds = true, onRounds = {}, scoring = Scoring.WINS, onScoring = {}, haptics = true, onHaptics = {}, keepOn = true, onKeepOn = {},
            onPlayers = {}, onNewGame = {}, onAbout = {}, onBack = {},
        )
    }

    @Test fun settings() = shot("settings") { Settings() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { ScorrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { ScorrAbout("0.1.0") {} }
}
