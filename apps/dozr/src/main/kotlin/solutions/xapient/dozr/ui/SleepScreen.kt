package solutions.xapient.dozr.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.dozr.R
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.domain.Moon
import solutions.xapient.dozr.domain.Phase
import solutions.xapient.dozr.domain.Sleep
import solutions.xapient.dozr.domain.TimeText
import java.util.Locale
import kotlin.math.min

// Timer: Restzeit oben, Mond-Himmel als Hero, Ausblend-Kurve, Schnellwahl bzw. +10, die eine Hauptaktion
@Composable
fun SleepScreen(
    sleep: Sleep,
    fadeMs: Long,
    haptics: Haptics?,
    onChoose: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExtend: () -> Unit,
    onSettings: () -> Unit,
    // Laufzeit-Zeitpunkt -> Uhrzeit
    wallText: (Long) -> String,
    clock: () -> Long = SystemClock::elapsedRealtime,
    animate: Boolean = true,
    dragPreview: Int? = null,
) {
    // Haeufig wechselnde Werte als State-Objekte: gelesen nur im Zeichnen bzw. in kleinen Scopes
    val now = remember { mutableLongStateOf(clock()) }
    val heroNow = remember { mutableLongStateOf(clock()) }
    val drag = remember { mutableStateOf(dragPreview) }
    val pathPx = remember { floatArrayOf(0f) }
    val lastFrac = remember { floatArrayOf(0f) }
    val idle = remember { Animatable(Moon.fractionOf(dragPreview ?: sleep.setMin)) }
    val sleepState = rememberUpdatedState(sleep)
    val fadeState = rememberUpdatedState(fadeMs)
    val inputs = remember { DialInputs(sleepState, fadeState, heroNow, drag, idle, pathPx, lastFrac) }

    val phase by remember(sleep, fadeMs) { derivedStateOf { sleep.phase(now.longValue, fadeMs) } }

    // Ticker nur waehrend des Laufs: Ziffern zur vollen Sekunde, Mond nur pro Pixel, beim Ausblenden 4/s.
    // Jeder Schritt wartet auf einen Frame: im Hintergrund pausiert die Frame-Uhr und damit der Ticker
    LaunchedEffect(sleep, fadeMs, animate) {
        now.longValue = clock()
        heroNow.longValue = now.longValue
        if (!animate) return@LaunchedEffect
        while (sleep.mode == Mode.RUNNING) {
            val rem = sleep.remaining(now.longValue)
            if (rem <= 0L) break
            val fading = sleep.phase(now.longValue, fadeMs) == Phase.FADING
            val toSecond = (rem - 1) % 1000 + 1
            delay(if (fading) min(toSecond, 250L) else toSecond)
            withFrameMillis { }
            val n = clock()
            now.longValue = n
            if (fading || n - heroNow.longValue >= Moon.msPerPixel(pathPx[0])) heroNow.longValue = n
        }
    }

    // Mond im Leerlauf per Feder; nach dem Stoppen von der letzten Laufposition aus, nach dem Ende aus dem Wasser
    LaunchedEffect(sleep.mode, animate) {
        when (sleep.mode) {
            Mode.RUNNING -> Unit
            Mode.DONE -> idle.snapTo(0f)
            Mode.IDLE -> {
                if (!animate) {
                    idle.snapTo(Moon.fractionOf(inputs.shownMinutes()))
                    return@LaunchedEffect
                }
                if (lastFrac[0] > 0f) idle.snapTo(lastFrac[0])
                lastFrac[0] = 0f
                snapshotFlow { Moon.fractionOf(inputs.shownMinutes()) }.collectLatest { idle.animateTo(it, Springs.snappy()) }
            }
        }
    }

    val running = sleep.mode == Mode.RUNNING
    // Beim Ziehen zeigen Endzeit und Kurve schon den neuen Wert (neu komponiert nur pro Raste)
    val idleMin = drag.value ?: sleep.setMin
    val shownMs = remember(sleep) {
        {
            val d = drag.value
            if (d != null) d * Sleep.MINUTE else sleep.remaining(now.longValue)
        }
    }
    val status = when {
        phase == Phase.DONE -> stringResource(R.string.state_done, wallText(sleep.endAt))
        phase == Phase.FADING || phase == Phase.ENDED -> stringResource(R.string.state_fading)
        running -> stringResource(R.string.state_stops, wallText(sleep.endAt))
        else -> stringResource(R.string.state_stops, wallText(clock() + idleMin * Sleep.MINUTE))
    }

    // Kurve: Zeiten fuer Beginn und Ende des Ausblendens
    val endAt = if (running) sleep.endAt else clock() + idleMin * Sleep.MINUTE
    val fadeFrom = if (running) sleep.fadeStart(fadeMs) else endAt - min(fadeMs, idleMin * Sleep.MINUTE)
    val fadeTitle = stringResource(R.string.fade_title_m, (fadeMs / Sleep.MINUTE).toInt())
    val graph = remember(sleep, fadeMs) {
        {
            val n = heroNow.longValue
            when (sleep.mode) {
                // Punkt nur beim Ausblenden, sonst laege er lange reglos am Kurvenanfang
                Mode.RUNNING -> GraphLook(sleep.fadeProgress(n, fadeMs).toFloat(), dim = false, dot = sleep.phase(n, fadeMs) != Phase.RUNNING)
                Mode.DONE -> GraphLook(1f, dim = true, dot = false)
                Mode.IDLE -> GraphLook(0f, dim = false, dot = false)
            }
        }
    }
    val graphDesc = stringResource(R.string.fade_desc, wallText(fadeFrom), wallText(endAt))
    val graphView: @Composable (Dp) -> Unit = { h ->
        FadeGraph(
            fadeTitle,
            if (sleep.mode == Mode.DONE) null else wallText(fadeFrom),
            if (sleep.mode == Mode.DONE) null else wallText(endAt),
            graphDesc, graph, curveHeight = h,
        )
    }

    AappsScaffold(background = { NightBackdrop() }) {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        val bigFont = LocalDensity.current.fontScale > 1.5f
        val gear: @Composable () -> Unit = {
            AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
        }
        val quick: @Composable () -> Unit = {
            QuickRow(running, sleep.setMin, { haptics?.press(); onChoose(it) }, { haptics?.press(); onExtend() })
        }
        val pill: @Composable () -> Unit = {
            AappsPillButton(
                stringResource(if (running) R.string.stop else R.string.start),
                {
                    haptics?.press()
                    if (running) onStop() else onStart()
                },
                Modifier.widthIn(min = 180.dp),
                icon = if (running) DozrIcons.Stop else DozrIcons.Play,
                active = !running,
            )
        }
        val dial: @Composable (Modifier) -> Unit = { m -> MoonDial(inputs, haptics, onChoose, m) }
        if (landscape) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.fillMaxHeight().weight(1f).padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Readout(shownMs, running, status, phase, if (bigFont) 72.dp else 92.dp)
                    if (!bigFont) Box(Modifier.widthIn(max = 420.dp)) { graphView(40.dp) }
                    quick()
                    pill()
                }
                Box(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).padding(12.dp), contentAlignment = Alignment.Center) {
                    dial(Modifier)
                }
                Spacer(Modifier.width(56.dp))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { gear() }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    gear()
                }
                // Ablesewert, Himmel und Kurve gleichmaessig verteilt, kein Loch am Rand
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Readout(shownMs, running, status, phase, if (bigFont) 88.dp else 104.dp)
                    Box(Modifier.weight(1f, fill = false), contentAlignment = Alignment.Center) { dial(Modifier) }
                    graphView(if (bigFont) 36.dp else 48.dp)
                }
                Spacer(Modifier.height(12.dp))
                quick()
                Box(Modifier.padding(bottom = 20.dp, top = 10.dp)) { pill() }
            }
        }
    }
}

// Zarter Nachtschein hinter allem, statisch
@Composable
private fun NightBackdrop() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color(0xFF0F1830), 0.45f to Aapps.colors.bg, 1f to Aapps.colors.bg,
            ),
        ),
    )
}

@Composable
private fun Readout(shownMs: () -> Long, seconds: Boolean, status: String, phase: Phase, height: Dp) {
    val c = Aapps.colors
    val parts by remember(shownMs) { derivedStateOf { TimeText.parts(shownMs()) } }
    val p = parts
    val color = if (phase == Phase.DONE) c.fgMuted else c.fg
    val unitMin = stringResource(R.string.unit_min)
    val unitS = stringResource(R.string.unit_s)
    // Grosse Minuten, klein "min", Sekunden klein und gedimmt nur im Lauf
    val text = buildAnnotatedString {
        append(p.minutes.toString())
        withStyle(SpanStyle(fontSize = 0.3.em)) { append(" $unitMin") }
        if (seconds) {
            withStyle(SpanStyle(fontSize = 0.3.em, color = c.fgMuted)) {
                append("  " + "%02d".format(Locale.ROOT, p.seconds) + " $unitS")
            }
        }
    }
    val minText = pluralStringResource(R.plurals.minutes_n, p.minutes.toInt(), p.minutes.toInt())
    val desc = if (seconds) {
        stringResource(R.string.time_left, minText, pluralStringResource(R.plurals.seconds_n, p.seconds.toInt(), p.seconds.toInt()))
    } else {
        minText
    }
    val ends = phase == Phase.IDLE || phase == Phase.RUNNING
    val statusColor = if (phase == Phase.FADING || phase == Phase.ENDED) c.accent else c.fgMuted
    val label = Aapps.type.label
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            text,
            Modifier.fillMaxWidth().height(height).semantics { contentDescription = desc },
            style = Aapps.type.display.copy(color = color, textAlign = TextAlign.Center, lineHeight = 1.em),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 32.sp, maxFontSize = 112.sp, stepSize = 2.sp),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ends) {
                val s = with(LocalDensity.current) { (label.fontSize * 1.2f).toDp() }
                ClockGlyph(statusColor, Modifier.size(s))
                Spacer(Modifier.width(6.dp))
            }
            BasicText(
                status.uppercase(),
                Modifier.weight(1f, fill = false).semantics { liveRegion = LiveRegionMode.Polite },
                style = label.copy(color = statusColor, fontSize = label.fontSize * 1.2f, textAlign = TextAlign.Center),
            )
        }
    }
}

// Kleine Uhr vor der Endzeit, rein dekorativ
@Composable
private fun ClockGlyph(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.minDimension * 0.11f
        val r = size.minDimension / 2f - w / 2f
        drawCircle(color, r, center, style = Stroke(w))
        drawLine(color, center, Offset(center.x, center.y - r * 0.6f), w, StrokeCap.Round)
        drawLine(color, center, Offset(center.x + r * 0.45f, center.y), w, StrokeCap.Round)
    }
}

// Schnellwahl im Leerlauf, +10 beim Lauf; gleiche Hoehe, damit nichts springt
@Composable
private fun QuickRow(running: Boolean, selected: Int, onChoose: (Int) -> Unit, onExtend: () -> Unit) {
    AnimatedContent(
        running,
        Modifier.heightIn(min = 56.dp),
        transitionSpec = { fadeIn(Springs.gentle()) togetherWith fadeOut(Springs.gentle()) },
        contentAlignment = Alignment.Center,
        label = "quick",
    ) { run ->
        Row(
            Modifier.heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (run) {
                Chip(stringResource(R.string.extend), false, Modifier.widthIn(min = 132.dp), onExtend)
            } else {
                Sleep.QUICK.forEach { m ->
                    Chip(
                        m.toString(), m == selected, Modifier.widthIn(min = 64.dp),
                        { onChoose(m) }, description = pluralStringResource(R.plurals.minutes_n, m, m),
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit, description: String? = null) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (selected) c.accent.copy(alpha = 0.16f) else Color.Transparent, Springs.gentle(), label = "chipBg")
    val border by animateColorAsState(if (selected) c.accent else c.line, Springs.gentle(), label = "chipBorder")
    val fg by animateColorAsState(if (selected) c.fg else c.fgMuted, Springs.gentle(), label = "chipFg")
    Box(
        modifier
            .graphicsLayer { val s = if (pressed) 0.94f else 1f; scaleX = s; scaleY = s }
            .heightIn(min = 48.dp)
            .background(bg, CircleShape)
            .border(1.dp, border, CircleShape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Aapps.type.number.copy(color = fg, fontSize = Aapps.type.body.fontSize), maxLines = 1)
    }
}
