package solutions.xapient.thinkr.ui

import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_back
import solutions.xapient.thinkr.res.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.thinkr.domain.GameId

data class GameItem(val id: GameId, val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val level: Int, val best: Int, val daily: Boolean, val doneToday: Boolean)

// Alle Spiele mit Stufe und Bestwert; Tipp startet frei
@Composable
fun GamesScreen(items: List<GameItem>, onPlay: (GameId) -> Unit, onBack: () -> Unit) {
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(stringResource(Res.string.games), onBack, stringResource(CoreRes.string.aapps_back))
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { GameRow(it, onPlay) }
            }
        }
    }
}

@Composable
private fun GameRow(item: GameItem, onPlay: (GameId) -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) c.surfaceHigh else c.surface, Springs.gentle(), label = "gameRow")
    val best = if (item.best > 0) stringResource(Res.string.best_n, formatPoints(item.best)) else null
    val sub = listOfNotNull(stringResource(Res.string.level_n, item.level), best).joinToString("  ·  ")
    val today = when {
        item.doneToday -> stringResource(Res.string.today_done)
        item.daily -> stringResource(Res.string.today)
        else -> null
    }
    val desc = listOfNotNull(item.name, sub, today).joinToString(", ")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(source, indication = null, role = Role.Button) { onPlay(item.id) }
            .heightIn(min = 88.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(56.dp)) {
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(c.surfaceHigh, c.bg), center = center.copy(y = center.y - r * 0.35f), radius = r * 1.3f), r)
                drawCircle(if (item.daily) c.accent.copy(alpha = 0.6f) else c.line, r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            }
            AappsIcon(item.icon, null, c.fg, size = 26.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            BasicText(item.name, style = Aapps.type.title.copy(color = c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(sub, style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
        }
        if (today != null) {
            Spacer(Modifier.width(8.dp))
            TodayChip(today, item.doneToday)
        }
        Spacer(Modifier.width(4.dp))
        AappsIcon(AappsIcons.Chevron, null, c.fgMuted, size = 20.dp)
    }
}

@Composable
private fun TodayChip(text: String, done: Boolean) {
    val c = Aapps.colors
    Row(
        Modifier
            .background(if (done) c.accent.copy(alpha = 0.16f) else Color.Transparent, CircleShape)
            .border(1.dp, c.accent.copy(alpha = if (done) 0.0f else 0.5f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            AappsIcon(AappsIcons.Check, null, c.accent, size = 14.dp)
            Spacer(Modifier.width(4.dp))
        }
        BasicText(text.uppercase(), style = Aapps.type.label.copy(color = c.accent), maxLines = 1)
    }
}
