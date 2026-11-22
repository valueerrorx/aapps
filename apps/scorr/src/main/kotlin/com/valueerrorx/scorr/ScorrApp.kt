package com.valueerrorx.scorr

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.valueerrorx.aapps.core.ui.AappsTheme
import com.valueerrorx.aapps.core.ui.KeepScreenOn
import com.valueerrorx.aapps.core.ui.rememberHaptics
import com.valueerrorx.scorr.domain.Game
import com.valueerrorx.scorr.ui.EntryOverlay
import com.valueerrorx.scorr.ui.HistoryScreen
import com.valueerrorx.scorr.ui.PlayersScreen
import com.valueerrorx.scorr.ui.ScoreScreen
import com.valueerrorx.scorr.ui.ScorrAbout
import com.valueerrorx.scorr.ui.ScorrAccent
import com.valueerrorx.scorr.ui.ScorrSettings
import com.valueerrorx.scorr.ui.displayName
import com.valueerrorx.scorr.ui.playerColor

private enum class Screen { BOARD, HISTORY, PLAYERS, SETTINGS, ABOUT }

@Composable
fun ScorrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var game by remember { mutableStateOf(prefs.game) }
    // Stand vor „Neues Spiel", damit Rueckgaengig auch das zuruecknimmt (nur im Speicher)
    var beforeNew by remember { mutableStateOf<Game?>(null) }
    var rounds by remember { mutableStateOf(prefs.rounds) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var keepOn by remember { mutableStateOf(prefs.keepOn) }
    var screen by rememberSaveable { mutableStateOf(Screen.BOARD) }
    var entry by rememberSaveable { mutableIntStateOf(-1) }

    // Jede Aenderung sofort speichern: uebersteht Prozess-Tod
    fun update(g: Game, keepUndo: Boolean = false) {
        if (!keepUndo) beforeNew = null
        game = g
        prefs.game = g
    }

    fun newGame() {
        beforeNew = game
        update(game.newGame(), keepUndo = true)
    }

    AappsTheme(ScorrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        if (keepOn && screen == Screen.BOARD) KeepScreenOn()
        when (screen) {
            Screen.BOARD -> {
                if (entry >= 0) BackHandler { entry = -1 }
                val restore = beforeNew
                ScoreScreen(
                    game = game,
                    rounds = rounds,
                    haptics = haptics,
                    onScore = { p, d -> update(game.score(p, d, System.currentTimeMillis())) },
                    onEnter = { entry = it },
                    onUndo = {
                        val prev = beforeNew
                        if (!game.canUndo && prev != null) update(prev) else update(game.undo())
                    },
                    onNextRound = { update(game.endRound()) },
                    onNewGame = { newGame() },
                    onHistory = { screen = Screen.HISTORY },
                    onSettings = { screen = Screen.SETTINGS },
                    canUndo = game.canUndo || restore != null,
                    overlay = {
                        val i = entry
                        if (i in game.players.indices) {
                            EntryOverlay(
                                name = displayName(game, i),
                                color = playerColor(game.players[i].color),
                                score = game.scores[i],
                                onApply = { d ->
                                    update(game.enter(i, d, System.currentTimeMillis()))
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
                    onRename = { i, n -> update(game.rename(i, n), keepUndo = true) },
                    onRecolor = { i, k -> update(game.recolor(i, k), keepUndo = true) },
                    onRemove = { update(game.removePlayer(it)) },
                    onAdd = { update(game.addPlayer()) },
                    onBack = { screen = Screen.SETTINGS },
                )
            }
            Screen.SETTINGS -> {
                BackHandler { screen = Screen.BOARD }
                ScorrSettings(
                    playerNames = game.players.indices.map { displayName(game, it) }.joinToString(", "),
                    target = game.target,
                    onTarget = { update(game.withTarget(it), keepUndo = true) },
                    rule = game.rule,
                    onRule = { update(game.withRule(it), keepUndo = true) },
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
