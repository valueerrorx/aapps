package solutions.xapient.focusr.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.focusr.R
import solutions.xapient.focusr.domain.Dial
import solutions.xapient.focusr.domain.DialDrag
import solutions.xapient.focusr.domain.Phase
import solutions.xapient.focusr.domain.Scale
import solutions.xapient.focusr.domain.TimeText
import solutions.xapient.focusr.domain.Timer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

// letzte Minute pulsiert
private const val PULSE_MS = 60_000L
private const val PULSE_PERIOD_MS = 1600L

// Lichtring nach dem Ende in Ruhe
private const val DONE_REST = 0.45f

private fun pulseOf(now: Long): Float {
    val t = (now % PULSE_PERIOD_MS) / PULSE_PERIOD_MS.toFloat()
    return 0.5f - 0.5f * cos(2f * PI.toFloat() * t)
}

// Timer: Restzeit oben, Scheibe als Hero, unten Zuruecksetzen und die eine Hauptaktion
@Composable
fun TimerScreen(
    timer: Timer,
    scale: Scale,
    haptics: Haptics?,
    onSet: (Long) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
    // Restzeit -> Uhrzeit des Endes
    endText: (Long) -> String,
    keepOn: Boolean = false,
    onReject: () -> Unit = {},
    clock: () -> Long = SystemClock::elapsedRealtime,
    animate: Boolean = true,
    dragPreview: Long? = null,
    pulseAt: Float = 0f,
) {
    // Haeufig wechselnde Werte als State-Objekte: gelesen nur im Zeichnen bzw. in kleinen Scopes
    val now = remember { mutableLongStateOf(clock()) }
    val drag = remember { mutableStateOf(dragPreview) }
    val pulse = remember { mutableFloatStateOf(pulseAt) }
    // Radius in px fuer das Ticker-Intervall, kommt aus dem Layout
    val radiusPx = remember { floatArrayOf(0f) }

    val phase by remember(timer) { derivedStateOf { timer.phase(now.longValue) } }

    // Ticker: nur waehrend des Laufs, so selten wie moeglich; letzte Minute pro Frame.
    // Jeder Schritt wartet auf einen Frame: im Hintergrund (ab ON_STOP) pausiert die Frame-Uhr und damit der Ticker
    LaunchedEffect(timer, scale, animate) {
        now.longValue = clock()
        if (!animate) return@LaunchedEffect
        while (timer.phase(now.longValue) == Phase.RUNNING) {
            val rem = timer.remaining(now.longValue)
            if (rem > PULSE_MS) delay(min(Dial.tickMs(scale, radiusPx[0]), Dial.msToNextSecond(rem)))
            withFrameMillis { }
            now.longValue = clock()
            if (timer.remaining(now.longValue) <= PULSE_MS) pulse.floatValue = pulseOf(now.longValue)
        }
        pulse.floatValue = 0f
    }

    // Lichtring einmal beim Ende, danach ruhig
    val doneGlow = remember { Animatable(if (timer.phase(clock()) == Phase.DONE) DONE_REST else 0f) }
    LaunchedEffect(phase) {
        when {
            phase != Phase.DONE -> doneGlow.snapTo(0f)
            !animate || doneGlow.value == DONE_REST -> doneGlow.snapTo(DONE_REST)
            else -> {
                doneGlow.snapTo(1f)
                doneGlow.animateTo(DONE_REST, spring(dampingRatio = 1f, stiffness = 30f))
            }
        }
    }

    // Spruenge (Zuruecksetzen, Nochmal, Skala) per Feder; Ziehen springt nie
    val prev = remember { arrayOfNulls<Pair<Timer, Scale>>(1) }
    val committed = remember { booleanArrayOf(false) }
    val jump = remember(timer, scale) {
        val p = prev[0]
        prev[0] = timer to scale
        val n = clock()
        val start = if (p == null || committed[0]) 0f else Dial.fill(p.first.remaining(n), p.second) - Dial.fill(timer.remaining(n), scale)
        committed[0] = false
        Animatable(start)
    }
    LaunchedEffect(jump) {
        if (animate && jump.value != 0f) jump.animateTo(0f, Springs.snappy()) else jump.snapTo(0f)
    }

    if (keepOn && phase == Phase.RUNNING) KeepScreenOn()

    val dragging by remember { derivedStateOf { drag.value != null } }
    val zero = phase == Phase.IDLE && timer.setMs == 0L && !dragging
    val ghost = if ((phase == Phase.RUNNING || phase == Phase.PAUSED) && !dragging) Dial.fill(timer.setMs, scale) else -1f
    val shownMs = remember(timer) { { drag.value ?: timer.remaining(now.longValue) } }
    val endsIn = remember(timer) { timer.remaining(clock()) }

    // einmal angelegt: Gesten und Zeichen-Cache bleiben ueber Rekompositionen bestehen
    val timerState = rememberUpdatedState(timer)
    val scaleState = rememberUpdatedState(scale)
    val ghostState = rememberUpdatedState(ghost)
    val dimState = rememberUpdatedState(phase == Phase.PAUSED)
    val jumpState = rememberUpdatedState(jump)
    val look = remember {
        DialInputs(timerState, scaleState, ghostState, dimState, jumpState, now, drag, pulse, doneGlow, radiusPx)
    }
    val currentOnSet by rememberUpdatedState(onSet)
    val onCommit = remember {
        { ms: Long ->
            committed[0] = true
            currentOnSet(ms)
        }
    }

    AappsScaffold {
        val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
        val bigFont = LocalDensity.current.fontScale > 1.5f
        val canStart = timer.canStart(phase)
        val controls: @Composable () -> Unit = {
            Controls(phase, canStart, haptics, onStart, onPause, onReset, onReject, plainPill = landscape && bigFont)
        }
        val gear: @Composable () -> Unit = {
            AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
        }
        if (landscape) {
            // Scheibe so hoch wie moeglich; links Text und Bedienung, rechts Platz fuer das Zahnrad: Abstaende gleichen sich aus
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.fillMaxHeight().weight(1f).padding(start = 16.dp, end = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Readout(shownMs, endsIn, phase, zero, endText, if (bigFont) 96.dp else 112.dp)
                    Spacer(Modifier.height(20.dp))
                    controls()
                }
                Box(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true).padding(12.dp), contentAlignment = Alignment.Center) {
                    TimerDial(look, haptics, onCommit, Modifier)
                }
                Spacer(Modifier.width(56.dp))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { gear() }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    gear()
                }
                // Ablesewert und Scheibe als Gruppe mittig, kein Loch dazwischen
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Readout(shownMs, endsIn, phase, zero, endText, if (bigFont) 112.dp else 136.dp)
                    Box(Modifier.weight(1f, fill = false).padding(8.dp), contentAlignment = Alignment.Center) {
                        TimerDial(look, haptics, onCommit, Modifier)
                    }
                }
                Box(Modifier.padding(bottom = 20.dp, top = 4.dp)) { controls() }
            }
        }
    }
}

// Eingaben der Scheibe als States, damit Ticks nur neu zeichnen statt neu zu komponieren
class DialInputs(
    val timer: State<Timer>,
    val scale: State<Scale>,
    val ghost: State<Float>,
    val dim: State<Boolean>,
    val jump: State<Animatable<Float, *>>,
    val now: MutableLongState,
    val drag: MutableState<Long?>,
    val pulse: MutableFloatState,
    val done: Animatable<Float, *>,
    val radiusPx: FloatArray,
) {
    fun shownMs(): Long = drag.value ?: timer.value.remaining(now.longValue)

    fun fill(): Float {
        val d = drag.value
        val sc = scale.value
        return if (d != null) Dial.fill(d, sc) else (Dial.fill(timer.value.remaining(now.longValue), sc) + jump.value.value).coerceIn(0f, 1f)
    }
}

// Die Scheibe: Ziehen stellt ein, TalkBack per Regler
@Composable
private fun TimerDial(look: DialInputs, haptics: Haptics?, onCommit: (Long) -> Unit, modifier: Modifier) {
    val scale = look.scale.value
    val colors = Aapps.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = Aapps.type.number
    val dialDesc = stringResource(R.string.dial_desc)
    val density = LocalDensity.current.density

    // Ansage: ganze Minuten, in der letzten Minute Sekunden; aendert sich selten
    val spoken by remember(look) {
        derivedStateOf {
            val s = TimeText.seconds(look.shownMs())
            if (s >= 60) true to (s + 59) / 60 else false to s
        }
    }
    val steps by remember(look, scale) {
        derivedStateOf { ((look.shownMs() + scale.stepMs - 1) / scale.stepMs).coerceAtMost(Scale.STEPS.toLong()) }
    }
    val n = spoken.second.toInt()
    val spokenText = if (spoken.first) pluralStringResource(R.plurals.minutes_n, n, n) else pluralStringResource(R.plurals.seconds_n, n, n)

    val draw = remember(look, scale, colors, measurer, labelStyle) {
        Modifier.drawWithCache {
            val parts = DialParts(this, scale, colors, measurer, labelStyle)
            onDrawBehind {
                drawDial(
                    parts,
                    DialLook(
                        fill = look.fill(),
                        ghost = look.ghost.value,
                        dim = look.dim.value,
                        pulse = look.pulse.floatValue,
                        done = look.done.value,
                        dragging = look.drag.value != null,
                    ),
                )
            }
        }
    }

    Box(
        modifier
            .aspectRatio(1f)
            // Muldenradius fuer das Ticker-Intervall, schon beim Layout bekannt
            .onSizeChanged { look.radiusPx[0] = faceRadius(it.width.toFloat(), it.height.toFloat(), density) }
            .semantics {
                contentDescription = dialDesc
                stateDescription = spokenText
                progressBarRangeInfo = ProgressBarRangeInfo(steps.toFloat(), 0f..Scale.STEPS.toFloat(), Scale.STEPS - 1)
                setProgress { v ->
                    onCommit(Dial.snap(v / Scale.STEPS.toDouble(), look.scale.value))
                    true
                }
            }
            .pointerInput(look) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = min(size.width, size.height) / 2f
                    if ((down.position - c).getDistance() > r) return@awaitEachGesture
                    val sc = look.scale.value
                    val startMs = look.shownMs()
                    // Beim Lauf nur an der Kante greifen
                    val d0 = down.position - c
                    if (look.timer.value.phase(look.now.longValue) == Phase.RUNNING &&
                        !Dial.nearEdge(Dial.fraction(d0.x, d0.y), look.fill())
                    ) {
                        return@awaitEachGesture
                    }
                    // Tippen aendert nichts, erst echtes Ziehen
                    val slop = awaitTouchSlopOrCancellation(down.id) { ch, _ -> ch.consume() } ?: return@awaitEachGesture
                    val drag = DialDrag()
                    var last = if (startMs <= sc.ms) Dial.snap(Dial.fill(startMs, sc).toDouble(), sc) else -1L
                    fun follow(p: Offset) {
                        val d = p - c
                        // nahe der Mitte ist der Winkel unruhig
                        if (d.getDistance() < r * 0.12f) return
                        val ms = Dial.snap(drag.move(Dial.fraction(d.x, d.y)), sc)
                        if (ms != last) {
                            haptics?.tick()
                            last = ms
                        }
                        look.drag.value = ms
                    }
                    follow(slop.position)
                    val id = slop.id
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == id } ?: break
                        if (!ch.pressed) break
                        ch.consume()
                        follow(ch.position)
                    }
                    val result = look.drag.value
                    if (result != null) onCommit(result)
                    look.drag.value = null
                }
            }
            .then(draw),
    )
}

// Restzeit gross, darunter der Zustand
@Composable
private fun Readout(
    shownMs: () -> Long,
    endsIn: Long,
    phase: Phase,
    zero: Boolean,
    endText: (Long) -> String,
    height: Dp,
) {
    val c = Aapps.colors
    val clockText by remember(shownMs) { derivedStateOf { TimeText.clock(shownMs()) } }
    val digitColor = when (phase) {
        Phase.DONE -> c.accent
        Phase.PAUSED -> c.fgMuted
        else -> c.fg
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            clockText,
            Modifier.fillMaxWidth().height(height),
            style = Aapps.type.display.copy(color = digitColor, textAlign = TextAlign.Center, lineHeight = 1.em),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 36.sp, maxFontSize = 124.sp, stepSize = 2.sp),
        )
        Spacer(Modifier.height(4.dp))
        val status = when {
            phase == Phase.DONE -> stringResource(R.string.state_done)
            phase == Phase.RUNNING -> stringResource(R.string.state_ends, endText(endsIn))
            phase == Phase.PAUSED -> stringResource(R.string.state_paused)
            zero -> null
            else -> stringResource(R.string.state_ready)
        }
        // Zustand wird bei Aenderung angesagt
        val live = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
        if (status != null) {
            BasicText(
                status.uppercase(),
                live,
                style = Aapps.type.label.copy(
                    color = if (phase == Phase.DONE) c.accent else c.fgMuted,
                    fontSize = Aapps.type.label.fontSize * 1.2f,
                    textAlign = TextAlign.Center,
                ),
            )
        } else {
            BasicText(
                stringResource(R.string.state_zero),
                live,
                style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun Controls(
    phase: Phase,
    canStart: Boolean,
    haptics: Haptics?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onReject: () -> Unit,
    plainPill: Boolean,
) {
    val side = 56.dp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Box(Modifier.size(side), contentAlignment = Alignment.Center) {
            ResetButton(phase != Phase.IDLE) {
                haptics?.confirm()
                onReset()
            }
        }
        Spacer(Modifier.width(12.dp))
        val (label, icon, active) = when (phase) {
            Phase.IDLE -> Triple(R.string.start, FocusrIcons.Play, canStart)
            Phase.RUNNING -> Triple(R.string.pause, FocusrIcons.Pause, false)
            Phase.PAUSED -> Triple(R.string.resume, FocusrIcons.Play, true)
            Phase.DONE -> Triple(R.string.again, FocusrIcons.Play, true)
        }
        AappsPillButton(
            stringResource(label),
            {
                when {
                    phase == Phase.RUNNING -> {
                        haptics?.press()
                        onPause()
                    }
                    canStart -> {
                        haptics?.press()
                        onStart()
                    }
                    else -> onReject()
                }
            },
            Modifier.widthIn(min = 168.dp),
            icon = if (plainPill) null else icon,
            active = active,
        )
        Spacer(Modifier.width(12.dp))
        Spacer(Modifier.width(side))
    }
}

// Zuruecksetzen erscheint per Feder, sobald nicht Leerlauf
@Composable
private fun ResetButton(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(Springs.gentle()) + scaleIn(Springs.bouncy(), initialScale = 0.6f),
        exit = fadeOut(Springs.gentle()) + scaleOut(Springs.gentle(), targetScale = 0.6f),
    ) {
        AappsIconButton(AappsIcons.Rotate, stringResource(R.string.reset), onClick)
    }
}
