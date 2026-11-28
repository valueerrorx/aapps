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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

// Start: Kopf mit Datum, Serie und Woche, darunter alle Spiele als Lernpfad; oeffnet bei der naechsten Einheit von heute
@Composable
fun HomeScreen(
    today: Int,
    games: List<GameItem>,
    streak: Int,
    week: List<Boolean>,
    haptics: Haptics?,
    onPlay: (GameId) -> Unit,
    onGames: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = Aapps.colors
    val next = games.firstOrNull { it.daily && !it.doneToday }
    val allDone = games.any { it.daily } && next == null
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val action: @Composable () -> Unit = {
        AappsPillButton(
            stringResource(
                when {
                    allDone -> R.string.done_today
                    games.any { it.doneToday } -> R.string.go_on
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
    // beim Oeffnen zur naechsten offenen (sonst ersten) Einheit von heute scrollen
    val scroll = rememberScrollState()
    val focus = games.indexOf(next ?: games.firstOrNull { it.daily })
    var pathTop by remember { mutableStateOf<Float?>(null) }
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val density = LocalDensity.current
    AappsScaffold {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewH = maxHeight
            LaunchedEffect(pathTop) {
                val top = pathTop ?: return@LaunchedEffect
                if (focus < 0) return@LaunchedEffect
                scroll.scrollTo(with(density) { (top + (pathNodeY(focus, fs) - viewH * 0.4f).toPx()).toInt().coerceAtLeast(0) })
            }
            val path = Modifier.onPlaced { pathTop = it.positionInParent().y }
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
                        GamePath(games, play, path.padding(bottom = 24.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Box(Modifier.padding(start = 12.dp)) { Header(today, onGames, onSettings) }
                    Spacer(Modifier.height(12.dp))
                    StreakRow(streak, week, today, Modifier.padding(horizontal = 28.dp))
                    GamePath(games, play, path.padding(bottom = 112.dp))
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

// Waagrechte Lage der Stationen: sanfte S-Kurve Mitte, rechts, rechts, Mitte, links, links ...
private fun pathX(i: Int): Float = 0.5f + 0.26f * sin(i * 1.05f)

private val PATH_NODE = 76.dp
private val PATH_TOP = 40.dp
private fun pathStep(fs: Float): Dp = 116.dp + 28.dp * (fs - 1f)

// Mitte der Station i, gemessen ab Oberkante des Pfads
private fun pathNodeY(i: Int, fs: Float): Dp = PATH_TOP + PATH_NODE / 2 + pathStep(fs) * i

// Alle Spiele als geschwungener Pfad; Tipp startet frei
@Composable
private fun GamePath(games: List<GameItem>, onPlay: (GameId) -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val node = PATH_NODE
    val step = pathStep(fs)
    val top = PATH_TOP
    // ruhiger Sternenstaub, fest per Seed: kein Flimmern, keine Animation
    val dust = remember { Random(41).let { r -> List(40) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    BoxWithConstraints(modifier.fillMaxWidth().height(top * 2 + step * (games.size - 1).coerceAtLeast(0) + node)) {
        val w = maxWidth
        val cx = { i: Int -> w * pathX(i) }
        val cy = { i: Int -> pathNodeY(i, fs) }
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
    val parts = when {
        item.doneToday -> listOf(stringResource(R.string.today_done), level)
        item.daily -> listOf(stringResource(R.string.today), level)
        else -> listOf(level)
    }.map { it.uppercase().replace(' ', ' ') }
    val tagStyle = Aapps.type.label.copy(color = if (item.daily) c.accent else c.fgMuted)
    // passt die Zeile nicht, je Teil eine eigene Zeile statt Umbruch mitten drin
    val measurer = rememberTextMeasurer()
    val labelPx = with(LocalDensity.current) { labelW.roundToPx() }
    val oneLine = parts.joinToString("  ·  ")
    val tag = if (parts.size < 2 || measurer.measure(oneLine, tagStyle, maxLines = 1).size.width <= labelPx) oneLine
    else parts.joinToString("\n")
    val align = if (labelLeft) TextAlign.End else TextAlign.Start
    val label: @Composable () -> Unit = {
        Column(Modifier.width(labelW.coerceAtLeast(0.dp))) {
            BasicText(
                item.name, Modifier.fillMaxWidth(), style = Aapps.type.title.copy(color = c.fg, textAlign = align),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                tag, Modifier.fillMaxWidth(),
                style = tagStyle.copy(textAlign = align),
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
