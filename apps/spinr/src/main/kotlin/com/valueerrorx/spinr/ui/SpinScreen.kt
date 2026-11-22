package com.valueerrorx.spinr.ui

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
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
import com.valueerrorx.aapps.core.ui.Aapps
import com.valueerrorx.aapps.core.ui.AappsIcon
import com.valueerrorx.aapps.core.ui.AappsIconButton
import com.valueerrorx.aapps.core.ui.AappsIcons
import com.valueerrorx.aapps.core.ui.AappsPillButton
import com.valueerrorx.aapps.core.ui.AappsScaffold
import com.valueerrorx.aapps.core.ui.AappsTextButton
import com.valueerrorx.aapps.core.ui.Haptics
import com.valueerrorx.aapps.core.ui.Inter
import com.valueerrorx.aapps.core.ui.Springs
import com.valueerrorx.aapps.core.ui.glow
import com.valueerrorx.aapps.core.ui.pressScale
import com.valueerrorx.spinr.R
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

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
    val pop by animateFloatAsState(
        if (winner != null) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 260f), label = "pop",
    )
    val confettiT = remember { Animatable(-1f) }
    var confettiSeed by remember { mutableIntStateOf(1) }

    if (animate) {
        // Physik-Schleife: laeuft nur, solange sich etwas bewegt
        LaunchedEffect(state) {
            var last = 0L
            var lastFeedback = 0L
            while (true) {
                val v = state.wake
                if (!state.isMoving) {
                    last = 0L
                    snapshotFlow { state.wake }.first { it != v }
                }
                withFrameNanos { now ->
                    val dt = if (last == 0L) 1.0 / 60 else ((now - last) / 1e9).coerceIn(0.0, 0.05)
                    last = now
                    val ticks = state.step(dt)
                    // Haptik und Klick hoechstens ~35 pro Sekunde
                    if (ticks > 0 && now - lastFeedback > 28_000_000L) {
                        lastFeedback = now
                        haptics?.tick()
                        onTick?.invoke()
                    }
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
    val winnerColor = if (winner != null && state.labels.isNotEmpty()) segmentColor(winner, state.labels.size) else c.accent
    val tint = lerp(c.accent, winnerColor, pop.coerceIn(0f, 1f))

    AappsScaffold {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        val hero: @Composable (Modifier) -> Unit = { modifier ->
            WheelHero(state, FaceLook(winner, pop), tint, pop, haptics, modifier) { g ->
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
                    TopRow(wheelName, onOpenWheels, null)
                    ResultBlock(state, winnerColor, pop, Modifier.weight(1f).fillMaxWidth())
                    actions()
                }
                Box(Modifier.weight(0.58f).fillMaxHeight()) {
                    hero(Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { GearButton(onOpenSettings) }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopRow(wheelName, onOpenWheels, onOpenSettings)
                ResultBlock(state, winnerColor, pop, Modifier.weight(0.5f).fillMaxWidth())
                hero(Modifier.weight(1.75f).fillMaxWidth())
                actions()
            }
        }
    }
}

// Rad als Ebenen: fest unten, drehend (plus Nachzieh-Kopien bei Tempo), fest oben, Zeiger, Konfetti
@Composable
private fun WheelHero(
    state: SpinState,
    look: FaceLook,
    tint: Color,
    pop: Float,
    haptics: Haptics?,
    modifier: Modifier,
    overlay: DrawScope.(WheelGeometry) -> Unit,
) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val textStyle = Aapps.type.label.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val labels = state.labels
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
                            if (state.dragBy(d) > 0) haptics?.tick()
                            ch.consume()
                        }
                        tracker.addDataPoint(ch.uptimeMillis, total)
                    }
                    if (!dragging) {
                        if (state.tap()) haptics?.press()
                    } else {
                        state.fling(tracker.calculateVelocity())
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawWheelBase(wheelGeometry(size, density), c, tint, pop)
        }
        // Drehende Ebenen: pro Frame aendern sich nur Drehung und Deckkraft, gezeichnet wird selten
        val layer: @Composable (Float, () -> Float, DrawScope.(WheelGeometry) -> Unit) -> Unit = { lag, alpha, draw ->
            Canvas(
                Modifier.fillMaxSize().graphicsLayer {
                    val g = wheelGeometry(size, density)
                    transformOrigin = TransformOrigin(g.center.x / size.width, g.center.y / size.height)
                    rotationZ = state.angle - lag * trail(state.speed)
                    this.alpha = alpha()
                },
            ) { draw(wheelGeometry(size, density)) }
        }
        val n = labels.size
        layer(0f, { 1f }) { drawWheelColors(it, n, c) }
        // Bewegungsunschaerfe: nachgezogene Farbflaechen, Schrift verblasst bei Tempo
        layer(0.5f, { 0.45f * blur(state.speed) }) { drawWheelColors(it, n, c) }
        layer(1f, { 0.3f * blur(state.speed) }) { drawWheelColors(it, n, c) }
        layer(0f, { 1f - 0.85f * blur(state.speed) }) { drawWheelLabels(it, labels, measurer, textStyle) }
        layer(0f, { 1f }) { drawWheelDetails(it, n, look, c) }
        Canvas(Modifier.fillMaxSize()) {
            drawWheelGlass(wheelGeometry(size, density), c, tint)
        }
        Canvas(Modifier.fillMaxSize()) {
            val g = wheelGeometry(size, density)
            drawPointer(g, c, state.flap, tint, pop)
            overlay(g)
        }
    }
}

// Nachzieh-Winkel und Sichtbarkeit der Unschaerfe aus der Drehgeschwindigkeit
private fun trail(speed: Float): Float = (speed * 0.014f).coerceIn(-20f, 20f)
private fun blur(speed: Float): Float = ((abs(speed) - 240f) / 900f).coerceIn(0f, 1f)

@Composable
private fun GearButton(onClick: () -> Unit) {
    AappsIconButton(AappsIcons.Gear, stringResource(com.valueerrorx.aapps.core.ui.R.string.aapps_settings), onClick)
}

// Rad-Name als Chip (oeffnet die Raeder), Zahnrad
@Composable
private fun TopRow(name: String, onOpenWheels: () -> Unit, onOpenSettings: (() -> Unit)?) {
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
                    .clickable(source, indication = null, role = Role.Button, onClickLabel = choose, onClick = onOpenWheels)
                    .padding(vertical = 6.dp)
                    .background(if (pressed) c.surfaceHigh else Color.Transparent, CircleShape)
                    .border(1.dp, if (pressed) c.fgFaint else c.line, CircleShape)
                    .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    name.ifBlank { "–" },
                    Modifier.weight(1f, fill = false),
                    style = Aapps.type.body.copy(color = c.fg),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                AappsIcon(SpinrIcons.ChevronDown, null, c.fgMuted, size = 18.dp)
            }
        }
        if (onOpenSettings != null) GearButton(onOpenSettings)
    }
}

// Ergebnis gross in der Segmentfarbe, sonst Hinweis bzw. der Eintrag unter dem Zeiger
@Composable
private fun ResultBlock(state: SpinState, winnerColor: Color, pop: Float, modifier: Modifier) {
    val c = Aapps.colors
    val winner = state.winner
    val labels = state.labels
    val current by remember(state) { derivedStateOf { state.current } }
    val mode = when {
        winner != null && winner in labels.indices -> 2
        state.spinning -> 1
        else -> 0
    }
    Box(modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            mode,
            transitionSpec = { (fadeIn(Springs.gentle()) + scaleIn(Springs.bouncy(), 0.85f)) togetherWith fadeOut(Springs.gentle()) },
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
                            Modifier.glow(winnerColor, 120.dp, 0.12f * pop),
                            style = Aapps.type.display.copy(color = winnerColor, textAlign = TextAlign.Center, lineHeight = 1.05.em),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 88.sp, stepSize = 2.sp),
                        )
                    }
                }
                1 -> BasicText(
                    labels.getOrNull(current) ?: "",
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
