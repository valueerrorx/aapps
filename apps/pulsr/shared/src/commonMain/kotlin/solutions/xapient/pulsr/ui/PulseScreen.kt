package solutions.xapient.pulsr.ui

import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_settings
import solutions.xapient.pulsr.res.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.min
import solutions.xapient.aapps.core.ui.drawGlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.pulsr.domain.Beats
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.frameNanos
import solutions.xapient.pulsr.presentNanos
import solutions.xapient.pulsr.rememberReject
import solutions.xapient.pulsr.uptimeMillis
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.Subdivision
import solutions.xapient.pulsr.domain.Tempo

// Metronom: Rad als Hero, darunter Tempo- und Taktsteuerung, eine Hauptaktion
@Composable
fun PulseScreen(
    settings: Settings,
    playing: Boolean,
    pulse: (Long) -> Pulse?,
    haptics: Haptics?,
    // true = Tempo hat sich geaendert
    onNudge: (Int) -> Boolean,
    onBpm: (Int) -> Unit,
    // Zeit des Aufsetzens in uptimeMillis
    onTap: (Long) -> Unit,
    onBeats: (Int) -> Unit,
    onSub: (Subdivision) -> Unit,
    onToggle: () -> Unit,
    onSettings: () -> Unit,
    animate: Boolean = true,
) {
    // Bildzeit nur waehrend des Spielens; gestoppt kein einziges Bild
    val frame = remember { mutableLongStateOf(0L) }
    LaunchedEffect(playing, animate) {
        if (playing && animate) while (true) withFrameNanos { frame.longValue = it }
    }
    if (playing && settings.keepOn) KeepScreenOn()
    // Bild erscheint etwa ein Frame nach seiner Bildzeit: Puls fuer diesen Moment, einmal je Bild
    val ahead = frameNanos()
    val memo = remember(playing) { PulseMemo() }
    val current = { memo.at(frame.longValue) { pulse(presentNanos(it) + ahead) } }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }

    // Haptik nur, wenn sich das Tempo wirklich aendert; am Rand einmal Ablehnung
    var atLimit by remember { mutableStateOf(false) }
    val reject = rememberReject()
    val dial: @Composable (Modifier) -> Unit = { m ->
        Dial(
            bpm = settings.bpm, beats = settings.beats, sub = settings.sub.count, accent = settings.accent, playing = playing, pulse = current,
            onRotate = {
                if (onNudge(it)) {
                    atLimit = false
                    haptics?.step()
                } else if (!atLimit) {
                    atLimit = true
                    if (haptics != null) reject()
                }
            },
            onSet = onBpm,
            modifier = m,
        )
    }
    val gear: @Composable () -> Unit = {
        AappsIconButton(AappsIcons.Gear, stringResource(CoreRes.string.aapps_settings), onSettings)
    }
    val main: @Composable () -> Unit = {
        AappsPillButton(
            stringResource(if (playing) Res.string.stop else Res.string.start),
            { haptics?.press(); onToggle() },
            Modifier.widthIn(min = 176.dp),
            icon = if (playing) PulsrIcons.Stop else PulsrIcons.Play,
            active = !playing,
        )
    }
    AappsScaffold {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(0.46f).fillMaxHeight().padding(start = 16.dp, end = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) { gear() }
                    Spacer(Modifier.weight(1f))
                    TempoRow(settings.bpm, haptics, onNudge, onTap)
                    Spacer(Modifier.height(16.dp))
                    MeterRow(settings, haptics, onBeats, onSub)
                    Spacer(Modifier.weight(1f))
                    main()
                }
                dial(Modifier.weight(0.54f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Taktart oben, Rad fuellt die Mitte, Zaehler und Tempo darunter: kein Leerraum um das Rad
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BeatsStepper(settings.beats, haptics, onBeats)
                    Spacer(Modifier.weight(1f))
                    gear()
                }
                // Rad quadratisch, mit dem Zaehler als Gruppe mittig im freien Raum: Zaehler sitzt direkt am Ring
                val counterH = counterHeight(LocalDensity.current.fontScale)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, maxHeight - counterH).coerceAtLeast(0.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        dial(Modifier.size(side))
                        BeatCounter(settings.beats, settings.accent, playing, current, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                TempoRow(settings.bpm, haptics, onNudge, onTap)
                Spacer(Modifier.height(14.dp))
                SubPicker(settings.sub, haptics, onSub)
                Spacer(Modifier.height(20.dp))
                Box(Modifier.padding(bottom = 16.dp)) { main() }
            }
        }
    }
}

// − TAP +
@Composable
private fun TempoRow(bpm: Int, haptics: Haptics?, onNudge: (Int) -> Boolean, onTap: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        StepButton(PulsrIcons.Minus, stringResource(Res.string.slower), bpm > Tempo.MIN, haptics, big = true) { onNudge(-1) }
        TapButton(haptics, onTap)
        StepButton(PulsrIcons.Plus, stringResource(Res.string.faster), bpm < Tempo.MAX, haptics, big = true) { onNudge(+1) }
    }
}

// Tippen im Takt; jeder Tipp leuchtet kurz auf
@Composable
private fun TapButton(haptics: Haptics?, onTap: (Long) -> Unit) {
    val c = Aapps.colors
    val tap by rememberUpdatedState(onTap)
    var pressed by remember { mutableStateOf(false) }
    val lit by animateFloatAsState(if (pressed) 1f else 0f, Springs.snappy(), label = "tap")
    val desc = stringResource(Res.string.tap_desc)
    val fill by animateColorAsState(if (pressed) c.accent else c.surface, Springs.snappy(), label = "tapFill")
    val fg by animateColorAsState(if (pressed) c.onAccent else c.fg, Springs.snappy(), label = "tapFg")
    Box(
        Modifier
            .pressScale(pressed, 0.94f)
            .glow(c.accent, 64.dp, 0.35f * lit)
            .widthIn(min = 128.dp)
            .heightIn(min = 68.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, if (pressed) c.accent else c.line, CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = desc
                onClick { tap(uptimeMillis()); true }
            }
            // Anschlag zaehlt beim Aufsetzen, nicht beim Loslassen
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = true
                    // Zeit des Aufsetzens, nicht der Auswertung: Tempo haengt nicht an Verzoegerungen im UI-Thread
                    tap(down.uptimeMillis)
                    haptics?.press()
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            stringResource(Res.string.tap).uppercase(),
            style = Aapps.type.number.copy(color = fg, fontSize = 22.sp, letterSpacing = Aapps.type.label.letterSpacing, textAlign = TextAlign.Center),
            maxLines = 1,
        )
    }
}

// Tippen = ein Schritt, Halten wiederholt immer schneller
@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, haptics: Haptics?, big: Boolean = false, onStep: () -> Unit) {
    val c = Aapps.colors
    val step by rememberUpdatedState(onStep)
    val on by rememberUpdatedState(enabled)
    var pressed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, Springs.gentle(), label = "step")
    Box(
        Modifier
            .size(if (big) 60.dp else 48.dp)
            .graphicsLayer { this.alpha = alpha }
            .pressScale(pressed, 0.86f)
            .background(if (pressed) c.surfaceHigh else if (big) c.surface else Color.Transparent, CircleShape)
            .then(if (big) Modifier.border(1.dp, c.line, CircleShape) else Modifier)
            .semantics {
                role = Role.Button
                contentDescription = desc
                if (enabled) onClick { step(); true }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        if (!on) return@awaitEachGesture
                        pressed = true
                        step()
                        haptics?.press()
                        val repeat = launch {
                            delay(420)
                            var gap = 120L
                            while (on) {
                                step()
                                haptics?.step()
                                delay(gap)
                                gap = (gap * 0.88).toLong().coerceAtLeast(35)
                            }
                        }
                        waitForUpOrCancellation()
                        repeat.cancel()
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(icon, null, c.fg, size = 22.dp)
    }
}

// Taktart-Stepper und Unterteilung; bricht bei wenig Platz in zwei Zeilen um
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeterRow(s: Settings, haptics: Haptics?, onBeats: (Int) -> Unit, onSub: (Subdivision) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        BeatsStepper(s.beats, haptics, onBeats)
        SubPicker(s.sub, haptics, onSub)
    }
}

@Composable
private fun BeatsStepper(beats: Int, haptics: Haptics?, onBeats: (Int) -> Unit) {
    val c = Aapps.colors
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val meter = stringResource(Res.string.meter)
    val beatsState = pluralStringResource(Res.plurals.beats_state, beats, beats)
    Row(
        Modifier.background(c.surface, CircleShape).border(1.dp, c.line, CircleShape).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(AappsIcons.Back, stringResource(Res.string.fewer_beats), beats > Beats.MIN, haptics) { onBeats(beats - 1) }
        BasicText(
            "$beats/4",
            Modifier.widthIn(min = 52.dp * fs).semantics { contentDescription = meter; stateDescription = beatsState },
            style = Aapps.type.number.copy(color = c.fg, fontSize = 22.sp, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        StepButton(AappsIcons.Chevron, stringResource(Res.string.more_beats), beats < Beats.MAX, haptics) { onBeats(beats + 1) }
    }
}

@Composable
private fun SubPicker(current: Subdivision, haptics: Haptics?, onSub: (Subdivision) -> Unit) {
    val c = Aapps.colors
    Row(
        Modifier
            .background(c.surface, CircleShape)
            .border(1.dp, c.line, CircleShape)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Subdivision.entries.forEach { sub ->
            val sel = sub == current
            val bg by animateColorAsState(if (sel) c.accent else Color.Transparent, Springs.gentle(), label = "sub")
            val fg by animateColorAsState(if (sel) c.onAccent else c.fgMuted, Springs.gentle(), label = "subFg")
            val label = subLabel(sub)
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .selectable(sel, indication = null, interactionSource = null, role = Role.RadioButton) {
                        if (!sel) {
                            haptics?.step()
                            onSub(sub)
                        }
                    }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                AappsIcon(PulsrIcons.note(sub), null, fg, size = 26.dp)
            }
        }
    }
}

private fun counterHeight(fontScale: Float) = 60.dp * fontScale.coerceIn(1f, 1.4f)

// Puls einmal je Bild berechnen; Rad, Zahl und Zaehler lesen denselben Wert
private class PulseMemo {
    private var time = Long.MIN_VALUE
    private var value: Pulse? = null

    fun at(t: Long, compute: (Long) -> Pulse?): Pulse? {
        if (t != time) {
            time = t
            value = compute(t)
        }
        return value
    }
}

// Zaehler 1..n: laufender Schlag als Akzent-Kreis, Punkt unter der betonten Eins; Puls nur in der Zeichenphase
@Composable
private fun BeatCounter(beats: Int, accent: Boolean, playing: Boolean, pulse: () -> Pulse?, modifier: Modifier = Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    val style = Aapps.type.number
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    Spacer(
        modifier
            .height(counterHeight(fs))
            .drawWithCache {
                val slot = min(size.width / beats, 64.dp.toPx())
                val r = min(slot * 0.42f, size.height * 0.4f)
                val font = min(22.sp.toPx() * fs, r * 1.1f)
                val layouts = (1..beats).map { measurer.measure(it.toString(), style.copy(fontSize = font.toSp())) }
                val x0 = (size.width - slot * beats) / 2
                val cy = size.height * 0.44f
                onDrawBehind {
                    val p = if (playing) pulse() else null
                    val f = p?.let { flash(it.sinceBeatMs, it.beatMs) } ?: 0f
                    layouts.forEachIndexed { i, l ->
                        val at = Offset(x0 + slot * (i + 0.5f), cy)
                        val cur = p != null && p.beat == i
                        if (cur) {
                            drawGlow(c.accent, at, r * 2.2f, 0.45f * f)
                            drawCircle(c.accent, r * (1f + 0.12f * f), at)
                        }
                        drawText(l, if (cur) c.onAccent else c.fgMuted, Offset(at.x - l.size.width / 2f, at.y - l.size.height / 2f))
                        if (i == 0 && accent && beats > 1) {
                            drawCircle(c.accent, 2.dp.toPx(), Offset(at.x, cy + r * 1.12f + 6.dp.toPx()))
                        }
                    }
                }
            },
    )
}
