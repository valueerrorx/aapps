package solutions.xapient.scorr.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.scorr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_settings
import solutions.xapient.scorr.domain.Board
import solutions.xapient.scorr.domain.Game
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

// Anzeigename: leer = „Spieler n"
@Composable
fun displayName(game: Game, i: Int): String =
    game.players.getOrNull(i)?.name?.ifBlank { null } ?: stringResource(Res.string.player_n, i + 1)

// Bestaetigung fuer „Neues Spiel" verfaellt nach dieser Zeit
private const val CONFIRM_MS = 3000L

// Tafel: Kacheln als Hero, oben Runde/Rueckgaengig/Zahnrad, unten die eine Hauptaktion
@Composable
fun ScoreScreen(
    game: Game,
    haptics: Haptics?,
    onScore: (player: Int, delta: Int) -> Unit,
    onEnter: (player: Int) -> Unit,
    onUndo: () -> Unit,
    onNextRound: () -> Unit,
    onNewGame: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    canUndo: Boolean = game.canUndo,
    onReject: () -> Unit = {},
    clock: () -> Long = ::nowMs,
    animate: Boolean = true,
    scrubPreview: Pair<Int, Int>? = null,
    shineAt: Float = -1f,
    confirmNew: Boolean = false,
    overlay: @Composable () -> Unit = {},
) {
    // Zug-Anzeige: nach Ablauf des Fensters einmal neu zeichnen
    var now by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(game.lastAt) {
        now = clock()
        if (animate && game.lastAt != Long.MIN_VALUE) {
            delay((game.lastAt + Game.MERGE_MS - now).coerceAtLeast(0) + 16)
            now = clock()
        }
    }

    // Glanz einmal, sobald das Spiel entschieden ist
    val shine = remember { Animatable(-1f) }
    val finished = game.finished
    var announced by remember { mutableIntStateOf(if (finished) 1 else 0) }
    LaunchedEffect(finished) {
        if (!finished) { announced = 0; return@LaunchedEffect }
        if (announced == 1 || !animate) return@LaunchedEffect
        announced = 1
        haptics?.confirm()
        shine.snapTo(0f)
        shine.animateTo(1f, spring(dampingRatio = 1f, stiffness = 40f))
        shine.snapTo(-1f)
    }

    // „Neues Spiel" auf der Tafel: zweiter Tipp bestaetigt, sonst verfaellt es
    var confirm by remember { mutableStateOf(confirmNew) }
    if (confirm && animate) LaunchedEffect(Unit) { delay(CONFIRM_MS); confirm = false }
    LaunchedEffect(finished) { if (!finished) confirm = false }

    val rounds = game.rounds
    AappsScaffold {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        // grosse Schrift im Querformat: Bedienspalte breiter, Hauptaktion ohne Icon, damit sie einzeilig bleibt
        val bigFont = LocalDensity.current.fontScale > 1.5f
        val board: @Composable (Modifier) -> Unit = { m ->
            TileGrid(
                game, landscape, now, haptics, onScore, onEnter, onReject, scrubPreview,
                { if (shineAt >= 0f) shineAt else shine.value }, m,
            )
        }
        val plainAction = landscape && bigFont
        val action: @Composable () -> Unit = {
            when {
                finished -> AappsPillButton(
                    stringResource(if (confirm) Res.string.new_game_again else Res.string.new_game),
                    {
                        if (confirm) {
                            confirm = false
                            haptics?.press()
                            onNewGame()
                        } else {
                            haptics?.press()
                            confirm = true
                        }
                    },
                    Modifier.widthIn(min = 176.dp),
                    icon = if (plainAction) null else AappsIcons.Rotate, active = true,
                )
                rounds -> AappsPillButton(
                    stringResource(Res.string.next_round),
                    {
                        haptics?.press()
                        // Rundensieg ist ein Ereignis
                        if (game.pendingRoundWinners.isNotEmpty()) haptics?.confirm()
                        onNextRound()
                    },
                    Modifier.widthIn(min = 176.dp), icon = if (plainAction) null else ScorrIcons.Plus,
                )
            }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                val side = if (bigFont) 0.4f else 0.3f
                Column(
                    Modifier.weight(side).fillMaxHeight().padding(start = 12.dp, top = 8.dp, bottom = 16.dp),
                ) {
                    RoundChip(game, rounds, onHistory)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UndoButton(canUndo, haptics, onUndo)
                        GearButton(onSettings)
                    }
                    Spacer(Modifier.weight(1f))
                    WinnerLine(game, Modifier.padding(start = 6.dp, bottom = 12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { action() }
                }
                board(Modifier.weight(1f - side).fillMaxHeight().padding(top = 12.dp, end = 12.dp, bottom = 12.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { RoundChip(game, rounds, onHistory) }
                    UndoButton(canUndo, haptics, onUndo)
                    GearButton(onSettings)
                }
                board(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp))
                if (finished || rounds) {
                    Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp), contentAlignment = Alignment.Center) { action() }
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        // Ansage fuer TalkBack: Knoten bleibt stehen, nur der Text kommt dazu
        val announce = if (finished) {
            stringResource(Res.string.announce_winner, game.winners.sorted().map { displayName(game, it) }.joinToString(", "))
        } else ""
        Box(Modifier.size(1.dp).semantics { contentDescription = announce; liveRegion = LiveRegionMode.Polite })
        overlay()
    }
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    AappsIconButton(AappsIcons.Gear, stringResource(CoreRes.string.aapps_settings), onClick)
}

// Rueckgaengig bleibt an seinem Platz, ist ohne Verlauf aber unsichtbar
@Composable
private fun UndoButton(enabled: Boolean, haptics: Haptics?, onUndo: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0f, Springs.gentle(), label = "undo")
    Box(Modifier.graphicsLayer { this.alpha = alpha }) {
        if (enabled) {
            AappsIconButton(ScorrIcons.Undo, stringResource(Res.string.undo), { haptics?.press(); onUndo() })
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

// Runde (bzw. Verlauf) als Chip, Ziel daneben oder bei Platzmangel darunter; oeffnet den Verlauf
@Composable
private fun RoundChip(game: Game, rounds: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val title = (if (rounds) stringResource(Res.string.round_n, game.round) else stringResource(Res.string.history)).uppercase()
    val target = game.target?.let {
        if (game.countsWins) pluralStringResource(Res.plurals.target_short_wins, it, Board.format(it)).uppercase()
        else stringResource(Res.string.target_short, Board.format(it)).uppercase()
    }
    val style = Aapps.type.label.copy(color = c.fg, fontSize = Aapps.type.label.fontSize * 1.1f)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints {
        // Icon, Abstaende und Rand des Chips
        val chrome = with(density) { (12 + 18 + 8 + 16 + 2).dp.roundToPx() }
        val stacked = target != null &&
            measurer.measure("$title  ·  $target", style, maxLines = 1, softWrap = false).size.width + chrome > constraints.maxWidth
        Row(
            Modifier
                .pressScale(pressed, 0.96f)
                .heightIn(min = 48.dp)
                .clickable(source, indication = null, role = Role.Button, onClickLabel = stringResource(Res.string.history), onClick = onClick)
                .padding(vertical = 6.dp)
                .background(if (pressed) c.surfaceHigh else Color.Transparent, RoundedCornerShape(24.dp))
                .border(1.dp, if (pressed) c.fgFaint else c.line, RoundedCornerShape(24.dp))
                .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AappsIcon(ScorrIcons.Table, null, c.accent, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            if (stacked) {
                Column {
                    BasicText(title, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText(target!!, style = style.copy(color = c.fgMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                BasicText(title, style = style, maxLines = 1)
                if (target != null) {
                    BasicText("  ·  $target", style = style.copy(color = c.fgMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// Im Querformat links: wer gewonnen hat
@Composable
private fun WinnerLine(game: Game, modifier: Modifier) {
    if (!game.finished) return
    val w = game.winners.sorted()
    val color = if (w.size == 1) playerColor(game.players[w[0]].color) else Aapps.colors.fg
    Column(modifier) {
        BasicText(
            stringResource(if (w.size == 1) Res.string.winner else Res.string.winners).uppercase(),
            style = Aapps.type.label.copy(color = Aapps.colors.fgMuted),
        )
        BasicText(
            w.map { displayName(game, it) }.joinToString(", "),
            style = Aapps.type.headline.copy(color = color, fontSize = 26.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Platz um die Zahl: Innenabstand seitlich, Band fuer Name und Plakette oben/unten
private fun tilePad(compact: Boolean): Dp = if (compact) 12.dp else 18.dp

private fun numberBand(fontScale: Float): Dp = 22.dp + 18.dp * fontScale.coerceAtMost(2f)

// Breiteste Zahl gleicher Laenge: Ziffern als „0", mindestens zwei Stellen
private fun widest(text: String): String {
    val digits = text.count { it.isDigit() }.coerceAtLeast(2)
    return (if (text.startsWith("−")) "−" else "") + "0".repeat(digits)
}

@Composable
private fun TileGrid(
    game: Game,
    landscape: Boolean,
    now: Long,
    haptics: Haptics?,
    onScore: (Int, Int) -> Unit,
    onEnter: (Int) -> Unit,
    onReject: () -> Unit,
    scrubPreview: Pair<Int, Int>?,
    shine: () -> Float,
    modifier: Modifier,
) {
    val n = game.players.size
    val grid = Board.grid(n, landscape)
    val gap = if (n > 4) 8.dp else 10.dp
    val compact = n > 4
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val baseStyle = Aapps.type.display.copy(lineHeight = 1.em)
    BoxWithConstraints(modifier) {
        // eine Zahlengroesse fuer alle Kacheln, gemessen an der Standard-Kachel und der laengsten Zahl
        val cellW = (maxWidth - gap * (grid.cols - 1)) / grid.cols
        val cellH = (maxHeight - gap * (grid.rows - 1)) / grid.rows
        val pad = tilePad(compact)
        val availW = with(density) { (cellW - pad * 2).toPx() }
        val availH = with(density) { (cellH - numberBand(density.fontScale) * 2).toPx().coerceAtLeast(24.dp.toPx()) }
        val maxPx = with(density) { 320.dp.toPx() }
        val common = widest(game.tileValues.map { Board.format(it) }.maxBy { it.length })
        val fit: (String) -> TextUnit = { text ->
            val ref = if (text.length > common.length) widest(text) else common
            val probe = 100.sp
            val size = measurer.measure(ref, baseStyle.copy(fontSize = probe), maxLines = 1, softWrap = false).size
            val probePx = with(density) { probe.toPx() }
            val px = min(min(availH * 0.92f, probePx * availW * 0.97f / size.width.coerceAtLeast(1)), maxPx)
            with(density) { px.toSp() }
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            for (r in 0 until grid.rows) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (k in 0 until min(grid.cols, n - r * grid.cols)) {
                        val i = r * grid.cols + k
                        // Spieler-Index als Schluessel: Zustand bleibt bei Aenderungen an seiner Kachel
                        androidx.compose.runtime.key(i) {
                            Tile(
                                game, i, now, haptics, onScore, onEnter, onReject,
                                scrubPreview?.takeIf { it.first == i }?.second ?: 0,
                                shine, compact, fit,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class Gesture { TAP, DRAG, CANCEL }

// Wartet bis Loslassen (Tippen), Bewegung (Ziehen) oder Abbruch
private suspend fun AwaitPointerEventScope.tapOrDrag(down: PointerInputChange, slop: Float): Gesture {
    var result: Gesture? = null
    while (result == null) {
        val ch = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
        result = when {
            ch == null -> Gesture.CANCEL
            !ch.pressed -> Gesture.TAP
            abs(ch.position.y - down.position.y) > slop -> Gesture.DRAG
            abs(ch.position.x - down.position.x) > slop * 2 -> Gesture.CANCEL
            else -> null
        }
    }
    return result
}

// Rest einer Geste schlucken, bis alle Finger oben sind
private suspend fun AwaitPointerEventScope.swallow() {
    do {
        val e = awaitPointerEvent()
        e.changes.forEach { it.consume() }
    } while (e.changes.any { it.pressed })
}

@Composable
private fun Tile(
    game: Game,
    index: Int,
    now: Long,
    haptics: Haptics?,
    onScore: (Int, Int) -> Unit,
    onEnter: (Int) -> Unit,
    onReject: () -> Unit,
    initialScrub: Int,
    shine: () -> Float,
    compact: Boolean,
    fit: (String) -> TextUnit,
    modifier: Modifier,
) {
    val player = game.players[index]
    val base = playerColor(player.color)
    val score = game.tileValues[index]
    // klein: Rundensiege und Gesamtpunkte, nur mit Runden
    val stats = if (game.rounds) game.wins[index] to game.scores[index] else null
    val finished = game.finished
    val isWinner = index in game.winners
    val isLeader = !finished && index in game.leaders
    val name = displayName(game, index)
    val burst = game.burst(index, now)

    // Ziehen: Schritte bis zum Loslassen, gebucht wird erst dann
    val scrub = remember { mutableIntStateOf(initialScrub) }
    // gedrueckte Haelfte: 1 oben, -1 unten; nur im Zeichnen gelesen
    val pressed = remember { mutableIntStateOf(0) }
    val pressTop = animateFloatAsState(if (pressed.intValue == 1) 1f else 0f, Springs.snappy(), label = "pt")
    val pressBottom = animateFloatAsState(if (pressed.intValue == -1) 1f else 0f, Springs.snappy(), label = "pb")
    // Verlierer werden zur dunklen Karte; nur im Zeichnen gelesen
    val dim = animateFloatAsState(if (finished && !isWinner) 1f else 0f, Springs.gentle(), label = "dim")
    // kurzes Nachfedern bei jeder Aenderung, nicht beim ersten Anzeigen
    val pop = remember { Animatable(1f) }
    val seen = remember { intArrayOf(score) }
    LaunchedEffect(score) {
        if (score == seen[0]) return@LaunchedEffect
        seen[0] = score
        pop.snapTo(0.95f)
        pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
    }
    val score1 = rememberUpdatedState(onScore)
    val enter1 = rememberUpdatedState(onEnter)
    val reject1 = rememberUpdatedState(onReject)
    val locked = rememberUpdatedState(finished)

    val c = Aapps.colors
    val radius = if (compact) 22.dp else 28.dp
    val shape = RoundedCornerShape(radius)
    val plusLabel = stringResource(Res.string.action_plus)
    val minusLabel = stringResource(Res.string.action_minus)
    val enterLabel = stringResource(Res.string.action_enter)
    val desc = if (isWinner) stringResource(Res.string.tile_desc_winner, name) else name
    val points = if (stats != null) {
        stringResource(Res.string.tile_state_rounds, Board.format(game.roundPoints[index]), stats.first, Board.format(stats.second))
    } else stringResource(Res.string.points_n, Board.format(score))

    Box(
        modifier
            .graphicsLayer {
                // Verlierer treten leicht zurueck
                val s = pop.value * (1f - 0.03f * dim.value)
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .drawWithCache {
                val top = lerp(base, Color.White, 0.10f)
                val bottom = lerp(base, Color.Black, 0.18f)
                val fill = Brush.verticalGradient(listOf(top, base, bottom))
                val light = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.18f, 0f), radius = size.maxDimension * 0.8f,
                )
                val edge = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.55f), 0.25f to Color.White.copy(alpha = 0.08f), 1f to Color.Black.copy(alpha = 0.18f),
                )
                val r = radius.toPx()
                val w = 1.5.dp.toPx()
                val half = size.height / 2f
                val inset = Offset(w / 2, w / 2)
                val inner = size.copy(size.width - w, size.height - w)
                onDrawWithContent {
                    val d = dim.value
                    drawRect(fill)
                    drawRect(light)
                    // Verlierer: Farbe tritt fast ganz hinter die Flaeche zurueck
                    if (d > 0f) drawRect(c.surface.copy(alpha = 0.85f * d))
                    // Druck-Feedback je Haelfte
                    val pt = pressTop.value
                    val pb = pressBottom.value
                    if (pt > 0f) drawRect(Color.White.copy(alpha = 0.16f * pt), size = size.copy(height = half))
                    if (pb > 0f) drawRect(Color.Black.copy(alpha = 0.12f * pb), topLeft = Offset(0f, half), size = size.copy(height = half))
                    // einmaliger Glanz quer ueber die Siegerkachel
                    val s = shine()
                    if (isWinner && s >= 0f) {
                        val x = -size.width + s * size.width * 3f
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                                start = Offset(x, 0f), end = Offset(x + size.width * 0.6f, size.height),
                            ),
                        )
                    }
                    drawContent()
                    val cr = CornerRadius(r - w / 2)
                    if (d < 1f) drawRoundRect(edge, cornerRadius = cr, style = Stroke(w), topLeft = inset, size = inner, alpha = 1f - d)
                    // duenner Rand in Spielerfarbe haelt die Zuordnung
                    if (d > 0f) drawRoundRect(base.copy(alpha = 0.6f * d), cornerRadius = cr, style = Stroke(w), topLeft = inset, size = inner)
                }
            }
            .clearAndSetSemantics {
                contentDescription = desc
                stateDescription = points
                // nach Spielende nur noch lesen
                if (!finished) {
                    role = Role.Button
                    onClick(plusLabel) { score1.value(index, 1); true }
                    customActions = listOf(
                        CustomAccessibilityAction(minusLabel) { score1.value(index, -1); true },
                        CustomAccessibilityAction(enterLabel) { enter1.value(index); true },
                    )
                }
            }
            .pointerInput(index) {
                val slop = viewConfiguration.touchSlop
                val longMs = viewConfiguration.longPressTimeoutMillis
                val stepPx = 24.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    // Spiel aus: abweisen, Rueckgaengig/Neues Spiel bleiben oben bzw. unten
                    if (locked.value) {
                        reject1.value()
                        swallow()
                        return@awaitEachGesture
                    }
                    val half = if (down.position.y < size.height / 2f) 1 else -1
                    pressed.intValue = half
                    when (withTimeoutOrNull(longMs) { tapOrDrag(down, slop) }) {
                        Gesture.TAP -> {
                            pressed.intValue = 0
                            haptics?.press()
                            score1.value(index, half)
                        }
                        Gesture.DRAG -> {
                            pressed.intValue = 0
                            while (true) {
                                val ch = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                ch.consume()
                                val s = Board.steps(ch.position.y - down.position.y, stepPx)
                                if (s != scrub.intValue) {
                                    scrub.intValue = s
                                    haptics?.step()
                                }
                            }
                            val s = scrub.intValue
                            scrub.intValue = 0
                            if (s != 0) score1.value(index, s)
                        }
                        Gesture.CANCEL -> pressed.intValue = 0
                        null -> {
                            // lange gedrueckt: Eingabe, Rest der Geste schlucken
                            pressed.intValue = 0
                            haptics?.press()
                            enter1.value(index)
                            swallow()
                        }
                    }
                }
            },
    ) {
        TileContent(name, score, stats, scrub, burst, isLeader, isWinner, finished, compact, base, dim, fit)
    }
}

@Composable
private fun TileContent(
    name: String,
    score: Int,
    stats: Pair<Int, Int>?,
    scrub: MutableIntState,
    burst: Int?,
    isLeader: Boolean,
    isWinner: Boolean,
    finished: Boolean,
    compact: Boolean,
    base: Color,
    dim: State<Float>,
    fit: (String) -> TextUnit,
) {
    val density = LocalDensity.current
    val pad = tilePad(compact)
    // Inhalt: Tinte auf Farbe, auf der dunklen Verliererkarte helle Spielerfarbe; Wechsel nur im Zeichnen
    val bright = lerp(base, Color.White, 0.3f)
    val ink = remember(bright) { { a: Float -> ColorProducer { lerp(Ink.copy(alpha = a), bright, dim.value) } } }
    val labelStyle = Aapps.type.label.copy(fontSize = Aapps.type.label.fontSize * 1.15f)
    val steps = scrub.intValue
    val shown = Game.clamp(score.toLong() + steps)
    // nach Spielende keine Zug-Anzeige mehr, nur noch die Siegerplakette
    val badge = if (finished) null else if (steps != 0) steps else burst
    val plaque = isWinner && steps == 0
    Box(Modifier.fillMaxSize()) {
        // Kopfzeile: Krone, Name, ggf. Status; rechts Platz fuer das Plus, solange es sichtbar ist
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = pad, end = if (finished) pad else pad + 20.dp, top = pad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isWinner || isLeader) {
                AappsIcon(ScorrIcons.Crown, null, Ink.copy(alpha = if (isWinner) 0.9f else 0.6f), size = 18.dp * min(density.fontScale, 1.6f))
                Spacer(Modifier.width(6.dp))
            }
            BasicText(
                name.uppercase(), Modifier.weight(1f, fill = false),
                style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(0.78f),
            )
        }
        // Tipp-Hinweise am rechten Rand; weg nach Spielende, Minus weicht der Plakette
        val plusAlpha by animateFloatAsState(if (finished) 0f else 1f, Springs.gentle(), label = "plus")
        val minusAlpha by animateFloatAsState(if (finished || badge != null) 0f else 1f, Springs.gentle(), label = "minus")
        val hint = Ink.copy(alpha = 0.55f)
        AappsIcon(
            ScorrIcons.Plus, null, hint,
            Modifier.align(Alignment.TopEnd).padding(top = pad - 2.dp, end = pad - 4.dp).graphicsLayer { alpha = plusAlpha },
            size = 20.dp,
        )
        AappsIcon(
            ScorrIcons.Minus, null, hint,
            Modifier.align(Alignment.BottomEnd).padding(bottom = pad - 2.dp, end = pad - 4.dp).graphicsLayer { alpha = minusAlpha },
            size = 20.dp,
        )

        // Siege und Summe unten links; weicht Zug-Anzeige und Plakette wie das Minus
        if (stats != null) {
            val statsAlpha by animateFloatAsState(if (badge != null || plaque) 0f else 1f, Springs.gentle(), label = "stats")
            val fs = density.fontScale
            // waechst nur bis 150 %, sonst stoesst es an Plakette und Minus
            val statSize = 15.sp * (min(fs, 1.5f) / fs)
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = pad, bottom = pad).graphicsLayer { alpha = statsAlpha },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // auf der dunklen Verliererkarte hell wie die Schrift
                AappsIcon(ScorrIcons.Trophy, null, lerp(Ink.copy(alpha = 0.7f), bright, dim.value), size = 16.dp * min(fs, 1.5f))
                Spacer(Modifier.width(4.dp))
                // kompakt nur die Siege
                val text = if (compact) "${stats.first}" else "${stats.first}  ·  ${Board.format(stats.second)}"
                BasicText(text, style = Aapps.type.number.copy(fontSize = statSize), maxLines = 1, softWrap = false, color = ink(0.78f))
            }
        }

        // Zahl: gleiche Groesse auf allen Kacheln, gemessen statt geschaetzt
        val numberStyle: TextStyle = Aapps.type.display.copy(fontSize = fit(Board.format(shown)), lineHeight = 1.em)
        val numberColor = ink(1f)
        Box(Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            AnimatedContent(
                shown,
                transitionSpec = {
                    val up = targetState > initialState
                    val enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 600f)) { if (up) it / 2 else -it / 2 } + fadeIn(spring(stiffness = 900f))
                    val exit = slideOutVertically(spring(dampingRatio = 1f, stiffness = 700f)) { if (up) -it / 2 else it / 2 } + fadeOut(spring(stiffness = 900f))
                    enter togetherWith exit
                },
                label = "score",
            ) { v ->
                BasicText(Board.format(v), style = numberStyle, maxLines = 1, softWrap = false, color = numberColor)
            }
        }

        // Zug bzw. Ziehen als Plakette unter der Zahl
        // letzter Wert bleibt beim Ausblenden stehen (reiner Cache, kein State)
        val last = remember { intArrayOf(0) }
        if (badge != null) last[0] = badge
        // Sieger: Plakette statt Zug-Anzeige
        AnimatedVisibility(
            plaque,
            Modifier.align(Alignment.BottomCenter).padding(bottom = pad),
            enter = fadeIn(Springs.gentle()) + scaleIn(Springs.bouncy(), 0.6f),
            exit = fadeOut(Springs.gentle()),
        ) {
            BasicText(
                stringResource(Res.string.winner).uppercase(),
                Modifier.background(Ink, CircleShape).padding(horizontal = 16.dp, vertical = 5.dp),
                style = labelStyle.copy(color = Color.White),
                maxLines = 1,
            )
        }
        AnimatedVisibility(
            badge != null && !plaque,
            Modifier.align(Alignment.BottomCenter).padding(bottom = pad),
            enter = fadeIn(Springs.gentle()) + scaleIn(Springs.bouncy(), 0.6f),
            exit = fadeOut(Springs.gentle()) + scaleOut(Springs.gentle(), 0.8f),
        ) {
            val strong = steps != 0
            BasicText(
                Board.formatDelta(last[0]),
                Modifier
                    .background(if (strong) Ink else Ink.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                style = Aapps.type.number.copy(
                    color = if (strong) Color.White else Ink.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                ),
                maxLines = 1,
            )
        }
    }
}
