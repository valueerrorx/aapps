package solutions.xapient.scorr

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import solutions.xapient.aapps.core.ui.AappsTheme
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
import solutions.xapient.scorr.ui.playerColor

private enum class Screen { BOARD, HISTORY, PLAYERS, SETTINGS, ABOUT }

@Composable
fun ScorrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    // Spiel samt Stand vor „Neues Spiel"
    var session by remember { mutableStateOf(prefs.session) }
    // nur zum Anzeigen; Aenderungen lesen immer session.game (kein veralteter Stand)
    val game = session.game
    var rounds by remember { mutableStateOf(prefs.rounds) }
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
        val view = LocalView.current
        // Abweisen nach Spielende; Haptics aus core kennt keinen Reject
        val reject = {
            if (hapticsOn) {
                view.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
                )
            }
        }
        if (keepOn && screen == Screen.BOARD) KeepScreenOn()
        when (screen) {
            Screen.BOARD -> {
                if (entry >= 0) BackHandler { entry = -1 }
                ScoreScreen(
                    game = game,
                    rounds = rounds,
                    haptics = haptics,
                    onScore = { p, d -> update(session.game.score(p, d, System.currentTimeMillis())) },
                    onEnter = { entry = it },
                    onUndo = { save(session.undo(rounds)) },
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
                                score = game.scores[i],
                                onApply = { d ->
                                    update(session.game.enter(i, d, System.currentTimeMillis()))
                                    haptics.tick()
                                    entry = -1
                                },
                                onDismiss = { entry = -1 },
                            )
                        }
                    },
                )
            }
            Screen.HISTORY -> {
                BackHandler { screen = Screen.BOARD }
                HistoryScreen(game, rounds) { screen = Screen.BOARD }
            }
            Screen.PLAYERS -> {
                BackHandler { screen = Screen.SETTINGS }
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
                BackHandler { screen = Screen.BOARD }
                ScorrSettings(
                    playerNames = game.players.indices.map { displayName(game, it) }.joinToString(", "),
                    target = game.target,
                    onTarget = { update(session.game.withTarget(it), keepUndo = true) },
                    rule = game.rule,
                    onRule = { update(session.game.withRule(it), keepUndo = true) },
                    rounds = rounds,
                    onRounds = { rounds = it; prefs.rounds = it },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    keepOn = keepOn,
                    onKeepOn = { keepOn = it; prefs.keepOn = it },
                    onPlayers = { screen = Screen.PLAYERS },
                    onNewGame = {
                        newGame()
                        haptics.confirm()
                        screen = Screen.BOARD
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.BOARD },
                )
            }
            Screen.ABOUT -> {
                BackHandler { screen = Screen.SETTINGS }
                ScorrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
