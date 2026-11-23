package solutions.xapient.spinr.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Inter
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.spinr.R
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

// Haptik und Klick hoechstens ~35 pro Sekunde, gleich fuer Schwung und Hand
private class TickFeedback {
    private var last = 0L

    fun ticks(count: Int, haptics: Haptics?, sound: (() -> Unit)?) {
        if (count <= 0) return
        val now = System.nanoTime()
        if (now - last < 28_000_000L) return
        last = now
        // Raster wie beim Ziehen: Vibrator waere auf 120 ms entprellt und zu schwer
        haptics?.step()
        sound?.invoke()
    }
}

// Hauptbildschirm: Ergebnis, Rad, eine Hauptaktion
@Composable
fun SpinScreen(
    state: SpinState,
    wheelName: String,
    canRemove: Boolean,
    removedCount: Int,
    haptics: Haptics?,
    onTick: (() -> Unit)?,
    onOpenWheels: () -> Unit,
    onOpenSettings: () -> Unit,
    onRemove: (Int) -> Unit,
    onRestore: () -> Unit,
    animate: Boolean = true,
    confettiAt: Float = -1f,
) {
    val c = Aapps.colors
    val winner = state.winner
    // pop wird erst beim Zeichnen gelesen: die Feder kostet keine Neukomposition
    val pop = animateFloatAsState(
        if (winner != null) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 260f), label = "pop",
    )
    val confettiT = remember { Animatable(-1f) }
    var confettiSeed by remember { mutableIntStateOf(1) }
    val feedback = remember { TickFeedback() }
    val currentHaptics by rememberUpdatedState(haptics)
    val currentTick by rememberUpdatedState(onTick)
    val tick: (Int) -> Unit = { n -> feedback.ticks(n, currentHaptics, currentTick) }

    if (animate) {
        // Physik-Schleife: laeuft nur, solange sich etwas bewegt
        LaunchedEffect(state) {
            var last = 0L
            while (true) {
                val v = state.wake
                if (!state.isMoving) {
                    last = 0L
                    snapshotFlow { state.wake }.first { it != v }
                }
                withFrameNanos { now ->
                    val dt = if (last == 0L) 1.0 / 60 else ((now - last) / 1e9).coerceIn(0.0, 0.05)
                    last = now
                    tick(state.step(dt))
                }
            }
        }
        // nur neue Ergebnisse, nicht beim Zurueckkehren aus den Einstellungen
        var handled by remember { mutableIntStateOf(state.results) }
        LaunchedEffect(state.results) {
            if (state.results == handled) return@LaunchedEffect
            handled = state.results
            haptics?.confirm()
            confettiSeed++
            confettiT.snapTo(0f)
            confettiT.animateTo(Confetti.DURATION, tween((Confetti.DURATION * 1000).toInt(), easing = LinearEasing))
            confettiT.snapTo(-1f)
        }
    }

    val confetti = remember(confettiSeed) { Confetti(confettiSeed) }
    val winnerColor = state.colors.getOrNull(winner ?: -1) ?: c.accent
    val accent = c.accent
    val tint = { lerp(accent, winnerColor, pop.value.coerceIn(0f, 1f)) }
    val locked = state.spinning

    AappsScaffold {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        val hero: @Composable (Modifier) -> Unit = { modifier ->
            WheelHero(state, tint, pop, tick, { currentHaptics?.press() }, modifier) { g ->
                val t = if (confettiAt >= 0f) confettiAt else confettiT.value
                if (t >= 0f) with(confetti) { draw(g.pointerPivot, t) }
            }
        }
        val actions: @Composable () -> Unit = {
            BottomActions(state, canRemove, removedCount, haptics, onRemove, onRestore)
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.42f).fillMaxHeight().padding(start = 8.dp)) {
                    TopRow(wheelName, !locked, onOpenWheels, null)
                    ResultBlock(state, winnerColor, pop, Modifier.weight(1f).fillMaxWidth())
                    actions()
                }
                Box(Modifier.weight(0.58f).fillMaxHeight()) {
                    hero(Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { GearButton(!locked, onOpenSettings) }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopRow(wheelName, !locked, onOpenWheels, onOpenSettings)
                // Ergebnis und Rad als Gruppe im freien Raum, etwas ueber der Mitte; Ergebnis dicht ueber dem Zeiger
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = BiasAlignment(0f, -0.25f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ResultBlock(state, winnerColor, pop, Modifier.fillMaxWidth().height(140.dp), Alignment.BottomCenter)
                        hero(Modifier.fillMaxWidth().fitWheelHeight())
                    }
                }
                actions()
            }
        }
    }
}

// So hoch, wie das Rad bei voller Breite braucht, hoechstens der verfuegbare Platz
private fun Modifier.fitWheelHeight(): Modifier = layout { measurable, constraints ->
    val w = constraints.maxWidth
    val h = min(wheelHeight(w.toFloat(), density).roundToInt(), constraints.maxHeight)
    val p = measurable.measure(constraints.copy(minWidth = w, minHeight = h, maxHeight = h))
    layout(p.width, p.height) { p.place(0, 0) }
}

// Rad als Ebenen: fest unten, drehend (Farben, Unschaerfe, Schrift, Stifte), fest oben, Zeiger, Konfetti
@Composable
private fun WheelHero(
    state: SpinState,
    tint: () -> Color,
    pop: State<Float>,
    tick: (Int) -> Unit,
    onTapped: () -> Unit,
    modifier: Modifier,
    overlay: DrawScope.(WheelGeometry) -> Unit,
) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val textStyle = Aapps.type.label.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val labels = state.labels
    val colors = state.colors
    val winner = state.winner
    val slop = LocalViewConfiguration.current.touchSlop
    val description = if (labels.size >= 2) pluralStringResource(R.plurals.wheel_desc, labels.size, labels.size) else stringResource(R.string.hint_too_few)
    val spinLabel = stringResource(R.string.spin)

    Box(
        modifier
            .semantics {
                contentDescription = description
                role = Role.Button
                onClick(spinLabel) { state.tap() }
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val g = wheelGeometry(Size(size.width.toFloat(), size.height.toFloat()), density)
                    fun angleOf(p: Offset): Float = Math.toDegrees(atan2((p.y - g.center.y).toDouble(), (p.x - g.center.x).toDouble())).toFloat()
                    if ((down.position - g.center).getDistance() > g.radius * 1.05f || state.spinning) return@awaitEachGesture
                    val tracker = VelocityTracker1D(isDataDifferential = false)
                    var last = angleOf(down.position)
                    var total = 0f
                    var travel = 0f
                    var dragging = false
                    tracker.addDataPoint(down.uptimeMillis, 0f)
                    while (true) {
                        val event = awaitPointerEvent()
                        val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        travel += (ch.position - ch.previousPosition).getDistance()
                        // nahe der Mitte ist der Winkel unruhig
                        val nearCenter = hypot(ch.position.x - g.center.x, ch.position.y - g.center.y) < g.radius * 0.15f
                        val a = angleOf(ch.position)
                        var d = a - last
                        if (d > 180f) d -= 360f
                        if (d < -180f) d += 360f
                        last = a
                        if (nearCenter) d = 0f
                        if (!dragging && travel > slop) dragging = true
                        if (dragging) {
                            total += d
                            tick(state.dragBy(d))
                            ch.consume()
                        }
                        tracker.addDataPoint(ch.uptimeMillis, total)
                    }
                    if (!dragging) {
                        if (state.tap()) onTapped()
                    } else {
                        state.fling(tracker.calculateVelocity())
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawWheelBase(wheelGeometry(size, density), c, tint(), pop.value)
        }
        // Drehende Ebenen: pro Frame aendern sich nur Drehung und Deckkraft, gezeichnet wird selten
        val layer: @Composable (() -> Float, DrawScope.(WheelGeometry) -> Unit) -> Unit = { alpha, draw ->
            Canvas(
                Modifier.fillMaxSize().graphicsLayer {
                    val g = wheelGeometry(size, density)
                    transformOrigin = TransformOrigin(g.center.x / size.width, g.center.y / size.height)
                    rotationZ = state.angle
                    this.alpha = alpha()
                },
            ) { draw(wheelGeometry(size, density)) }
        }
        layer({ 1f }) { drawWheelFace(it, colors, winner, pop.value, c) }
        // Bewegungsunschaerfe: bei Tempo fliessen die Farben ineinander, die Schrift verblasst
        layer({ blur(state.speed) }) { drawWheelBlur(it, colors) }
        layer({ 1f - 0.85f * blur(state.speed) }) { drawWheelLabels(it, labels, measurer, textStyle, winner, pop.value) }
        layer({ 1f }) { drawWheelDetails(it, colors.size, winner, pop.value) }
        Canvas(Modifier.fillMaxSize()) {
            drawWheelGlass(wheelGeometry(size, density), c, tint())
        }
        Canvas(Modifier.fillMaxSize()) {
            val g = wheelGeometry(size, density)
            drawPointer(g, c, state.flap, tint(), pop.value)
            overlay(g)
        }
    }
}

// Sichtbarkeit der Unschaerfe aus der Drehgeschwindigkeit
private fun blur(speed: Float): Float = ((abs(speed) - 260f) / 700f).coerceIn(0f, 1f)

@Composable
private fun GearButton(enabled: Boolean, onClick: () -> Unit) {
    AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onClick, enabled = enabled)
}

// Rad-Name als Chip (oeffnet die Raeder), Zahnrad; beides gesperrt, solange das Rad dreht
@Composable
private fun TopRow(name: String, enabled: Boolean, onOpenWheels: () -> Unit, onOpenSettings: (() -> Unit)?) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val choose = stringResource(R.string.choose_wheel)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Row(
                Modifier
                    .pressScale(pressed, 0.96f)
                    .heightIn(min = 48.dp)
                    .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClickLabel = choose, onClick = onOpenWheels)
                    .padding(vertical = 6.dp)
                    .background(if (pressed) c.surfaceHigh else Color.Transparent, CircleShape)
                    .border(1.dp, if (pressed) c.fgFaint else c.line, CircleShape)
                    .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    name.ifBlank { "–" },
                    Modifier.weight(1f, fill = false),
                    style = Aapps.type.body.copy(color = if (enabled) c.fg else c.fgMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                AappsIcon(SpinrIcons.ChevronDown, null, if (enabled) c.fgMuted else c.fgFaint, size = 18.dp)
            }
        }
        if (onOpenSettings != null) GearButton(enabled, onOpenSettings)
    }
}

// Ergebnis gross in der Segmentfarbe, sonst Hinweis bzw. der Eintrag unter dem Zeiger
@Composable
private fun ResultBlock(
    state: SpinState,
    winnerColor: Color,
    pop: State<Float>,
    modifier: Modifier,
    align: Alignment = Alignment.Center,
) {
    val c = Aapps.colors
    val winner = state.winner
    val labels = state.labels
    val current by remember(state) { derivedStateOf { state.current } }
    val mode = when {
        winner != null && winner in labels.indices -> 2
        state.spinning -> 1
        else -> 0
    }
    Box(modifier.padding(horizontal = 24.dp), contentAlignment = align) {
        AnimatedContent(
            mode,
            transitionSpec = { (fadeIn(Springs.gentle()) + scaleIn(Springs.bouncy(), 0.85f)) togetherWith fadeOut(Springs.gentle()) },
            contentAlignment = align,
            label = "result",
        ) { m ->
            when (m) {
                2 -> {
                    val text = labels.getOrNull(state.winner ?: -1) ?: ""
                    val desc = stringResource(R.string.result_desc, text)
                    Column(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                            contentDescription = desc
                            liveRegion = LiveRegionMode.Polite
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BasicText(
                            stringResource(R.string.picked).uppercase(),
                            style = Aapps.type.label.copy(color = c.fgMuted),
                        )
                        Spacer(Modifier.height(6.dp))
                        BasicText(
                            text,
                            Modifier.glow(winnerColor, 120.dp, 0.12f * pop.value),
                            style = Aapps.type.display.copy(color = winnerColor, textAlign = TextAlign.Center, lineHeight = 1.05.em),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 88.sp, stepSize = 2.sp),
                        )
                    }
                }
                // Eintrag unter dem Zeiger erst, wenn er lesbar langsam vorbeizieht
                1 -> BasicText(
                    labels.getOrNull(current) ?: "",
                    Modifier.graphicsLayer { alpha = ((600f - abs(state.speed)) / 200f).coerceIn(0f, 1f) },
                    style = Aapps.type.headline.copy(color = c.fgMuted, textAlign = TextAlign.Center),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                else -> BasicText(
                    stringResource(if (labels.size >= 2) R.string.hint_idle else R.string.hint_too_few),
                    style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center, fontSize = 17.sp),
                )
            }
        }
    }
}

// Hauptaktion Drehen; Nebenaktionen fuer die Runde
@Composable
private fun BottomActions(
    state: SpinState,
    canRemove: Boolean,
    removedCount: Int,
    haptics: Haptics?,
    onRemove: (Int) -> Unit,
    onRestore: () -> Unit,
) {
    val winner = state.winner
    Column(
        Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val showRemove = winner != null && canRemove
        if (showRemove || removedCount > 0) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (showRemove) AappsTextButton(stringResource(R.string.remove), { haptics?.press(); onRemove(winner!!) })
                if (removedCount > 0) AappsTextButton(stringResource(R.string.restore, removedCount), { haptics?.press(); onRestore() })
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }
        AappsPillButton(
            text = stringResource(if (winner != null) R.string.spin_again else R.string.spin),
            onClick = { if (state.tap()) haptics?.press() },
            modifier = Modifier.widthIn(min = 168.dp),
            icon = AappsIcons.Rotate,
            active = state.canSpin,
            enabled = state.canSpin,
        )
    }
}
