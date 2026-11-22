package com.valueerrorx.scorr.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.valueerrorx.aapps.core.ui.Aapps
import com.valueerrorx.aapps.core.ui.AappsIcon
import com.valueerrorx.aapps.core.ui.AappsIconButton
import com.valueerrorx.aapps.core.ui.AappsIcons
import com.valueerrorx.aapps.core.ui.AappsPillButton
import com.valueerrorx.aapps.core.ui.AappsScaffold
import com.valueerrorx.aapps.core.ui.Haptics
import com.valueerrorx.aapps.core.ui.Springs
import com.valueerrorx.aapps.core.ui.pressScale
import com.valueerrorx.scorr.R
import com.valueerrorx.scorr.domain.Board
import com.valueerrorx.scorr.domain.Game
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

// Anzeigename: leer = „Spieler n"
@Composable
fun displayName(game: Game, i: Int): String =
    game.players.getOrNull(i)?.name?.ifBlank { null } ?: stringResource(R.string.player_n, i + 1)

// Tafel: Kacheln als Hero, oben Runde/Rueckgaengig/Zahnrad, unten die eine Hauptaktion
@Composable
fun ScoreScreen(
    game: Game,
    rounds: Boolean,
    haptics: Haptics?,
    onScore: (player: Int, delta: Int) -> Unit,
    onEnter: (player: Int) -> Unit,
    onUndo: () -> Unit,
    onNextRound: () -> Unit,
    onNewGame: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    canUndo: Boolean = game.canUndo,
    clock: () -> Long = System::currentTimeMillis,
    animate: Boolean = true,
    scrubPreview: Pair<Int, Int>? = null,
    shineAt: Float = -1f,
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

    AappsScaffold {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        val board: @Composable (Modifier) -> Unit = { m ->
            TileGrid(
                game, landscape, now, haptics, onScore, onEnter, scrubPreview,
                { if (shineAt >= 0f) shineAt else shine.value }, m,
            )
        }
        val action: @Composable () -> Unit = {
            when {
                finished -> AappsPillButton(
                    stringResource(R.string.new_game), { haptics?.press(); onNewGame() },
                    Modifier.widthIn(min = 176.dp), icon = AappsIcons.Rotate, active = true,
                )
                rounds -> AappsPillButton(
                    stringResource(R.string.next_round), { haptics?.press(); onNextRound() },
                    Modifier.widthIn(min = 176.dp), icon = ScorrIcons.Plus,
                )
            }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(0.3f).fillMaxHeight().padding(start = 12.dp, top = 8.dp, bottom = 16.dp),
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
                board(Modifier.weight(0.7f).fillMaxHeight().padding(top = 12.dp, end = 12.dp, bottom = 12.dp))
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
        // Ansage fuer TalkBack, sobald jemand gewinnt
        if (finished) {
            val names = game.winners.sorted().map { displayName(game, it) }.joinToString(", ")
            val text = stringResource(R.string.announce_winner, names)
            Box(Modifier.size(1.dp).semantics { contentDescription = text; liveRegion = LiveRegionMode.Polite })
        }
        overlay()
    }
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    AappsIconButton(AappsIcons.Gear, stringResource(com.valueerrorx.aapps.core.ui.R.string.aapps_settings), onClick)
}

// Rueckgaengig bleibt an seinem Platz, ist ohne Verlauf aber unsichtbar
@Composable
private fun UndoButton(enabled: Boolean, haptics: Haptics?, onUndo: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0f, Springs.gentle(), label = "undo")
    Box(Modifier.graphicsLayer { this.alpha = alpha }) {
        if (enabled) {
            AappsIconButton(ScorrIcons.Undo, stringResource(R.string.undo), { haptics?.press(); onUndo() })
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

// Runde (bzw. Verlauf) als Chip, Ziel daneben; oeffnet den Verlauf
@Composable
private fun RoundChip(game: Game, rounds: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val title = if (rounds) stringResource(R.string.round_n, game.round) else stringResource(R.string.history)
    val target = game.target?.let { stringResource(R.string.target_short, Board.format(it)) }
    Row(
        Modifier
            .pressScale(pressed, 0.96f)
            .heightIn(min = 48.dp)
            .clickable(source, indication = null, role = Role.Button, onClickLabel = stringResource(R.string.history), onClick = onClick)
            .padding(vertical = 6.dp)
            .background(if (pressed) c.surfaceHigh else Color.Transparent, CircleShape)
            .border(1.dp, if (pressed) c.fgFaint else c.line, CircleShape)
            .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AappsIcon(ScorrIcons.Table, null, c.accent, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        BasicText(title.uppercase(), style = Aapps.type.label.copy(color = c.fg, fontSize = Aapps.type.label.fontSize * 1.1f), maxLines = 1)
        if (target != null) {
            BasicText(
                "  ·  ${target.uppercase()}",
                style = Aapps.type.label.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            stringResource(if (w.size == 1) R.string.winner else R.string.winners).uppercase(),
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

@Composable
private fun TileGrid(
    game: Game,
    landscape: Boolean,
    now: Long,
    haptics: Haptics?,
    onScore: (Int, Int) -> Unit,
    onEnter: (Int) -> Unit,
    scrubPreview: Pair<Int, Int>?,
    shine: () -> Float,
    modifier: Modifier,
) {
    val n = game.players.size
    val grid = Board.grid(n, landscape)
    val gap = if (n > 4) 8.dp else 10.dp
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until grid.rows) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (k in 0 until min(grid.cols, n - r * grid.cols)) {
                    val i = r * grid.cols + k
                    // Spieler-Index als Schluessel: Zustand bleibt bei Aenderungen an seiner Kachel
                    androidx.compose.runtime.key(i) {
                        Tile(
                            game, i, now, haptics, onScore, onEnter,
                            scrubPreview?.takeIf { it.first == i }?.second ?: 0,
                            shine, compact = n > 4,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
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

@Composable
private fun Tile(
    game: Game,
    index: Int,
    now: Long,
    haptics: Haptics?,
    onScore: (Int, Int) -> Unit,
    onEnter: (Int) -> Unit,
    initialScrub: Int,
    shine: () -> Float,
    compact: Boolean,
    modifier: Modifier,
) {
    val player = game.players[index]
    val base = playerColor(player.color)
    val score = game.scores[index]
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
    val dim by animateFloatAsState(if (finished && !isWinner) 1f else 0f, Springs.gentle(), label = "dim")
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

    val c = Aapps.colors
    val radius = if (compact) 22.dp else 28.dp
    val shape = RoundedCornerShape(radius)
    val plusLabel = stringResource(R.string.action_plus)
    val minusLabel = stringResource(R.string.action_minus)
    val enterLabel = stringResource(R.string.action_enter)
    val desc = stringResource(if (isWinner) R.string.tile_desc_winner else R.string.tile_desc, name, Board.format(score))

    Box(
        modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
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
                onDrawWithContent {
                    drawRect(fill)
                    drawRect(light)
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
                    drawRoundRect(edge, cornerRadius = CornerRadius(r - w / 2), style = Stroke(w), topLeft = Offset(w / 2, w / 2), size = size.copy(size.width - w, size.height - w))
                    // Verlierer treten zurueck
                    if (dim > 0f) drawRect(c.bg.copy(alpha = 0.68f * dim))
                }
            }
            .clearAndSetSemantics {
                contentDescription = desc
                role = Role.Button
                onClick(plusLabel) { score1.value(index, 1); true }
                customActions = listOf(
                    CustomAccessibilityAction(minusLabel) { score1.value(index, -1); true },
                    CustomAccessibilityAction(enterLabel) { enter1.value(index); true },
                )
            }
            .pointerInput(index) {
                val slop = viewConfiguration.touchSlop
                val longMs = viewConfiguration.longPressTimeoutMillis
                val stepPx = 24.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val half = if (down.position.y < size.height / 2f) 1 else -1
                    pressed.intValue = half
                    when (withTimeoutOrNull(longMs) { tapOrDrag(down, slop) }) {
                        Gesture.TAP -> {
                            pressed.intValue = 0
                            haptics?.tick()
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
                                    haptics?.tick()
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
                            do {
                                val e = awaitPointerEvent()
                                e.changes.forEach { it.consume() }
                            } while (e.changes.any { it.pressed })
                        }
                    }
                }
            },
    ) {
        TileContent(name, score, scrub, burst, isLeader, isWinner, compact)
    }
}

@Composable
private fun TileContent(
    name: String,
    score: Int,
    scrub: MutableIntState,
    burst: Int?,
    isLeader: Boolean,
    isWinner: Boolean,
    compact: Boolean,
) {
    val density = LocalDensity.current
    val pad = if (compact) 12.dp else 18.dp
    val labelStyle = Aapps.type.label.copy(color = Ink.copy(alpha = 0.78f), fontSize = Aapps.type.label.fontSize * 1.15f)
    val steps = scrub.intValue
    val shown = Game.clamp(score.toLong() + steps)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Kopfzeile: Krone, Name, ggf. Status
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = pad, end = pad + 8.dp, top = pad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isWinner || isLeader) {
                AappsIcon(ScorrIcons.Crown, null, Ink.copy(alpha = if (isWinner) 0.9f else 0.6f), size = 18.dp * min(density.fontScale, 1.6f))
                Spacer(Modifier.width(6.dp))
            }
            BasicText(name.uppercase(), Modifier.weight(1f, fill = false), style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Tipp-Hinweise am rechten Rand
        val hint = Ink.copy(alpha = 0.32f)
        AappsIcon(ScorrIcons.Plus, null, hint, Modifier.align(Alignment.TopEnd).padding(top = pad - 2.dp, end = pad - 4.dp), size = 20.dp)
        AappsIcon(ScorrIcons.Minus, null, hint, Modifier.align(Alignment.BottomEnd).padding(bottom = pad - 2.dp, end = pad - 4.dp), size = 20.dp)

        // Zahl: so gross wie die Kachel erlaubt, unabhaengig von der Schriftgroesse
        val band = 22.dp + 18.dp * density.fontScale.coerceAtMost(2f)
        val text = Board.format(shown)
        val fontPx = with(density) {
            val h = (maxHeight - band * 2).toPx().coerceAtLeast(24.dp.toPx())
            val w = (maxWidth - pad * 2).toPx()
            min(min(h * 0.92f, w / (text.length * 0.62f + 0.1f)), 320.dp.toPx())
        }
        val numberStyle = Aapps.type.display.copy(
            color = Ink,
            fontSize = with(density) { fontPx.toSp() },
            lineHeight = 1.em,
        )
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
                BasicText(Board.format(v), style = numberStyle, maxLines = 1, softWrap = false)
            }
        }

        // Zug bzw. Ziehen als Plakette unter der Zahl
        val badge = if (steps != 0) steps else burst
        // letzter Wert bleibt beim Ausblenden stehen (reiner Cache, kein State)
        val last = remember { intArrayOf(0) }
        if (badge != null) last[0] = badge
        // Sieger: Plakette statt Zug-Anzeige
        AnimatedVisibility(
            isWinner && steps == 0,
            Modifier.align(Alignment.BottomCenter).padding(bottom = pad),
            enter = fadeIn(Springs.gentle()) + scaleIn(Springs.bouncy(), 0.6f),
            exit = fadeOut(Springs.gentle()),
        ) {
            BasicText(
                stringResource(R.string.winner).uppercase(),
                Modifier.background(Ink, CircleShape).padding(horizontal = 16.dp, vertical = 5.dp),
                style = labelStyle.copy(color = Color.White),
                maxLines = 1,
            )
        }
        AnimatedVisibility(
            badge != null && !(isWinner && steps == 0),
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
