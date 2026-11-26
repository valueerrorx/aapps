package solutions.xapient.levlr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Springs
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_settings
import solutions.xapient.levlr.res.*
import solutions.xapient.levlr.decimalSeparator
import solutions.xapient.levlr.domain.AngleUnit
import solutions.xapient.levlr.domain.LevelMode
import solutions.xapient.levlr.domain.LevelReading
import solutions.xapient.levlr.domain.TiltMath
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

// Hauptbildschirm: Zahl, Libelle, eine Hauptaktion
@Composable
fun LevelScreen(
    state: LevelState,
    unit: AngleUnit,
    haptics: Haptics?,
    onOpenSettings: () -> Unit,
    onCalibrationDone: (CalibrationResult) -> Unit,
    sensorMissing: Boolean = false,
    animate: Boolean = true,
    shineOverride: Float = -1f,
) {
    val c = Aapps.colors
    val reading = state.reading
    val levelTarget = if (reading.isLevel && !state.held) 1f else 0f
    val level by animateFloatAsState(levelTarget, Springs.gentle(), label = "level")
    val edgeAlpha by animateFloatAsState(if (reading.mode == LevelMode.EDGE) 1f else 0f, Springs.gentle(), label = "mode")
    val shine = remember { Animatable(-1f) }
    var hero by remember { mutableStateOf(Rect.Zero) }
    val measurer = rememberTextMeasurer()
    // Skalenbeschriftung ist Teil der Grafik, waechst nicht mit der Schriftgroesse
    val tickStyle = Aapps.type.number.copy(fontSize = (11f / LocalDensity.current.fontScale).sp, color = c.fgMuted)

    // Physik-Schleife: laeuft nur, solange sich etwas bewegt
    if (animate) {
        LaunchedEffect(state) {
            var last = 0L
            while (true) {
                if (state.physics.isSettled) {
                    last = 0L
                    val v = state.targetVersion
                    snapshotFlow { state.targetVersion }.first { it != v }
                }
                withFrameNanos { now ->
                    val dt = if (last == 0L) 1f / 60f else (now - last) / 1e9f
                    last = now
                    state.physics.step(dt)
                    state.frame++
                }
            }
        }
        // nur neue Einrast-Ereignisse, nicht beim Zurueckkehren aus den Einstellungen
        var handled by remember { mutableIntStateOf(state.snapCount) }
        LaunchedEffect(state.snapCount) {
            if (state.snapCount == handled) return@LaunchedEffect
            handled = state.snapCount
            haptics?.confirm()
            shine.snapTo(0f)
            shine.animateTo(1f, spring(dampingRatio = 1f, stiffness = 60f))
            shine.snapTo(-1f)
        }
    }

    AappsScaffold(
        background = {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = edgeAlpha }) {
                state.frame
                if (hero != Rect.Zero && edgeAlpha > 0f) {
                    drawEdgeLevel(
                        state.physics, hero, reading.quadrant, level, unit,
                        if (shineOverride >= 0f) shineOverride else shine.value, c, measurer, tickStyle,
                    )
                }
            }
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - edgeAlpha }) {
                state.frame
                if (hero != Rect.Zero && edgeAlpha < 1f) {
                    drawFlatLevel(
                        state.physics, hero, level, unit,
                        if (shineOverride >= 0f) shineOverride else shine.value, c, measurer, tickStyle,
                    )
                }
            }
        },
    ) {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        val heroModifier = Modifier.onGloballyPositioned { hero = it.boundsInRoot() }
        val session = state.calibration
        state.calibrationTick // Overlay bei Kalibrier-Fortschritt neu zeichnen

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.4f).fillMaxHeight().padding(start = 8.dp)) {
                    TopRow(reading.mode, null)
                    ReadingBlock(state, unit, level, Modifier.weight(1f).fillMaxWidth())
                    BottomAction(state, session, onCalibrationDone, haptics)
                }
                Box(Modifier.weight(0.6f).fillMaxHeight()) {
                    Box(heroModifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { GearButton(onOpenSettings) }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopRow(reading.mode, onOpenSettings)
                ReadingBlock(state, unit, level, Modifier.weight(0.8f).fillMaxWidth())
                Box(heroModifier.weight(1.6f).fillMaxWidth())
                BottomAction(state, session, onCalibrationDone, haptics)
            }
        }
        if (sensorMissing) {
            Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                BasicText(
                    stringResource(Res.string.no_sensor),
                    Modifier.padding(32.dp),
                    style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center),
                )
            }
        }
    }
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    AappsIconButton(AappsIcons.Gear, stringResource(CoreRes.string.aapps_settings), onClick)
}

@Composable
private fun TopRow(mode: LevelMode, onOpenSettings: (() -> Unit)?) {
    val c = Aapps.colors
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.border(1.dp, c.line, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(c.accent, CircleShape))
            Spacer(Modifier.width(8.dp))
            BasicText(
                stringResource(if (mode == LevelMode.EDGE) Res.string.mode_edge else Res.string.mode_flat).uppercase(),
                style = Aapps.type.label.copy(color = c.fgMuted),
            )
        }
        Spacer(Modifier.weight(1f))
        if (onOpenSettings != null) GearButton(onOpenSettings)
    }
}

// Eine Nachkommastelle mit Dezimaltrenner der Sprache, Minus als echtes Minuszeichen
internal fun formatValue(v: Float, sep: Char): String {
    val tenths = (kotlin.math.abs(v) * 10f).roundToInt()
    val sign = if (v < 0f && tenths != 0) "−" else ""
    return "$sign${tenths / 10}$sep${tenths % 10}"
}

internal fun unitSuffix(unit: AngleUnit) = when (unit) {
    AngleUnit.DEGREE -> "°"
    AngleUnit.PERCENT -> "%"
    AngleUnit.MM_PER_M -> "mm/m"
}

@Composable
private fun ReadingBlock(state: LevelState, unit: AngleUnit, level: Float, modifier: Modifier) {
    val c = Aapps.colors
    val r = state.reading
    val sep = decimalSeparator()
    val value = formatValue(TiltMath.convert(r.primary, unit), sep)
    val color by animateColorAsState(if (r.isLevel && !state.held) c.accent else c.fg, Springs.gentle(), label = "numColor")
    val big: AnnotatedString = buildAnnotatedString {
        append(value)
        withStyle(SpanStyle(fontSize = if (unit == AngleUnit.DEGREE) 1.em else 0.32.em, color = color.copy(alpha = 0.55f))) {
            append(if (unit == AngleUnit.MM_PER_M) " " + unitSuffix(unit) else unitSuffix(unit))
        }
    }
    val secondary = secondaryLine(r, unit, sep)
    val description = value + " " + unitSuffix(unit)
    Column(
        modifier
            .clickable(remember { MutableInteractionSource() }, indication = null) { state.toggleHold() }
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BasicText(
                big,
                Modifier.semantics { contentDescription = description },
                style = Aapps.type.display.copy(color = color, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 132.sp, stepSize = 2.sp),
            )
        }
        Spacer(Modifier.height(6.dp))
        StatusLine(state, secondary, level)
    }
}

@Composable
private fun ColumnScope.StatusLine(state: LevelState, secondary: String, level: Float) {
    val c = Aapps.colors
    val text = when {
        state.held -> stringResource(Res.string.held).uppercase()
        state.reading.isLevel -> stringResource(Res.string.level).uppercase()
        else -> secondary
    }
    val color = when {
        state.held -> c.accent
        state.reading.isLevel -> c.accent.copy(alpha = 0.6f + 0.4f * level)
        else -> c.fgMuted
    }
    // nur Einrasten und Halten ansagen, nicht jeden Messwert
    val announce = state.held || state.reading.isLevel
    BasicText(
        text,
        Modifier
            .align(Alignment.CenterHorizontally)
            .semantics { if (announce) liveRegion = LiveRegionMode.Polite },
        style = if (announce) {
            Aapps.type.label.copy(color = color)
        } else {
            Aapps.type.number.copy(color = color, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
        },
        maxLines = 3,
    )
}

// Flach: beide Achsen; Kante: welche Seite tiefer liegt und die beiden anderen Einheiten
@Composable
private fun secondaryLine(r: LevelReading, unit: AngleUnit, sep: Char): String {
    fun f(a: Float, u: AngleUnit) = formatValue(TiltMath.convert(a, u), sep) + (if (u == AngleUnit.MM_PER_M) " " else "") + unitSuffix(u)
    return if (r.mode == LevelMode.FLAT) {
        stringResource(Res.string.flat_axes, f(r.flatX, unit), f(r.flatY, unit))
    } else {
        val side = stringResource(if (r.edgeAngle > 0f) Res.string.right_lower else Res.string.left_lower)
        val others = AngleUnit.entries.filter { it != unit }.joinToString("  ·  ") { f(kotlin.math.abs(r.edgeAngle), it) }
        "$side\n$others"
    }
}

@Composable
private fun BottomAction(
    state: LevelState,
    session: solutions.xapient.levlr.domain.CalibrationSession?,
    onCalibrationDone: (CalibrationResult) -> Unit,
    haptics: Haptics?,
) {
    Box(Modifier.fillMaxWidth().padding(bottom = 20.dp, top = 8.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            session == null,
            enter = fadeIn(Springs.gentle()),
            exit = fadeOut(Springs.gentle()),
        ) {
            AappsPillButton(
                text = stringResource(if (state.held) Res.string.held else Res.string.hold),
                onClick = {
                    haptics?.press()
                    state.toggleHold()
                },
                icon = if (state.held) AappsIcons.Lock else AappsIcons.Unlock,
                active = state.held,
            )
        }
        AnimatedVisibility(
            session != null,
            enter = fadeIn(Springs.gentle()) + slideInVertically(Springs.snappy()) { it / 3 },
            exit = fadeOut(Springs.gentle()) + slideOutVertically(Springs.snappy()) { it / 3 },
        ) {
            if (session != null) CalibrationCard(state, session, onCalibrationDone, haptics)
        }
    }
}
