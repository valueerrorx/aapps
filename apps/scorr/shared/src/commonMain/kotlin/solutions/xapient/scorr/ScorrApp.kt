package solutions.xapient.scorr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.scorr.domain.Game
import solutions.xapient.scorr.domain.Session
import solutions.xapient.scorr.ui.EntryOverlay
import solutions.xapient.scorr.ui.HistoryScreen
import solutions.xapient.scorr.ui.PlayersScreen
import solutions.xapient.scorr.ui.ScoreScreen
import solutions.xapient.scorr.ui.ScorrAbout
import solutions.xapient.scorr.ui.ScorrAccent
import solutions.xapient.scorr.ui.ScorrSettings
import solutions.xapient.scorr.ui.displayName
import solutions.xapient.scorr.ui.nowMs
import solutions.xapient.scorr.ui.playerColor

const val APP_NAME = "scorr"

private enum class Screen { BOARD, HISTORY, PLAYERS, SETTINGS, ABOUT }

/** Abweisen nach Spielende; Android View-Haptik REJECT, iOS Fehler-Haptik */
@Composable
expect fun rememberReject(enabled: Boolean): () -> Unit

// Gemeinsam fuer Android und iOS; Speicher kommt von der Plattform
@Composable
fun ScorrApp(prefs: Prefs, versionName: String) {
    // Spiel samt Stand vor „Neues Spiel"
    var session by remember { mutableStateOf(prefs.session) }
    // nur zum Anzeigen; Aenderungen lesen immer session.game (kein veralteter Stand)
    val game = session.game
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var keepOn by remember { mutableStateOf(prefs.keepOn) }
    var screen by rememberSaveable { mutableStateOf(Screen.BOARD) }
    var entry by rememberSaveable { mutableIntStateOf(-1) }

    // Jede Aenderung sofort speichern: uebersteht Prozess-Tod
    fun save(s: Session) {
        session = s
        prefs.session = s
    }

    fun update(g: Game, keepUndo: Boolean = false) = save(if (keepUndo) session.edit(g) else session.play(g))

    fun newGame() = save(session.newGame())

    AappsTheme(ScorrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        // Abweisen nach Spielende; Haptics aus core kennt keinen Reject
        val reject = rememberReject(hapticsOn)
        if (keepOn && screen == Screen.BOARD) KeepScreenOn()
        when (screen) {
            Screen.BOARD -> {
                if (entry >= 0) BackEffect { entry = -1 }
                ScoreScreen(
                    game = game,
                    haptics = haptics,
                    onScore = { p, d -> update(session.game.score(p, d, nowMs())) },
                    onEnter = { entry = it },
                    onUndo = { save(session.undo()) },
                    onNextRound = { update(session.game.endRound()) },
                    onNewGame = { newGame() },
                    onHistory = { screen = Screen.HISTORY },
                    onSettings = { screen = Screen.SETTINGS },
                    canUndo = session.canUndo,
                    onReject = reject,
                    overlay = {
                        val i = entry
                        if (i in game.players.indices) {
                            EntryOverlay(
                                name = displayName(game, i),
                                color = playerColor(game.players[i].color),
                                score = game.tileValues[i],
                                onApply = { d ->
                                    update(session.game.enter(i, d, nowMs()))
                                    haptics.press()
                                    entry = -1
                                },
                                onDismiss = { entry = -1 },
                            )
                        }
                    },
                )
            }
            Screen.HISTORY -> {
                BackEffect { screen = Screen.BOARD }
                HistoryScreen(game) { screen = Screen.BOARD }
            }
            Screen.PLAYERS -> {
                BackEffect { screen = Screen.SETTINGS }
                PlayersScreen(
                    game = game,
                    onRename = { i, n -> update(session.game.rename(i, n), keepUndo = true) },
                    onRecolor = { i, k -> update(session.game.recolor(i, k), keepUndo = true) },
                    onRemove = { update(session.game.removePlayer(it)) },
                    onAdd = { update(session.game.addPlayer()) },
                    onBack = { screen = Screen.SETTINGS },
                )
            }
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.BOARD }
                ScorrSettings(
                    playerNames = game.players.indices.map { displayName(game, it) }.joinToString(", "),
                    target = game.target,
                    onTarget = { update(session.game.withTarget(it), keepUndo = true) },
                    rule = game.rule,
                    onRule = { update(session.game.withRule(it), keepUndo = true) },
                    rounds = game.rounds,
                    onRounds = { update(session.game.withRounds(it), keepUndo = true) },
                    scoring = game.scoring,
                    onScoring = { update(session.game.withScoring(it), keepUndo = true) },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    keepOn = keepOn,
                    onKeepOn = { keepOn = it; prefs.keepOn = it },
                    onPlayers = { screen = Screen.PLAYERS },
                    onNewGame = {
                        newGame()
                        haptics.press()
                        screen = Screen.BOARD
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.BOARD },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                ScorrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
