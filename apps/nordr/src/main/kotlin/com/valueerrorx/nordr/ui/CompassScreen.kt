package com.valueerrorx.nordr.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.valueerrorx.aapps.core.ui.Aapps
import com.valueerrorx.aapps.core.ui.AappsIconButton
import com.valueerrorx.aapps.core.ui.AappsIcons
import com.valueerrorx.aapps.core.ui.AappsPillButton
import com.valueerrorx.aapps.core.ui.AappsScaffold
import com.valueerrorx.aapps.core.ui.Haptics
import com.valueerrorx.aapps.core.ui.Springs
import com.valueerrorx.nordr.R
import com.valueerrorx.nordr.domain.Accuracy
import com.valueerrorx.nordr.domain.Angles
import com.valueerrorx.nordr.domain.NorthRef
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Hauptbildschirm: Richtung, Rose, eine Hauptaktion
@Composable
fun CompassScreen(
    state: CompassState,
    northRef: NorthRef,
    haptics: Haptics?,
    onOpenSettings: () -> Unit,
    onCourseChanged: (Float?) -> Unit,
    sensorMissing: Boolean = false,
    animate: Boolean = true,
    shineOverride: Float = -1f,
    hintPhase: Float = -1f,
) {
    val c = Aapps.colors
    // nur abgeleitete Werte lesen, damit nicht jeder Messwert den ganzen Screen neu aufbaut
    val onCourse by remember(state) { derivedStateOf { state.reading.onCourse && state.reading.course != null } }
    val accuracy by remember(state) { derivedStateOf { state.reading.accuracy } }
    val level = animateFloatAsState(if (onCourse) 1f else 0f, Springs.gentle(), label = "level")
    val warn = animateFloatAsState(if (accuracy != Accuracy.GOOD) 1f else 0f, Springs.gentle(), label = "warn")
    val shine = remember { Animatable(-1f) }
    val measurer = rememberTextMeasurer(cacheSize = 24)
    val points = stringArrayResource(R.array.points)
    val letters = remember(points.toList()) { listOf(points[0], points[4], points[8], points[12]) }

    if (animate) {
        // Physik-Schleife: laeuft nur, solange sich etwas bewegt
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
        // nur neue Ereignisse, nicht beim Zurueckkehren aus den Einstellungen
        var snapsHandled by remember { mutableIntStateOf(state.courseSnaps) }
        LaunchedEffect(state.courseSnaps) {
            if (state.courseSnaps == snapsHandled) return@LaunchedEffect
            snapsHandled = state.courseSnaps
            haptics?.confirm()
            shine.snapTo(0f)
            shine.animateTo(1f, spring(dampingRatio = 1f, stiffness = 60f))
            shine.snapTo(-1f)
        }
        var ticksHandled by remember { mutableIntStateOf(state.cardinalTicks) }
        LaunchedEffect(state.cardinalTicks) {
            if (state.cardinalTicks == ticksHandled) return@LaunchedEffect
            ticksHandled = state.cardinalTicks
            haptics?.tick()
        }
    }

    val numberStyle = Aapps.type.number
    // Teile einmal pro Groesse; Zustand wird erst beim Zeichnen gelesen
    val dialModifier = remember(state, letters, c, measurer, numberStyle, shineOverride, level, warn) {
        Modifier.drawWithCache {
            val parts = DialParts(this, letters, c, measurer, numberStyle)
            onDrawBehind {
                state.frame // bei jedem Physik-Schritt neu zeichnen
                val look = DialLook(
                    course = state.reading.course,
                    level = level.value,
                    warn = warn.value,
                    shine = if (shineOverride >= 0f) shineOverride else shine.value,
                )
                drawCompass(parts, state.physics, look)
            }
        }
    }

    AappsScaffold {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        val action: @Composable () -> Unit = {
            BottomAction(state, haptics, onCourseChanged, hintPhase)
        }

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.42f).fillMaxHeight().padding(start = 8.dp)) {
                    TopRow(northRef, accuracy, null)
                    ReadingBlock(state, Modifier.weight(1f).fillMaxWidth())
                    action()
                }
                Box(Modifier.weight(0.58f).fillMaxHeight()) {
                    Spacer(dialModifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { GearButton(onOpenSettings) }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopRow(northRef, accuracy, onOpenSettings)
                ReadingBlock(state, Modifier.weight(0.5f).fillMaxWidth())
                Spacer(dialModifier.weight(1.7f).fillMaxWidth())
                action()
            }
        }
        if (sensorMissing) {
            Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                BasicText(
                    stringResource(R.string.no_sensor),
                    Modifier.padding(32.dp),
                    style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center),
                )
            }
        }
    }
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    AappsIconButton(AappsIcons.Gear, stringResource(com.valueerrorx.aapps.core.ui.R.string.aapps_settings), onClick)
}

// Nordbezug mit Genauigkeitspunkt, Zahnrad
@Composable
private fun TopRow(northRef: NorthRef, accuracy: Accuracy, onOpenSettings: (() -> Unit)?) {
    val c = Aapps.colors
    val dot by animateColorAsState(if (accuracy == Accuracy.GOOD) c.accent else Warn, Springs.gentle(), label = "dot")
    val low = stringResource(R.string.accuracy_low)
    val label = stringResource(if (northRef == NorthRef.TRUE) R.string.north_true else R.string.north_magnetic)
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .border(1.dp, c.line, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) {
                    if (accuracy != Accuracy.GOOD) contentDescription = "$label, $low"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(dot, CircleShape))
            Spacer(Modifier.width(8.dp))
            BasicText(label.uppercase(), style = Aapps.type.label.copy(color = c.fgMuted), maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        if (onOpenSettings != null) GearButton(onOpenSettings)
    }
}

// Betrag der Abweichung in ganzen Grad; Kurs selbst ueber Angles.whole
internal fun formatDegrees(v: Float): String = "${abs(v).roundToInt()}°"

@Composable
private fun ReadingBlock(state: CompassState, modifier: Modifier) {
    val c = Aapps.colors
    val r = state.reading
    val points = stringArrayResource(R.array.points)
    val onCourse = r.course != null && r.onCourse
    val color by animateColorAsState(if (onCourse) c.accent else c.fg, Springs.gentle(), label = "numColor")
    val value = if (r.valid) r.whole.toString() else "–––"
    val big = buildAnnotatedString {
        append(value)
        withStyle(SpanStyle(color = color.copy(alpha = 0.55f))) { append("°") }
    }
    val description = if (r.valid) stringResource(R.string.heading_desc, r.whole, points[r.point]) else ""
    Column(
        modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BasicText(
                big,
                Modifier.semantics { contentDescription = description },
                style = Aapps.type.display.copy(color = color, textAlign = TextAlign.Center, letterSpacing = (-0.03).em),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 120.sp, stepSize = 2.sp),
            )
        }
        Spacer(Modifier.height(4.dp))
        StatusLine(state, points)
    }
}

@Composable
private fun StatusLine(state: CompassState, points: Array<String>) {
    val c = Aapps.colors
    val r = state.reading
    val course = r.course
    if (!r.valid) return
    val muted = Aapps.type.number.copy(color = c.fgMuted, textAlign = TextAlign.Center)
    if (course == null) {
        BasicText(points[r.point], style = muted.copy(fontSize = 20.sp, letterSpacing = 0.12.em), maxLines = 1)
        return
    }
    // Kursmodus: Abweichung ist die Hauptbotschaft, Himmelsrichtung und Kurs klein darunter
    val main = Aapps.type.number.copy(fontSize = 26.sp, textAlign = TextAlign.Center, lineHeight = 30.sp)
    if (r.onCourse) {
        // nur das Einrasten ansagen, nicht jeden Messwert
        BasicText(
            stringResource(R.string.on_course).uppercase(),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = main.copy(color = c.accent, letterSpacing = 0.1.em),
            maxLines = 1,
        )
    } else {
        val dev = formatDegrees(r.deviation)
        val text = if (r.deviation > 0f) {
            stringResource(R.string.dev_right, dev) + "  →"
        } else {
            "←  " + stringResource(R.string.dev_left, dev)
        }
        BasicText(text, style = main.copy(color = c.fg), maxLines = 2)
    }
    Spacer(Modifier.height(6.dp))
    val sub = points[r.point] + "  ·  " + stringResource(R.string.course_line, "${Angles.whole(course)}°")
    BasicText(sub, style = muted.copy(fontSize = 15.sp, lineHeight = 20.sp), maxLines = 2)
}

@Composable
private fun BottomAction(state: CompassState, haptics: Haptics?, onCourseChanged: (Float?) -> Unit, hintPhase: Float) {
    val hint = state.hint
    val hasCourse by remember(state) { derivedStateOf { state.reading.course != null } }
    val valid by remember(state) { derivedStateOf { state.reading.valid } }
    Column(
        Modifier.fillMaxWidth().padding(bottom = 20.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hinweis als Karte ueber der Pille; der letzte bleibt beim Ausblenden stehen
        AnimatedContent(
            hint,
            transitionSpec = {
                (fadeIn(Springs.gentle()) + slideInVertically(Springs.snappy()) { it / 3 })
                    .togetherWith(fadeOut(Springs.gentle()))
                    .using(SizeTransform(clip = false))
            },
            label = "hint",
        ) { h ->
            if (h != null) {
                Box(Modifier.padding(bottom = 12.dp)) {
                    HintCard(h, state.hintIsManual, { state.dismissHint() }, hintPhase)
                }
            }
        }
        AappsPillButton(
            text = stringResource(if (hasCourse) R.string.clear_course else R.string.set_course),
            onClick = {
                haptics?.press()
                onCourseChanged(state.toggleCourse())
            },
            icon = AappsIcons.Target,
            active = hasCourse,
            enabled = valid,
        )
    }
}

internal fun formatDeclination(deg: Float, locale: Locale): String = String.format(locale, "%.1f°", abs(deg))
