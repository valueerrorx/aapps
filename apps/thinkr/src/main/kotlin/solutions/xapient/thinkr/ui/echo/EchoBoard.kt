package solutions.xapient.thinkr.ui.echo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlinx.coroutines.launch
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.echo.EchoAnswer
import solutions.xapient.thinkr.domain.echo.EchoState
import solutions.xapient.thinkr.domain.echo.Glyph
import solutions.xapient.thinkr.ui.Motion
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.AnswerButton
import solutions.xapient.thinkr.ui.drawGlass
import solutions.xapient.thinkr.ui.drawGlyph
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// Karte, die gerade abtritt: Form, Auswertung (null = Merk-Karte), Startversatz vom Wisch
private data class Outgoing(val glyph: Glyph, val answer: EchoAnswer?, val fromX: Float)

@Composable
fun glyphName(g: Glyph): String = stringResource(
    when (g) {
        Glyph.CIRCLE -> R.string.glyph_circle
        Glyph.TRIANGLE -> R.string.glyph_triangle
        Glyph.DIAMOND -> R.string.glyph_diamond
        Glyph.STAR -> R.string.glyph_star
        Glyph.WAVE -> R.string.glyph_wave
        Glyph.RING -> R.string.glyph_ring
    },
)

// Echo-Brett: Karte in der Mitte, Nein links, Ja rechts; Wisch links/rechts antwortet ebenso
@Composable
fun EchoBoard(s: EchoState, live: Boolean, count: Int, onAnswer: (Boolean) -> Unit) {
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val canAnswer = live && !s.isMemo
    val scope = rememberCoroutineScope()

    var drag by remember { mutableFloatStateOf(0f) }
    var flyFrom by remember { mutableFloatStateOf(0f) }
    // sichtbare Karte; -1 = Einzaehlen
    val key = if (count > 0) -1 else s.index
    var shownKey by remember { mutableIntStateOf(key) }
    val enter = remember { Animatable(1f) }
    val out = remember { Animatable(1f) }
    var outgoing by remember { mutableStateOf<Outgoing?>(null) }
    val state by rememberUpdatedState(s)
    LaunchedEffect(key) {
        if (key == shownKey) return@LaunchedEffect
        val prev = shownKey
        shownKey = key
        if (prev >= 0 && key >= 0) {
            outgoing = Outgoing(state.deck.cards[prev], state.last?.takeIf { it.index == prev }, flyFrom)
            launch {
                out.snapTo(0f)
                out.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
                outgoing = null
            }
        }
        flyFrom = 0f
        drag = 0f
        enter.snapTo(0f)
        enter.animateTo(1f, Springs.bouncy())
    }

    // kurzes Aufleuchten der gedrueckten Taste: Akzent richtig, Bernstein falsch
    val flash = remember { Animatable(0f) }
    LaunchedEffect(s.last) {
        val a = s.last ?: return@LaunchedEffect
        if (a.said == null) return@LaunchedEffect
        flash.snapTo(1f)
        flash.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow))
    }
    val last = s.last
    val flashYes = if (last?.said == true) flash.value else 0f
    val flashNo = if (last?.said == false) flash.value else 0f
    val flashColor = if (last?.correct == true) Aapps.colors.accent else Amber

    val stage: @Composable (Modifier) -> Unit = { m ->
        BoxWithConstraints(m, contentAlignment = Alignment.Center) {
            val side = min(min(maxWidth * 0.66f, maxHeight * 0.56f), 300.dp)
            val threshold = with(LocalDensity.current) { (side * 0.28f).toPx() }
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(canAnswer, threshold) {
                        if (!canAnswer) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val d = drag
                                if (abs(d) > threshold) {
                                    flyFrom = d
                                    onAnswer(d > 0)
                                } else {
                                    scope.launch { animate(d, 0f, animationSpec = Springs.snappy()) { v, _ -> drag = v } }
                                }
                            },
                            onDragCancel = {
                                scope.launch { animate(drag, 0f, animationSpec = Springs.snappy()) { v, _ -> drag = v } }
                            },
                        ) { change, dx ->
                            change.consume()
                            drag += dx
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                CardStage(s, side, key, count, live, enter.value, drag, outgoing, out.value)
            }
        }
    }

    if (landscape) {
        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnswerButton(false, canAnswer, flashNo, flashColor, { onAnswer(false) }, Modifier.width(148.dp).fillMaxHeight(0.62f))
            stage(Modifier.weight(1f).fillMaxHeight())
            AnswerButton(true, canAnswer, flashYes, flashColor, { onAnswer(true) }, Modifier.width(148.dp).fillMaxHeight(0.62f))
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            stage(Modifier.weight(1f).fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnswerButton(false, canAnswer, flashNo, flashColor, { onAnswer(false) }, Modifier.weight(1f).heightIn(min = 88.dp))
                AnswerButton(true, canAnswer, flashYes, flashColor, { onAnswer(true) }, Modifier.weight(1f).heightIn(min = 88.dp))
            }
        }
    }
}

@Composable
private fun CardStage(
    s: EchoState,
    side: Dp,
    key: Int,
    count: Int,
    live: Boolean,
    enter: Float,
    drag: Float,
    outgoing: Outgoing?,
    out: Float,
) {
    val c = Aapps.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TopLine(s, count, live, Modifier.widthIn(max = side * 1.5f).heightIn(min = 56.dp))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(side), contentAlignment = Alignment.Center) {
            // Lichthof hinter dem Stapel
            Canvas(Modifier.size(side)) { drawGlow(c.accent, center, size.width * 0.85f, 0.16f) }
            if (outgoing != null) OutgoingCard(outgoing, side, out)
            if (key < 0) {
                EmptyCard(side, count)
            } else {
                val glyph = s.deck.cards[key]
                val name = glyphName(glyph)
                val sidePx = with(LocalDensity.current) { side.toPx() }
                Canvas(
                    Modifier
                        .size(side)
                        .semantics { contentDescription = name }
                        .graphicsLayer {
                            val t = enter
                            translationY = (1f - t) * sidePx * 0.22f
                            translationX = drag
                            rotationZ = drag / sidePx * 14f
                            val sc = 0.86f + 0.14f * t
                            scaleX = sc; scaleY = sc
                            alpha = t.coerceIn(0f, 1f)
                        },
                ) {
                    // Wisch-Richtung leuchtet leicht an der Kante
                    val lean = (drag / (sidePx * 0.28f)).coerceIn(-1f, 1f)
                    val edge = c.accent.copy(alpha = 0.42f + 0.4f * abs(lean))
                    drawGlass(Offset.Zero, size, size.width * 0.16f, edge)
                    drawGlyph(glyph, center, size.width * 0.5f)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        DeadlineLine(s, live && key >= 0, side)
    }
}

// Ueber der Karte: Frage beim Einzaehlen, „Merken" bei Merk-Karten, Plakette „2" bei 2-back
@Composable
private fun TopLine(s: EchoState, count: Int, live: Boolean, modifier: Modifier) {
    val c = Aapps.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            count > 0 -> BasicText(
                stringResource(if (s.nBack == 2) R.string.echo_prompt_two else R.string.echo_prompt_one),
                style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center),
                maxLines = 2,
            )
            live && s.isMemo -> BasicText(
                stringResource(R.string.memorize).uppercase(),
                style = Aapps.type.label.copy(color = c.accent, fontSize = Aapps.type.label.fontSize * 1.2f),
            )
            s.nBack == 2 -> BackBadge()
        }
    }
}

// Grosse „2" im Ring: verglichen wird mit der Karte vor zwei
@Composable
private fun BackBadge() {
    val c = Aapps.colors
    val desc = stringResource(R.string.two_back)
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = desc }
            .drawBehind {
                drawGlow(c.accent, center, size.width * 0.9f, 0.25f)
                drawCircle(c.surfaceHigh, size.width / 2)
                drawCircle(c.accent, size.width / 2 - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText("2", style = Aapps.type.headline.copy(color = c.accent, fontSize = Aapps.type.headline.fontSize * 0.75f))
    }
}

@Composable
private fun EmptyCard(side: Dp, count: Int) {
    val c = Aapps.colors
    val pop = remember { Animatable(1f) }
    LaunchedEffect(count) {
        pop.snapTo(0.6f)
        pop.animateTo(1f, Springs.bouncy())
    }
    Box(Modifier.size(side), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(side)) { drawGlass(Offset.Zero, size, size.width * 0.16f, c.line) }
        if (count > 0) {
            BasicText(
                count.toString(),
                Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                style = Aapps.type.display.copy(color = c.accent, fontSize = Aapps.type.display.fontSize * 0.8f),
            )
        }
    }
}

@Composable
private fun OutgoingCard(o: Outgoing, side: Dp, p: Float) {
    val c = Aapps.colors
    val sidePx = with(LocalDensity.current) { side.toPx() }
    val a = o.answer
    val bad = a != null && !a.correct
    Canvas(
        Modifier
            .size(side)
            .graphicsLayer {
                when {
                    // richtig: fliegt mit Schwung in Antwortrichtung und dreht sich
                    a != null && a.correct -> {
                        val dir = if (a.said == true) 1f else -1f
                        translationX = o.fromX + dir * p * sidePx * 1.5f
                        translationY = -p * sidePx * 0.08f
                        rotationZ = dir * p * 28f
                        alpha = 1f - p
                    }
                    // falsch oder Frist um: wackelt und sinkt
                    bad -> {
                        translationX = o.fromX * (1f - p) + sin(p * 6f * PI.toFloat()) * (1f - p) * sidePx * Motion.still(0.06f)
                        translationY = p * sidePx * 0.32f
                        alpha = 1f - p
                    }
                    // Merk-Karte: tritt nach hinten zurueck
                    else -> {
                        val sc = 1f - 0.18f * p
                        scaleX = sc; scaleY = sc
                        translationY = -p * sidePx * 0.1f
                        alpha = (1f - p) * 0.9f
                    }
                }
            },
    ) {
        drawGlass(Offset.Zero, size, size.width * 0.16f, if (bad) Amber else c.accent.copy(alpha = 0.42f))
        drawGlyph(o.glyph, center, size.width * 0.5f, glow = if (bad) Amber else c.accent)
    }
}

// Frist der aktuellen Karte als Linie, die schrumpft; knapp wird sie Bernstein
@Composable
private fun DeadlineLine(s: EchoState, visible: Boolean, side: Dp) {
    val c = Aapps.colors
    val show = visible && !s.isMemo && !s.round.isOver
    val f = if (show) ((s.dueAt - s.round.now).toFloat() / s.deadlineMs).coerceIn(0f, 1f) else 0f
    val alpha by animateFloatAsState(if (show) 1f else 0f, Springs.gentle(), label = "deadline")
    Canvas(Modifier.width(side * 0.8f).height(6.dp).graphicsLayer { this.alpha = alpha }) {
        val y = size.height / 2
        val w = size.width * f
        val x0 = (size.width - w) / 2
        drawLine(c.line, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), StrokeCap.Round)
        if (w > 0f) drawLine(if (f < 0.3f) Amber else c.accent, Offset(x0, y), Offset(x0 + w, y), 3.dp.toPx(), StrokeCap.Round)
    }
}
