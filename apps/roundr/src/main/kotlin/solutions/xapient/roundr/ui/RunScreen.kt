package solutions.xapient.roundr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.roundr.R
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.TimeFormat
import solutions.xapient.roundr.domain.Timeline

private const val CONFIRM_MS = 3000L

// Lauf: Farbflaeche als Hero, leert sich wie ein Fuellstand; fertig -> DoneScreen
@Composable
fun RunScreen(
    run: Run,
    clock: () -> Long,
    haptics: Haptics?,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onAgain: () -> Unit,
    onClose: () -> Unit,
    keepOn: Boolean = true,
    animate: Boolean = true,
    confirmStop: Boolean = false,
) {
    // Uhr fuer die Zeichenphase: nur waehrend des Laufs, angehalten und fertig kein einziges Bild
    val now = remember { mutableLongStateOf(clock()) }
    val running = run.session.running
    val t = run.timeline
    // Hoehe der Flaeche: neu gezeichnet wird nur, wenn sich Fuellkante (in Pixeln) oder Sekunde aendern
    var heightPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(run, animate) {
        now.longValue = clock()
        if (animate && running) {
            var shown: Any? = null
            while (!run.finished(now.longValue)) withFrameMillis {
                val c = clock()
                val p = t.at(run.session.elapsed(c))
                val key = p?.let { Triple(it.index, TimeFormat.ceilSec(it.remainingMs), (it.fraction * heightPx).toInt()) }
                if (key != shown || p == null) {
                    shown = key
                    now.longValue = c
                }
            }
        }
    }
    val finished by remember(run) { derivedStateOf { run.finished(now.longValue) } }
    if (finished) {
        DoneScreen(run.workout, onAgain, onClose)
        return
    }
    if (keepOn && running) KeepScreenOn()
    val elapsed = { run.session.elapsed(now.longValue) }
    // nur bei Abschnitts- bzw. Sekundenwechsel neu zusammensetzen
    val index by remember(run) { derivedStateOf { t.at(elapsed())?.index ?: (t.segments.size - 1) } }
    val sec by remember(run) { derivedStateOf { TimeFormat.ceilSec(t.at(elapsed())?.remainingMs ?: 0) } }
    val seg = t.segments[index]

    // Neuer Abschnitt fuellt per Feder auf, Farbe morpht
    val rise = remember { Animatable(1f) }
    var seenIndex by remember { mutableStateOf(index) }
    LaunchedEffect(index) {
        if (index != seenIndex && animate) {
            seenIndex = index
            rise.snapTo(0f)
            rise.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 90f))
        }
        seenIndex = index
    }
    val color by animateColorAsState(phaseColor(seg.phase), spring(stiffness = 120f), label = "phase")

    // Letzte 3 s: Zahl pulsiert einmal pro Sekunde
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(sec, index) {
        if (animate && running && sec in 1..3) {
            pulse.snapTo(1.1f)
            pulse.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
        }
    }

    var confirm by remember { mutableStateOf(confirmStop) }
    if (confirm && animate) LaunchedEffect(Unit) { delay(CONFIRM_MS); confirm = false }

    // Fuellstand nur in der Zeichenphase lesen
    val level = { (t.at(elapsed())?.fraction ?: 0f) * rise.value }

    Box(Modifier.fillMaxSize().onSizeChanged { heightPx = it.height }) {
        Canvas(Modifier.fillMaxSize()) {
            val c = color
            drawRect(drained(c))
            val top = size.height * (1f - level())
            // weicher Schein ueber der Fuellkante
            val band = 96.dp.toPx()
            drawRect(
                Brush.verticalGradient(listOf(c.copy(alpha = 0f), c.copy(alpha = EDGE_GLOW)), startY = top - band, endY = top),
                topLeft = Offset(0f, top - band), size = Size(size.width, band),
            )
            drawRect(c, topLeft = Offset(0f, top), size = Size(size.width, size.height - top))
            // Glanzkante
            drawRect(Color.White.copy(alpha = 0.28f), topLeft = Offset(0f, top), size = Size(size.width, 1.5.dp.toPx()))
            if (!running) {
                // angehalten: ruhige Schraffur ueber der Farbe
                val step = 22.dp.toPx()
                val w = 7.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(Ink.copy(alpha = HATCH_ALPHA), Offset(x, size.height), Offset(x + size.height, 0f), w)
                    x += step
                }
            }
        }
        // Alles Weisse wird an der Fuellkante umgefaerbt: Phasenfarbe auf Dunkel, Tinte auf Farbe
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val top = size.height * (1f - level())
                    drawRect(color, size = Size(size.width, top), blendMode = BlendMode.SrcIn)
                    drawRect(Ink, topLeft = Offset(0f, top), size = Size(size.width, size.height - top), blendMode = BlendMode.SrcIn)
                },
        ) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                RunContent(run, index, sec, running, confirm, haptics, { pulse.value }, elapsed,
                    onToggle = onToggle, onSkip = onSkip, onBack = onBack,
                    onStop = {
                        if (confirm) {
                            confirm = false
                            haptics?.confirm()
                            onStop()
                        } else {
                            haptics?.press()
                            confirm = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RunContent(
    run: Run, index: Int, sec: Int, running: Boolean, confirm: Boolean, haptics: Haptics?,
    pulse: () -> Float, elapsed: () -> Long,
    onToggle: () -> Unit, onSkip: () -> Unit, onBack: () -> Unit, onStop: () -> Unit,
) {
    val t = run.timeline
    val seg = t.segments[index]
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val fs = LocalDensity.current.fontScale
    val white = Color.White
    val label = Aapps.type.label.copy(color = white, fontSize = 14.sp)
    val roundText = if (t.sets > 1) {
        stringResource(R.string.round_set_of, seg.round, t.rounds, seg.set, t.sets)
    } else {
        stringResource(R.string.round_of, seg.round, t.rounds)
    }
    val next = t.next(index)
    val phaseText = phaseLabel(seg.phase)
    // angehalten: Zustand gross in der Phasenzeile, Phase darunter
    val sub = when {
        !running -> phaseText
        next != null -> stringResource(R.string.next_is, phaseLabel(next.phase), TimeFormat.clock((next.durationMs / 1000).toInt()))
        else -> stringResource(R.string.last_phase)
    }
    val headline = if (running) phaseText else stringResource(R.string.paused)
    val number = TimeFormat.big(sec)
    // Vorlage fuer die Groesse: alle Ziffern als 0, mindestens zwei Stellen -> Zahl springt nicht
    val template = number.map { if (it.isDigit()) '0' else it }.joinToString("").padStart(2, '0')

    val top: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            StopButton(confirm, onStop)
            Spacer(Modifier.weight(1f))
            // beim Nachfragen braucht der Hinweis den Platz
            if (!confirm) BasicText(
                roundText.uppercase(),
                Modifier.padding(end = 8.dp),
                style = label.copy(textAlign = TextAlign.End),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = label.fontSize, stepSize = 0.5.sp),
            )
        }
    }
    val bar: @Composable () -> Unit = {
        SegmentBar(t, elapsed, Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(12.dp))
    }
    val phase: @Composable () -> Unit = {
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            BasicText(
                headline.uppercase(),
                style = Aapps.type.headline.copy(color = white, fontSize = 40.sp, letterSpacing = 0.06.em),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 40.sp, stepSize = 1.sp),
            )
            Spacer(Modifier.height(4.dp))
            BasicText(sub.uppercase(), style = label, maxLines = 2)
        }
    }
    val bigNumber: @Composable (Modifier) -> Unit = { m ->
        val desc = pluralStringResource(R.plurals.seconds_left, sec, sec)
        BigNumber(number, template, pulse, !running, m.clearAndSetSemantics { contentDescription = desc })
    }
    val controls: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            RoundButton(RoundrIcons.Prev, stringResource(R.string.back), 60.dp, 30.dp, false) { haptics?.press(); onBack() }
            RoundButton(
                if (running) RoundrIcons.Pause else RoundrIcons.Play,
                stringResource(if (running) R.string.pause else R.string.resume),
                88.dp, 38.dp, true,
            ) { haptics?.confirm(); onToggle() }
            RoundButton(RoundrIcons.Next, stringResource(R.string.skip), 60.dp, 30.dp, false) { haptics?.press(); onSkip() }
        }
    }

    if (landscape) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(if (fs > 1.5f) 0.5f else 0.42f).fillMaxHeight().padding(start = 12.dp, bottom = 16.dp),
            ) {
                top()
                bar()
                Spacer(Modifier.height(16.dp))
                Box(Modifier.padding(horizontal = 12.dp)) { phase() }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { controls() }
            }
            bigNumber(Modifier.weight(if (fs > 1.5f) 0.5f else 0.58f).fillMaxHeight().padding(16.dp))
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            top()
            bar()
            Spacer(Modifier.height(24.dp))
            Box(Modifier.padding(horizontal = 12.dp)) { phase() }
            bigNumber(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp))
            Box(Modifier.fillMaxWidth().padding(bottom = 28.dp), contentAlignment = Alignment.Center) { controls() }
        }
    }
}

// Zahl so gross wie der Platz erlaubt; Groesse aus der Vorlage gemessen, damit sie zwischen Sekunden ruhig bleibt; angehalten als Umriss
@Composable
private fun BigNumber(text: String, template: String, pulse: () -> Float, outline: Boolean, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = Aapps.type.display.copy(
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        letterSpacing = (-0.03).em,
        lineHeight = 1.em,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        val style = remember(template, maxW, maxH, density, base) {
            fun fits(px: Float): Boolean {
                val s = base.copy(fontSize = with(density) { px.toSp() })
                val r = measurer.measure(template, s, maxLines = 1, softWrap = false)
                return r.size.width <= maxW * 0.94f && r.size.height <= maxH
            }
            var lo = 8f
            var hi = 2400f
            repeat(18) {
                val mid = (lo + hi) / 2
                if (fits(mid)) lo = mid else hi = mid
            }
            base.copy(fontSize = with(density) { lo.toSp() })
        }
        if (outline) {
            // nur Aussenkontur: breiter Strich, Fuellung ausgestanzt; ueberlappende Glyph-Teile bleiben unsichtbar
            val stroke = style.copy(drawStyle = Stroke(width = with(density) { style.fontSize.toPx() } * OUTLINE * 2))
            Box(Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }, contentAlignment = Alignment.Center) {
                BasicText(text, style = stroke, maxLines = 1, softWrap = false)
                BasicText(text, Modifier.graphicsLayer { blendMode = BlendMode.DstOut }, style = style, maxLines = 1, softWrap = false)
            }
            return@BoxWithConstraints
        }
        BasicText(
            text,
            Modifier.graphicsLayer {
                val s = pulse()
                scaleX = s
                scaleY = s
            },
            style = style,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// Strichstaerke des Umrisses relativ zur Schriftgroesse
private const val OUTLINE = 0.03f

// Alle Abschnitte als Leiste: Arbeit hoch, Pausen flach; erledigt voll, kommend blass; Grundform gecacht
@Composable
private fun SegmentBar(t: Timeline, elapsed: () -> Long, modifier: Modifier) {
    Spacer(
        modifier.drawWithCache {
            val n = t.segments.size
            val gap = (size.width / n * 0.25f).coerceAtMost(3.dp.toPx())
            val usable = size.width - gap * (n - 1)
            val r = CornerRadius(3.dp.toPx())
            var x = 0f
            val rects = t.segments.map { s ->
                val w = usable * s.durationMs / t.totalMs
                val h = if (s.phase == solutions.xapient.roundr.domain.Phase.WORK) size.height else size.height * 0.45f
                Rect(Offset(x, (size.height - h) / 2), Size(w, h)).also { x += w + gap }
            }
            val pale = Path().apply { rects.forEach { addRoundRect(RoundRect(it, r)) } }
            onDrawBehind {
                drawPath(pale, Color.White.copy(alpha = 0.3f))
                val e = elapsed().coerceIn(0, t.totalMs)
                t.segments.forEachIndexed { i, s ->
                    val done = ((e - s.startMs).toFloat() / s.durationMs).coerceIn(0f, 1f)
                    if (done <= 0f) return@onDrawBehind
                    val b = rects[i]
                    drawRoundRect(Color.White, b.topLeft, Size(b.width * done, b.height), r)
                }
            }
        },
    )
}

@Composable
private fun RoundButton(
    icon: ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp,
    ring: Boolean, onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, Springs.bouncy(), label = "btn")
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (ring) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier)
            .clickable(source, indication = null, role = Role.Button, onClickLabel = null, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(icon, null, Color.White, size = iconSize)
    }
}

// Beenden: erster Tipp fragt nach, zweiter beendet
@Composable
private fun StopButton(confirm: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        Modifier
            .pressScale(pressed)
            .heightIn(min = 48.dp)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AappsIcon(RoundrIcons.Close, stringResource(R.string.stop), Color.White, size = 24.dp)
        if (confirm) {
            Spacer(Modifier.width(8.dp))
            BasicText(
                stringResource(R.string.stop_confirm).uppercase(),
                style = Aapps.type.label.copy(color = Color.White, fontSize = 14.sp),
                maxLines = 1,
            )
        }
    }
}
