package solutions.xapient.hertzr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.hertzr.R
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Level
import solutions.xapient.hertzr.domain.Mode
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.domain.Warning
import solutions.xapient.hertzr.domain.Wave
import kotlin.math.roundToInt

enum class InputTarget { NONE, HZ, FROM, TO }

// Alle Aktionen des Hauptschirms; Standard = nichts (Screenshots)
class ToneActions(
    // true = Wert hat sich geaendert (Haptik)
    val onHz: (Double) -> Boolean = { false },
    val onNudge: (Int) -> Boolean = { false },
    val onOctave: (Boolean) -> Boolean = { false },
    val onWave: (Wave) -> Unit = {},
    val onLevel: (Int) -> Boolean = { false },
    val onMode: (Mode) -> Unit = {},
    val onSweepFrom: (Double) -> Unit = {},
    val onSweepTo: (Double) -> Unit = {},
    val onDuration: () -> Unit = {},
    val onToggle: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

// Generator: Rad als Hero, darunter Bedienfeld fester Hoehe, Welle, Pegel, eine Hauptaktion
@Composable
fun ToneScreen(
    settings: Settings,
    playing: Boolean,
    maxHz: Double,
    // was gerade klingt (Sweep live); nur in der Zeichenphase gelesen
    live: () -> Double,
    headphones: Boolean,
    haptics: Haptics?,
    actions: ToneActions,
    animate: Boolean = true,
    initialInput: InputTarget = InputTarget.NONE,
    initialText: String = "",
    // je Bild der Spiel-Schleife (Test: Leerlauf ohne Frames)
    onFrame: () -> Unit = {},
) {
    // Bildzeit nur waehrend des Spielens; gestoppt kein einziges Bild
    val frame = remember { mutableLongStateOf(0L) }
    LaunchedEffect(playing, animate) {
        if (playing && animate) {
            while (true) {
                withFrameNanos { frame.longValue = it }
                onFrame()
            }
        }
    }
    if (playing && settings.keepOn) KeepScreenOn()
    var input by rememberSaveable { mutableStateOf(initialInput) }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val sweep = settings.mode == Mode.SWEEP
    val warning = Level.warning(settings.levelDb, headphones)

    val dial: @Composable (Modifier) -> Unit = { m ->
        FreqDial(
            hz = live,
            baseHz = settings.hz,
            maxHz = maxHz,
            wave = settings.wave,
            playing = playing,
            frame = { frame.longValue },
            band = if (sweep) settings.sweepFrom to settings.sweepTo else null,
            haptics = haptics,
            onHz = if (sweep) null else actions.onHz,
            onEdit = if (sweep) null else ({ input = InputTarget.HZ }),
            modifier = m,
        )
    }
    val top: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeSwitch(settings.mode, haptics, actions.onMode)
            Spacer(Modifier.weight(1f))
            AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), actions.onSettings)
        }
    }
    val panel: @Composable () -> Unit = {
        SwapPanel(
            showFirst = !sweep,
            first = { ToneControls(settings.hz, maxHz, haptics, actions) },
            second = {
                SweepControls(settings, haptics, { input = InputTarget.FROM }, { input = InputTarget.TO }, actions.onDuration)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
    val level: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            LevelSlider(settings.levelDb, warning, haptics, actions.onLevel)
            WarningLine(warning)
        }
    }
    val main: @Composable () -> Unit = {
        AappsPillButton(
            stringResource(if (playing) R.string.stop else if (sweep) R.string.start_sweep else R.string.start),
            { haptics?.confirm(); actions.onToggle() },
            Modifier.widthIn(min = 176.dp),
            icon = if (playing) HertzrIcons.Stop else HertzrIcons.Play,
            active = !playing,
        )
    }

    AappsScaffold {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(0.5f).fillMaxHeight().padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    top()
                    Spacer(Modifier.weight(1f))
                    panel()
                    Spacer(Modifier.height(10.dp))
                    WavePicker(settings.wave, haptics, actions.onWave)
                    Spacer(Modifier.height(6.dp))
                    level()
                    Spacer(Modifier.weight(1f))
                    main()
                }
                dial(Modifier.weight(0.5f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                top()
                // Rad passt sich der Hoehe an (Bogen unten offen): kein Leerraum darunter
                dial(Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                panel()
                Spacer(Modifier.height(12.dp))
                WavePicker(settings.wave, haptics, actions.onWave)
                Spacer(Modifier.height(8.dp))
                level()
                Spacer(Modifier.height(8.dp))
                Box(Modifier.padding(bottom = 16.dp)) { main() }
            }
        }
        if (input != InputTarget.NONE) {
            val target = input
            val current = when (target) {
                InputTarget.FROM -> settings.sweepFrom
                InputTarget.TO -> settings.sweepTo
                else -> settings.hz
            }
            FreqInput(
                title = stringResource(
                    when (target) {
                        InputTarget.FROM -> R.string.sweep_from_title
                        InputTarget.TO -> R.string.sweep_to_title
                        else -> R.string.frequency
                    },
                ),
                current = current,
                maxHz = maxHz,
                initial = initialText,
                focus = animate,
                onApply = { hz ->
                    haptics?.confirm()
                    when (target) {
                        InputTarget.FROM -> actions.onSweepFrom(hz)
                        InputTarget.TO -> actions.onSweepTo(hz)
                        else -> actions.onHz(hz)
                    }
                    input = InputTarget.NONE
                },
                onDismiss = { input = InputTarget.NONE },
            )
        }
    }
}

// Misst beide Inhalte, zeigt einen: gleiche Hoehe in beiden Modi, nichts springt
@Composable
private fun SwapPanel(showFirst: Boolean, first: @Composable () -> Unit, second: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(first, second), modifier = modifier) { (a, b), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val pa = a.map { it.measure(loose) }
        val pb = b.map { it.measure(loose) }
        val w = constraints.maxWidth
        val h = maxOf(pa.maxOfOrNull { it.height } ?: 0, pb.maxOfOrNull { it.height } ?: 0)
        layout(w, h) {
            (if (showFirst) pa else pb).forEach { it.place((w - it.width) / 2, (h - it.height) / 2) }
        }
    }
}

// TON | SWEEP
@Composable
private fun ModeSwitch(mode: Mode, haptics: Haptics?, onMode: (Mode) -> Unit) {
    val c = Aapps.colors
    Row(
        Modifier.background(c.surface, CircleShape).border(1.dp, c.line, CircleShape).padding(3.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(Mode.TONE to R.string.mode_tone, Mode.SWEEP to R.string.mode_sweep).forEach { (m, label) ->
            val sel = m == mode
            val bg by animateColorAsState(if (sel) c.surfaceHigh else Color.Transparent, Springs.gentle(), label = "mode")
            val fg by animateColorAsState(if (sel) c.fg else c.fgMuted, Springs.gentle(), label = "modeFg")
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .widthIn(min = 76.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .then(if (sel) Modifier.border(1.dp, c.accent.copy(alpha = 0.55f), CircleShape) else Modifier)
                    .selectable(sel, indication = null, interactionSource = null, role = Role.RadioButton) {
                        if (!sel) {
                            haptics?.tick()
                            onMode(m)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(stringResource(label).uppercase(), style = Aapps.type.label.copy(color = fg, fontSize = 13.sp), maxLines = 1)
            }
        }
    }
}

// ÷2  −  +  ×2
@Composable
private fun ToneControls(hz: Double, maxHz: Double, haptics: Haptics?, actions: ToneActions) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        StepButton(stringResource(R.string.octave_down), hz > Freq.MIN, haptics, repeat = false, { actions.onOctave(false) }) { fg ->
            BasicText("÷2", style = Aapps.type.number.copy(color = fg, fontSize = 19.sp), maxLines = 1)
        }
        StepButton(stringResource(R.string.lower), hz > Freq.MIN, haptics, repeat = true, { actions.onNudge(-1) }) { fg ->
            AappsIcon(HertzrIcons.Minus, null, fg, size = 22.dp)
        }
        StepButton(stringResource(R.string.higher), hz < maxHz, haptics, repeat = true, { actions.onNudge(1) }) { fg ->
            AappsIcon(HertzrIcons.Plus, null, fg, size = 22.dp)
        }
        StepButton(stringResource(R.string.octave_up), hz < maxHz, haptics, repeat = false, { actions.onOctave(true) }) { fg ->
            BasicText("×2", style = Aapps.type.number.copy(color = fg, fontSize = 19.sp), maxLines = 1)
        }
    }
}

// Tippen = ein Schritt; mit repeat wiederholt Halten immer schneller
@Composable
private fun StepButton(
    desc: String,
    enabled: Boolean,
    haptics: Haptics?,
    repeat: Boolean,
    onStep: () -> Boolean,
    content: @Composable (Color) -> Unit,
) {
    val c = Aapps.colors
    val step by rememberUpdatedState(onStep)
    val on by rememberUpdatedState(enabled)
    var pressed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, Springs.gentle(), label = "step")
    Box(
        Modifier
            .size(60.dp)
            .graphicsLayer { this.alpha = alpha }
            .pressScale(pressed, 0.88f)
            .background(if (pressed) c.surfaceHigh else c.surface, CircleShape)
            .border(1.dp, c.line, CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = desc
                if (enabled) onClick { step(); true }
            }
            .pointerInput(repeat) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        if (!on) return@awaitEachGesture
                        pressed = true
                        if (step()) haptics?.tick()
                        val job = if (repeat) {
                            launch {
                                delay(420)
                                var gap = 110L
                                while (on) {
                                    if (step()) haptics?.tick()
                                    delay(gap)
                                    gap = (gap * 0.9).toLong().coerceAtLeast(30)
                                }
                            }
                        } else {
                            null
                        }
                        waitForUpOrCancellation()
                        job?.cancel()
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(c.fg)
    }
}

// VON → BIS · DAUER
@Composable
private fun SweepControls(s: Settings, haptics: Haptics?, onFrom: () -> Unit, onTo: () -> Unit, onDuration: () -> Unit) {
    val dec = stringResource(R.string.decimal_sep).first()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Chip(stringResource(R.string.sweep_from), hzText(s.sweepFrom, dec), stringResource(R.string.sweep_from_title), Modifier.weight(1f)) {
            haptics?.press(); onFrom()
        }
        AappsIcon(HertzrIcons.Arrow, null, Aapps.colors.fgFaint, Modifier.padding(horizontal = 4.dp), size = 18.dp)
        Chip(stringResource(R.string.sweep_to), hzText(s.sweepTo, dec), stringResource(R.string.sweep_to_title), Modifier.weight(1f)) {
            haptics?.press(); onTo()
        }
        Spacer(Modifier.width(10.dp))
        Chip(stringResource(R.string.sweep_time), stringResource(R.string.seconds, s.sweepSec), stringResource(R.string.sweep_time_desc), Modifier.weight(0.78f)) {
            haptics?.tick(); onDuration()
        }
    }
}

@Composable
private fun Chip(label: String, value: String, desc: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Column(
        modifier
            .pressScale(pressed, 0.95f)
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (pressed) c.surfaceHigh else c.surface)
            .border(1.dp, c.line, RoundedCornerShape(20.dp))
            .clickable(source, indication = null, role = Role.Button, onClickLabel = desc, onClick = onClick)
            .semantics { contentDescription = "$desc: $value" }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            label.uppercase(),
            style = Aapps.type.label.copy(color = c.fgMuted, fontSize = 10.sp, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 10.sp, stepSize = 0.5.sp),
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            value,
            style = Aapps.type.number.copy(color = c.fg, fontSize = 17.sp, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 17.sp, stepSize = 0.5.sp),
        )
    }
}

@Composable
private fun WavePicker(current: Wave, haptics: Haptics?, onWave: (Wave) -> Unit) {
    val c = Aapps.colors
    Row(
        Modifier
            .background(c.surface, CircleShape)
            .border(1.dp, c.line, CircleShape)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Wave.entries.forEach { w ->
            val sel = w == current
            val bg by animateColorAsState(if (sel) c.accent else Color.Transparent, Springs.gentle(), label = "wave")
            val fg by animateColorAsState(if (sel) c.onAccent else c.fgMuted, Springs.gentle(), label = "waveFg")
            val label = stringResource(waveName(w))
            Box(
                Modifier
                    .size(width = 60.dp, height = 50.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .selectable(sel, indication = null, interactionSource = null, role = Role.RadioButton) {
                        if (!sel) {
                            haptics?.tick()
                            onWave(w)
                        }
                    }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                AappsIcon(HertzrIcons.wave(w), null, fg, size = 28.dp)
            }
        }
    }
}

// Pegel: Ziehen verschiebt relativ (kein Sprung durch Tippen), Bereich ueber -10 dB in Bernstein
@Composable
private fun LevelSlider(db: Int, warning: Warning, haptics: Haptics?, onLevel: (Int) -> Boolean) {
    val c = Aapps.colors
    val set by rememberUpdatedState(onLevel)
    val current by rememberUpdatedState(db)
    val loud = warning == Warning.LOUD
    val desc = stringResource(R.string.level)
    val valueText = dbText(db)
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            desc.uppercase(),
            Modifier.widthIn(min = 52.dp),
            style = Aapps.type.label.copy(color = c.fgMuted),
            maxLines = 1,
        )
        Spacer(
            Modifier
                .weight(1f)
                .height(48.dp)
                .semantics {
                    contentDescription = desc
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(db.toFloat(), Level.MIN_DB.toFloat()..Level.MAX_DB.toFloat(), Level.MAX_DB - Level.MIN_DB - 1)
                    setProgress { v -> onLevel(v.roundToInt()); true }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val startDb = current
                        val usable = size.width - 24.dp.toPx()
                        var dx = 0f
                        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { ch, over ->
                            dx += over
                            ch.consume()
                        } ?: return@awaitEachGesture
                        fun follow() {
                            val target = (startDb + dx / usable * (Level.MAX_DB - Level.MIN_DB)).roundToInt().coerceIn(Level.MIN_DB, Level.MAX_DB)
                            if (set(target)) haptics?.tick()
                        }
                        follow()
                        horizontalDrag(slop.id) { ch ->
                            dx += ch.position.x - ch.previousPosition.x
                            follow()
                            ch.consume()
                        }
                    }
                }
                .drawWithCache {
                    val thumbR = 11.dp.toPx()
                    val x0 = thumbR + 1.dp.toPx()
                    val x1 = size.width - thumbR - 1.dp.toPx()
                    val cy = size.height / 2
                    val h = 6.dp.toPx()
                    val corner = CornerRadius(h / 2)
                    val warnX = x0 + (x1 - x0) * Level.toPos(Level.LOUD_DB)
                    val ticks = listOf(-60, -40, -20, -10, 0).map { x0 + (x1 - x0) * Level.toPos(it) }
                    onDrawBehind {
                        val x = x0 + (x1 - x0) * Level.toPos(db)
                        val fill = if (loud) Amber else c.accent
                        drawRoundRect(c.line, Offset(x0, cy - h / 2), Size(x1 - x0, h), corner)
                        drawRoundRect(Amber.copy(alpha = 0.3f), Offset(warnX, cy - h / 2), Size(x1 - warnX, h), corner)
                        drawRoundRect(fill, Offset(x0, cy - h / 2), Size(x - x0, h), corner)
                        ticks.forEach { tx -> drawCircle(c.fgFaint, 1.5.dp.toPx(), Offset(tx, cy + h + 5.dp.toPx())) }
                        drawGlow(fill, Offset(x, cy), thumbR * 2.6f, 0.4f)
                        drawCircle(c.fg, thumbR, Offset(x, cy))
                        drawCircle(fill, thumbR * 0.45f, Offset(x, cy))
                    }
                },
        )
        BasicText(
            valueText,
            Modifier.widthIn(min = 72.dp * fs),
            style = Aapps.type.number.copy(color = if (loud) Amber else c.fg, fontSize = 18.sp, textAlign = TextAlign.End),
            maxLines = 1,
        )
    }
}

// Hinweiszeile mit fester Hoehe: erscheint, ohne etwas zu verschieben
@Composable
private fun WarningLine(w: Warning) {
    val text = when (w) {
        Warning.LOUD -> stringResource(R.string.warn_loud)
        Warning.HEADPHONES -> stringResource(R.string.warn_headphones)
        Warning.NONE -> ""
    }
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    Box(Modifier.fillMaxWidth().height(warnHeight(fs)), contentAlignment = Alignment.Center) {
        if (text.isNotEmpty()) {
            BasicText(
                text,
                style = Aapps.type.body.copy(color = Amber, fontSize = 13.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
            )
        }
    }
}

private fun warnHeight(fs: Float): Dp = 22.dp * fs.coerceAtMost(1.5f)

// "−24 dB" mit echtem Minus
fun dbText(db: Int): String = if (db < 0) "−${-db} dB" else "$db dB"
