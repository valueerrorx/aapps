package solutions.xapient.scorr

import androidx.compose.runtime.Composable
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
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

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    BOARD, BOARD4, SCRUB, WINNER, BOARD8_ROUNDS, HISTORY, ENTRY, PLAYERS, BOARD8, WINNER_WINS, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// Zuege im Abstand > Zusammenfass-Fenster
private fun Game.play(vararg moves: Pair<Int, Int>, endRound: Boolean = false): Game {
    var g = this
    var t = g.lastAt.takeIf { it != Long.MIN_VALUE } ?: 0L
    moves.forEach { (p, d) -> t += 10_000; g = g.score(p, d, t) }
    return if (endRound) g.endRound() else g
}

private fun Game.settled() = copy(lastAt = Long.MIN_VALUE)

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
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(ScorrAccent) {
        when (scene) {
            Scene.BOARD -> Board(two().settled())
            // Zug-Anzeige „+3": letzter Zug gerade eben
            Scene.BOARD4 -> Board(four().play(3 to 1).let { it.score(3, 2, it.lastAt + 300) })
            Scene.SCRUB -> Board(two().settled(), scrub = 1 to 7)
            Scene.WINNER -> Board(finishedGame().settled())
            Scene.BOARD8_ROUNDS -> Board(eight().withRounds(true).endRound().play(3 to 4, 5 to 2).settled())
            Scene.HISTORY -> HistoryScreen(four().settled()) {}
            Scene.ENTRY -> {
                val g = four().settled()
                Board(g) { EntryOverlay("Sophie", playerColor(3), g.tileValues[2], {}, {}, initial = "37", focus = false) }
            }
            Scene.PLAYERS -> PlayersScreen(four().settled().addPlayer(), { _, _ -> }, { _, _ -> }, {}, {}, {}, openColors = 1)
            Scene.BOARD8 -> Board(eight().settled())
            Scene.WINNER_WINS -> Board(finishedByWins().settled())
            Scene.SETTINGS -> ScorrSettings(
                playerNames = "Lena, Max, Sophie, Jonas", target = 3, onTarget = {}, rule = WinRule.MOST, onRule = {},
                rounds = true, onRounds = {}, scoring = Scoring.WINS, onScoring = {}, haptics = true, onHaptics = {},
                keepOn = true, onKeepOn = {}, onPlayers = {}, onNewGame = {}, onAbout = {}, onBack = {},
            )
            Scene.ABOUT -> ScorrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Board(g: Game, scrub: Pair<Int, Int>? = null, overlay: @Composable () -> Unit = {}) = ScoreScreen(
    g, haptics = null, onScore = { _, _ -> }, onEnter = {}, onUndo = {}, onNextRound = {}, onNewGame = {},
    onHistory = {}, onSettings = {}, clock = { g.lastAt + 400 }, animate = false, scrubPreview = scrub,
    overlay = overlay,
)

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
