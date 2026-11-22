package com.valueerrorx.scorr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valueerrorx.aapps.core.ui.Aapps
import com.valueerrorx.aapps.core.ui.AappsIcon
import com.valueerrorx.aapps.core.ui.AappsScaffold
import com.valueerrorx.aapps.core.ui.AappsTopBar
import com.valueerrorx.aapps.core.ui.SettingsDivider
import com.valueerrorx.aapps.core.ui.SettingsGroup
import com.valueerrorx.scorr.R
import com.valueerrorx.scorr.domain.Board
import com.valueerrorx.scorr.domain.Game
import com.valueerrorx.scorr.domain.WinRule

// Scrollbare Seite mit eigener Kopfzeile (wie SettingsScreen, aber mit Titel)
@Composable
fun ListScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(title, onBack, stringResource(com.valueerrorx.aapps.core.ui.R.string.aapps_back))
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

// Verlauf: Rangliste mit Summen, darunter die Runden (neueste oben)
@Composable
fun HistoryScreen(game: Game, rounds: Boolean, onBack: () -> Unit) {
    val title = if (rounds) stringResource(R.string.round_n, game.round) else stringResource(R.string.history)
    ListScreen(title, onBack) {
        Totals(game)
        if (!game.canUndo) {
            BasicText(
                stringResource(R.string.no_points),
                Modifier.padding(horizontal = 12.dp),
                style = Aapps.type.body.copy(color = Aapps.colors.fgMuted),
            )
        } else if (rounds) {
            RoundTable(game)
        }
    }
}

@Composable
private fun Totals(game: Game) {
    val c = Aapps.colors
    val scores = game.scores
    val order = scores.indices.sortedWith(
        if (game.rule == WinRule.MOST) compareByDescending { scores[it] } else compareBy { scores[it] },
    )
    val crowned = if (game.finished) game.winners else game.leaders
    SettingsGroup(stringResource(R.string.total)) {
        order.forEachIndexed { rank, i ->
            if (rank > 0) SettingsDivider()
            val color = playerColor(game.players[i].color)
            val name = displayName(game, i)
            val desc = stringResource(R.string.tile_desc, name, Board.format(scores[i]))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 10.dp)
                    .semantics(mergeDescendants = true) { contentDescription = desc },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(14.dp).background(color, CircleShape))
                Spacer(Modifier.width(14.dp))
                BasicText(name, Modifier.weight(1f), style = Aapps.type.body.copy(color = c.fg, fontSize = 17.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (i in crowned) {
                    AappsIcon(ScorrIcons.Crown, null, color, size = 20.dp)
                    Spacer(Modifier.width(10.dp))
                }
                BasicText(Board.format(scores[i]), style = Aapps.type.headline.copy(color = color, fontFeatureSettings = "tnum"), maxLines = 1)
            }
        }
    }
}

// Runden-Tabelle; bei vielen Spielern waagrecht scrollbar
@Composable
private fun RoundTable(game: Game) {
    val c = Aapps.colors
    val table = game.roundTable()
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val first = 64.dp * fs
    val col = 68.dp * fs
    val cell = Aapps.type.number.copy(fontSize = 17.sp, textAlign = TextAlign.End)
    SettingsGroup(stringResource(R.string.rounds_title)) {
        Column(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            // Kopf: Farbpunkt + Name je Spieler
            Row(Modifier.padding(horizontal = 12.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(first))
                game.players.forEachIndexed { i, p ->
                    Row(Modifier.width(col), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(playerColor(p.color), CircleShape))
                        Spacer(Modifier.width(5.dp))
                        BasicText(
                            displayName(game, i), Modifier.weight(1f, fill = false),
                            style = Aapps.type.label.copy(color = c.fgMuted, letterSpacing = 0.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            for (r in table.indices.reversed()) {
                val running = r == table.lastIndex
                Box(Modifier.padding(horizontal = 12.dp).width(first + col * game.players.size).heightIn(min = 1.dp).background(c.line.copy(alpha = 0.6f)))
                Row(Modifier.padding(horizontal = 12.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(first)) {
                        BasicText(stringResource(R.string.round_short, r + 1), style = Aapps.type.number.copy(color = if (running) c.accent else c.fgMuted, fontSize = 15.sp))
                        if (running) BasicText(stringResource(R.string.running).uppercase(), style = Aapps.type.label.copy(color = c.fgFaint, fontSize = 10.sp))
                    }
                    table[r].forEach { v ->
                        BasicText(
                            if (v == 0) "·" else Board.formatDelta(v),
                            Modifier.width(col),
                            style = cell.copy(color = if (v == 0) c.fgFaint else c.fg),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
