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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
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
import kotlin.math.sin
import kotlin.random.Random

data class DailyItem(val id: GameId, val name: String, val icon: ImageVector, val level: Int, val done: Boolean)

// Heute: Tagesbild als Hero, darunter Serie und Woche, beim Scrollen alle Spiele als Pfad; unten die eine Hauptaktion
@Composable
fun HomeScreen(
    today: Int,
    items: List<DailyItem>,
    games: List<GameItem>,
    streak: Int,
    week: List<Boolean>,
    haptics: Haptics?,
    onPlay: (GameId) -> Unit,
    onGames: () -> Unit,
    onSettings: () -> Unit,
    scroll: ScrollState = rememberScrollState(),
) {
    val c = Aapps.colors
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewH = maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(0.42f).fillMaxHeight().padding(start = 12.dp, bottom = 16.dp)) {
                        Header(today, onGames, onSettings)
                        Spacer(Modifier.weight(1f))
                        StreakRow(streak, week, today, Modifier.padding(start = 12.dp, end = 8.dp))
                        Spacer(Modifier.height(20.dp))
                        Box(Modifier.padding(start = 8.dp)) { action() }
                    }
                    Column(Modifier.weight(0.58f).fillMaxHeight().verticalScroll(scroll)) {
                        // Pfadtitel lugt unten hervor: zeigt, dass es weitergeht
                        Constellation(items, next?.id, play, Modifier.fillMaxWidth().height(viewH - 44.dp).padding(12.dp))
                        PathTitle()
                        GamePath(games, play, Modifier.padding(bottom = 24.dp))
                    }
                }
            } else {
                // Hero wie bisher, nur so viel kuerzer, dass der Pfad ueber der Pille hervorlugt
                val heroH = (viewH - 64.dp - 76.dp - 98.dp - 64.dp).coerceAtLeast(300.dp)
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Box(Modifier.padding(start = 12.dp)) { Header(today, onGames, onSettings) }
                    Constellation(items, next?.id, play, Modifier.fillMaxWidth().height(heroH).padding(horizontal = 12.dp))
                    StreakRow(streak, week, today, Modifier.padding(horizontal = 28.dp))
                    Spacer(Modifier.height(36.dp))
                    PathTitle()
                    GamePath(games, play, Modifier.padding(bottom = 112.dp))
                }
                // Pille schwebt ueber dem Pfad, weicher Uebergang statt harter Kante
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(0f to c.bg.copy(alpha = 0f), 0.45f to c.bg.copy(alpha = 0.92f), 1f to c.bg))
                        .padding(top = 30.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { action() }
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
        OrbFace(item.icon, item.done, isNext, size, Modifier.pressScale(pressed, 0.94f))
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

// Glaskugel mit Symbol; lit = hervorgehoben (naechstes bzw. heute dran), done = Akzent und Haken
@Composable
private fun OrbFace(icon: ImageVector, done: Boolean, lit: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val c = Aapps.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2
            when {
                done -> drawGlow(c.accent, center, r * 1.9f, 0.32f)
                lit -> drawGlow(c.accent, center, r * 1.8f, 0.2f)
            }
            drawCircle(
                Brush.radialGradient(listOf(c.surfaceHigh, c.bg), center = center.copy(y = center.y - r * 0.35f), radius = r * 1.3f),
                r, center,
            )
            val edge = when {
                done -> c.accent
                lit -> c.accent.copy(alpha = 0.6f)
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
        AappsIcon(icon, null, if (done) c.accent else c.fg, size = size * 0.36f)
        if (done) {
            val badge = (size * 0.3f).coerceIn(22.dp, 30.dp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-size * 0.04f), y = (-size * 0.04f))
                    .size(badge)
                    .background(c.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AappsIcon(AappsIcons.Check, null, c.onAccent, size = badge * 0.6f)
            }
        }
    }
}

@Composable
private fun PathTitle() {
    val c = Aapps.colors
    BasicText(
        stringResource(R.string.all_games).uppercase(),
        Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        style = Aapps.type.label.copy(color = c.fgMuted),
    )
}

// Waagrechte Lage der Stationen: sanfte S-Kurve Mitte, rechts, rechts, Mitte, links, links ...
private fun pathX(i: Int): Float = 0.5f + 0.26f * sin(i * 1.05f)

// Alle Spiele als geschwungener Pfad; Tipp startet frei
@Composable
private fun GamePath(games: List<GameItem>, onPlay: (GameId) -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val node = 76.dp
    val step = 116.dp + 28.dp * (fs - 1f)
    val top = 40.dp
    // eigener Sternenstaub, gleich ruhig wie im Hero
    val dust = remember { Random(41).let { r -> List(40) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    BoxWithConstraints(modifier.fillMaxWidth().height(top * 2 + step * (games.size - 1).coerceAtLeast(0) + node)) {
        val w = maxWidth
        val cx = { i: Int -> w * pathX(i) }
        val cy = { i: Int -> top + node / 2 + step * i }
        Canvas(Modifier.fillMaxSize()) {
            dust.forEach { (x, y, a) ->
                drawCircle(c.fg.copy(alpha = 0.05f + 0.13f * a), (0.6f + 1.0f * a).dp.toPx(), Offset(x * size.width, y * size.height))
            }
            if (games.size < 2) return@Canvas
            val pts = games.indices.map { Offset(cx(it).toPx(), cy(it).toPx()) }
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.zipWithNext().forEach { (a, b) ->
                    val dy = (b.y - a.y) / 2
                    cubicTo(a.x, a.y + dy, b.x, b.y - dy, b.x, b.y)
                }
            }
            // breites, ruhiges Band als Weg, darauf die gestrichelte Spur wie im Sternbild
            drawPath(path, c.surface, style = Stroke(18.dp.toPx(), cap = StrokeCap.Round))
            drawPath(
                path, c.fgFaint,
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx()))),
            )
        }
        games.forEachIndexed { i, g ->
            // Beschriftung auf die freie Seite: weg von der Kurve zur naechsten Station
            val bias = (pathX(i) - 0.5f) + (pathX(i + 1) - 0.5f) * 0.5f
            val labelLeft = bias > 0f
            val gap = 14.dp
            val labelW = if (labelLeft) cx(i) - node / 2 - gap - 20.dp else w - cx(i) - node / 2 - gap - 20.dp
            val x = if (labelLeft) cx(i) - node / 2 - gap - labelW else cx(i) - node / 2
            PathNode(g, node, labelLeft, labelW, gap, { onPlay(g.id) }, Modifier.offset(x, cy(i) - node / 2))
        }
    }
}

@Composable
private fun PathNode(item: GameItem, node: Dp, labelLeft: Boolean, labelW: Dp, gap: Dp, onClick: () -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val desc = stringResource(if (item.doneToday) R.string.orb_done else R.string.orb_open, item.name, item.level) +
        if (item.daily && !item.doneToday) ", " + stringResource(R.string.today) else ""
    val level = stringResource(R.string.level_n, item.level)
    val tag = when {
        item.doneToday -> stringResource(R.string.today_done) + "  ·  " + level
        item.daily -> stringResource(R.string.today) + "  ·  " + level
        else -> level
    }
    val align = if (labelLeft) TextAlign.End else TextAlign.Start
    val label: @Composable () -> Unit = {
        Column(Modifier.width(labelW.coerceAtLeast(0.dp))) {
            BasicText(
                item.name, Modifier.fillMaxWidth(), style = Aapps.type.title.copy(color = c.fg, textAlign = align),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                tag.uppercase(), Modifier.fillMaxWidth(),
                style = Aapps.type.label.copy(color = if (item.daily) c.accent else c.fgMuted, textAlign = align),
                maxLines = 2,
            )
        }
    }
    Row(
        modifier
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(source, indication = null, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (labelLeft) { label(); Spacer(Modifier.width(gap)) }
        OrbFace(item.icon, item.doneToday, item.daily, node, Modifier.pressScale(pressed, 0.94f))
        if (!labelLeft) { Spacer(Modifier.width(gap)); label() }
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
