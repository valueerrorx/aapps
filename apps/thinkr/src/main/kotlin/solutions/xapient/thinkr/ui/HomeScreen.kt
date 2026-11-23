package solutions.xapient.thinkr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.Days
import solutions.xapient.thinkr.domain.GameId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.random.Random

data class DailyItem(val id: GameId, val name: String, val icon: ImageVector, val level: Int, val done: Boolean)

// Heute: Tagesbild als Hero, darunter Serie und Woche, unten die eine Hauptaktion
@Composable
fun HomeScreen(
    today: Int,
    items: List<DailyItem>,
    streak: Int,
    week: List<Boolean>,
    haptics: Haptics?,
    onPlay: (GameId) -> Unit,
    onGames: () -> Unit,
    onSettings: () -> Unit,
) {
    val next = items.firstOrNull { !it.done }
    val allDone = items.isNotEmpty() && next == null
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val action: @Composable () -> Unit = {
        AappsPillButton(
            stringResource(
                when {
                    allDone -> R.string.done_today
                    items.any { it.done } -> R.string.go_on
                    else -> R.string.start
                },
            ),
            { if (next != null) { haptics?.press(); onPlay(next.id) } },
            Modifier.widthIn(min = 196.dp),
            icon = if (allDone) AappsIcons.Check else ThinkrIcons.Play,
            active = !allDone,
            enabled = !allDone,
        )
    }
    val play: (GameId) -> Unit = { haptics?.press(); onPlay(it) }
    AappsScaffold {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.42f).fillMaxHeight().padding(start = 12.dp, bottom = 16.dp)) {
                    Header(today, onGames, onSettings)
                    Spacer(Modifier.weight(1f))
                    StreakRow(streak, week, today, Modifier.padding(start = 12.dp, end = 8.dp))
                    Spacer(Modifier.height(20.dp))
                    Box(Modifier.padding(start = 8.dp)) { action() }
                }
                Constellation(items, next?.id, play, Modifier.weight(0.58f).fillMaxHeight().padding(12.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.padding(start = 12.dp)) { Header(today, onGames, onSettings) }
                Constellation(items, next?.id, play, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp))
                StreakRow(streak, week, today, Modifier.padding(horizontal = 28.dp))
                Box(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 20.dp), contentAlignment = Alignment.Center) { action() }
            }
        }
    }
}

@Composable
private fun Header(today: Int, onGames: () -> Unit, onSettings: () -> Unit) {
    val c = Aapps.colors
    val locale = LocalConfiguration.current.locales[0]
    val pattern = stringResource(R.string.date_pattern)
    val date = remember(today, locale, pattern) { Days.date(today).format(DateTimeFormatter.ofPattern(pattern, locale)) }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            date.uppercase(locale),
            Modifier.weight(1f).padding(start = 12.dp),
            style = Aapps.type.label.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AappsIconButton(ThinkrIcons.Games, stringResource(R.string.games), onGames)
        AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
    }
}

// Lage der Kugeln im Hero, normiert; Hochformat im Zickzack, Querformat als flacher Bogen
private fun spots(n: Int, wide: Boolean): List<Offset> = when (n) {
    0 -> emptyList()
    1 -> listOf(Offset(0.5f, 0.47f))
    2 -> if (wide) listOf(Offset(0.3f, 0.42f), Offset(0.7f, 0.58f)) else listOf(Offset(0.33f, 0.3f), Offset(0.67f, 0.7f))
    else -> if (wide) {
        listOf(Offset(0.18f, 0.36f), Offset(0.5f, 0.64f), Offset(0.82f, 0.38f))
    } else {
        listOf(Offset(0.3f, 0.2f), Offset(0.7f, 0.5f), Offset(0.32f, 0.8f))
    }
}

// Tagesbild: Spiele als Lichtkugeln, verbunden zu einem kleinen Sternbild
@Composable
private fun Constellation(items: List<DailyItem>, next: GameId?, onPlay: (GameId) -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    BoxWithConstraints(modifier) {
        val wide = maxWidth > maxHeight * 1.15f
        val pts = spots(items.size, wide)
        val k = when (items.size) {
            1 -> 0.46f
            2 -> 0.34f
            else -> 0.28f
        }
        val orb = min(min(maxWidth, maxHeight) * k, 168.dp).coerceAtLeast(84.dp)
        val density = LocalDensity.current
        val orbPx = with(density) { orb.toPx() }
        // ruhiger Sternenstaub, fest per Seed: kein Flimmern, keine Animation
        val dust = remember { Random(23).let { r -> List(46) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
        Canvas(Modifier.fillMaxSize()) {
            dust.forEach { (x, y, a) ->
                drawCircle(c.fg.copy(alpha = 0.06f + 0.16f * a), (0.6f + 1.1f * a).dp.toPx(), Offset(x * size.width, y * size.height))
            }
            val centers = pts.map { Offset(it.x * size.width, it.y * size.height) }
            centers.zipWithNext().forEachIndexed { i, (a, b) ->
                val lit = items[i].done && items[i + 1].done
                val d = b - a
                val len = d.getDistance()
                if (len <= orbPx) return@forEachIndexed
                val u = d / len
                val gap = orbPx / 2 + 8.dp.toPx()
                val p0 = a + u * gap
                val p1 = b - u * gap
                if (lit) {
                    drawLine(c.accent.copy(alpha = 0.25f), p0, p1, 8.dp.toPx(), StrokeCap.Round)
                    drawLine(c.accent, p0, p1, 2.dp.toPx(), StrokeCap.Round)
                } else {
                    drawLine(
                        c.fgFaint, p0, p1, 1.5.dp.toPx(), StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx())),
                    )
                }
            }
        }
        val colW = orb * 1.7f
        items.forEachIndexed { i, item ->
            val p = pts[i]
            Orb(
                item, item.id == next, orb, { onPlay(item.id) },
                Modifier
                    .width(colW)
                    .offset {
                        IntOffset(
                            (p.x * constraints.maxWidth - colW.toPx() / 2).toInt(),
                            (p.y * constraints.maxHeight - orbPx / 2).toInt(),
                        )
                    },
            )
        }
    }
}

@Composable
private fun Orb(item: DailyItem, isNext: Boolean, size: Dp, onClick: () -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val desc = stringResource(if (item.done) R.string.orb_done else R.string.orb_open, item.name, item.level)
    Column(
        modifier
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(source, indication = null, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size).pressScale(pressed, 0.94f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val r = this.size.minDimension / 2
                when {
                    item.done -> drawGlow(c.accent, center, r * 1.9f, 0.32f)
                    isNext -> drawGlow(c.accent, center, r * 1.8f, 0.2f)
                }
                drawCircle(
                    Brush.radialGradient(listOf(c.surfaceHigh, c.bg), center = center.copy(y = center.y - r * 0.35f), radius = r * 1.3f),
                    r, center,
                )
                val edge = when {
                    item.done -> c.accent
                    isNext -> c.accent.copy(alpha = 0.6f)
                    else -> c.line
                }
                drawCircle(edge, r - 0.75.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
                // Reflex oben
                drawArc(
                    c.fg.copy(alpha = 0.16f), 215f, 110f, false,
                    topLeft = Offset(center.x - r * 0.78f, center.y - r * 0.78f),
                    size = androidx.compose.ui.geometry.Size(r * 1.56f, r * 1.56f),
                    style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            AappsIcon(item.icon, null, if (item.done) c.accent else c.fg, size = size * 0.36f)
            if (item.done) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-size * 0.04f), y = (-size * 0.04f))
                        .size(30.dp)
                        .background(c.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AappsIcon(AappsIcons.Check, null, c.onAccent, size = 18.dp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BasicText(
            item.name, style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            stringResource(R.string.level_n, item.level).uppercase(),
            style = Aapps.type.label.copy(color = if (item.done) c.accent else c.fgMuted, textAlign = TextAlign.Center),
            maxLines = 1,
        )
    }
}

// Serie ohne Druck plus Woche Mo–So
@Composable
private fun StreakRow(streak: Int, week: List<Boolean>, today: Int, modifier: Modifier) {
    val c = Aapps.colors
    // grosse Schrift: Woche unter die Serie, sonst draengen sich die Wochentage
    val fs = LocalDensity.current.fontScale
    val dayW = 20.dp * fs.coerceIn(1f, 1.6f)
    val texts: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            if (streak > 0) {
                BasicText(streak.toString(), style = Aapps.type.headline.copy(color = c.fg))
                BasicText(
                    pluralStringResource(R.plurals.streak_days, streak).uppercase(),
                    style = Aapps.type.label.copy(color = c.fgMuted),
                )
            } else {
                BasicText(stringResource(R.string.streak_new), style = Aapps.type.body.copy(color = c.fgMuted))
            }
        }
    }
    if (fs > 1.4f) {
        Column(modifier.fillMaxWidth()) {
            texts(Modifier)
            Spacer(Modifier.height(14.dp))
            WeekDots(week, today, dayW)
        }
    } else {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            texts(Modifier.weight(1f).padding(end = 12.dp))
            WeekDots(week, today, dayW)
        }
    }
}

@Composable
private fun WeekDots(week: List<Boolean>, today: Int, dayW: Dp) {
    val c = Aapps.colors
    val locale = LocalConfiguration.current.locales[0]
    val todayIndex = Days.weekday(today)
    val mon = Days.monday(today)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 7) {
            run {
                val day = Days.date(Days.add(mon, i))
                val letter = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
                val done = week.getOrElse(i) { false }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(dayW)) {
                    Canvas(Modifier.size(12.dp)) {
                        val r = size.minDimension / 2
                        when {
                            done -> {
                                drawGlow(c.accent, center, r * 2.6f, 0.3f)
                                drawCircle(c.accent, r, center)
                            }
                            i == todayIndex -> drawCircle(c.accent, r - 0.75.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
                            i < todayIndex -> drawCircle(c.line, r * 0.55f, center)
                            else -> drawCircle(c.fgFaint, r - 0.6.dp.toPx(), center, style = Stroke(1.2.dp.toPx()))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        letter,
                        style = Aapps.type.label.copy(
                            color = if (i == todayIndex) c.fg else c.fgFaint,
                            letterSpacing = 0.sp,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
