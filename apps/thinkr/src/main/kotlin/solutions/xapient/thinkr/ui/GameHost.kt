package solutions.xapient.thinkr.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.GameDef
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.TonePlayer
import solutions.xapient.thinkr.domain.Combo
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Tones

enum class Phase { COUNTDOWN, PLAYING, PAUSED, OVER }

private const val COUNT_FROM = 3
private const val COUNT_STEP_MS = 650L

// Laengster Frame, der als Spielzeit zaehlt: nach Haengern springt die Uhr nicht
private const val MAX_FRAME_NS = 100_000_000L

// Rahmen jeder Runde: Einzaehlen, Spielzeit nur in PLAYING, Pause, Ende; Haptik und Toene aus dem Rundenstand
@Composable
fun <S : Play, I> GameHost(
    def: GameDef<S, I>,
    seed: Long,
    level: Int,
    relaxed: Boolean,
    haptics: Haptics?,
    tones: TonePlayer?,
    onFinish: (Play) -> Unit,
    onQuit: () -> Unit,
    initial: S? = null,
    initialPhase: Phase = Phase.COUNTDOWN,
    frozen: Boolean = false,
    initialCount: Int = COUNT_FROM,
    safeColors: Boolean = false,
) {
    var state by remember { mutableStateOf(initial ?: def.game.start(seed, level, relaxed, safeColors)) }
    var phase by remember { mutableStateOf(initialPhase) }
    var count by remember { mutableIntStateOf(if (initialPhase == Phase.COUNTDOWN) initialCount else 0) }
    // Spielzeit in ns; steht ausserhalb von PLAYING
    var gameNs by remember { mutableLongStateOf(state.round.now * 1_000_000L) }
    val finish by rememberUpdatedState(onFinish)

    fun update(next: S) {
        val a = state.round
        val b = next.round
        val cheer = next.cheers > state.cheers
        state = next
        if (cheer || b.combo.mult > a.combo.mult || (def.tickOnHit && b.correct > a.correct)) haptics?.tick()
        if (b.correct > a.correct) tones?.play(Tones.Kind.HIT) else if (b.wrong > a.wrong) tones?.play(Tones.Kind.MISS)
        if (b.isOver && phase == Phase.PLAYING) phase = Phase.OVER
    }

    fun pause() {
        if (phase == Phase.PLAYING || phase == Phase.COUNTDOWN) phase = Phase.PAUSED
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { pause() }
    // Zurueck pausiert, in der Pause beendet es; kurz vor dem Ergebnis wird es geschluckt
    BackHandler {
        when (phase) {
            Phase.PAUSED -> onQuit()
            Phase.OVER -> Unit
            else -> pause()
        }
    }

    LaunchedEffect(phase) {
        when (phase) {
            Phase.COUNTDOWN -> if (!frozen) {
                for (n in COUNT_FROM downTo 1) {
                    count = n
                    delay(COUNT_STEP_MS)
                }
                count = 0
                phase = Phase.PLAYING
            }
            Phase.PLAYING -> if (!frozen) {
                var last = -1L
                while (isActive) {
                    withFrameNanos { now ->
                        if (last >= 0) gameNs += (now - last).coerceIn(0, MAX_FRAME_NS)
                        last = now
                        update(def.game.tick(state, gameNs / 1_000_000L))
                    }
                }
            }
            Phase.OVER -> if (!frozen) {
                haptics?.confirm()
                tones?.play(Tones.Kind.END)
                // letzte Karte noch wegfliegen lassen
                delay(500)
                finish(state)
            }
            Phase.PAUSED -> Unit
        }
    }

    val onInput: (I) -> Unit = { i ->
        if (phase == Phase.PLAYING) {
            haptics?.press()
            update(def.game.input(state, i, gameNs / 1_000_000L))
        }
    }

    AappsScaffold {
        Column(Modifier.fillMaxSize()) {
            Hud(state, onPause = { haptics?.press(); pause() })
            TimeBar(state.round.remainingMs.toFloat() / state.round.durationMs)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                def.board(state, phase == Phase.PLAYING, if (phase == Phase.COUNTDOWN) count else 0, onInput)
            }
        }
        if (phase == Phase.PAUSED) {
            PauseOverlay(
                score = state.round.score,
                onResume = { haptics?.press(); phase = Phase.COUNTDOWN },
                onQuit = onQuit,
            )
        }
    }
}

@Composable
private fun Hud(state: Play, onPause: () -> Unit) {
    val c = Aapps.colors
    val r = state.round
    Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp)) {
        AappsIconButton(ThinkrIcons.Pause, stringResource(R.string.pause), onPause, Modifier.align(Alignment.CenterStart))
        val points = formatPoints(r.score)
        val desc = stringResource(R.string.points_n, points)
        BasicText(
            points,
            Modifier.align(Alignment.Center).semantics { contentDescription = desc },
            style = Aapps.type.number.copy(color = c.fg, fontSize = Aapps.type.number.fontSize * 1.5f, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        Multiplier(r.combo, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
    }
}

// Multiplikator mit vier Punkten bis zur naechsten Stufe
@Composable
private fun Multiplier(combo: Combo, modifier: Modifier) {
    val c = Aapps.colors
    val hot = combo.mult > 1
    val desc = stringResource(R.string.multiplier, combo.mult)
    Column(modifier.semantics { contentDescription = desc }, horizontalAlignment = Alignment.End) {
        BasicText(
            "×${combo.mult}",
            style = Aapps.type.number.copy(color = if (hot) c.accent else c.fgMuted, fontSize = Aapps.type.number.fontSize * 1.1f),
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val full = combo.mult >= Combo.MAX
            repeat(Combo.STEP) { i ->
                val on = full || i < combo.run
                val a by animateFloatAsState(if (on) 1f else 0f, Springs.snappy(), label = "pip")
                Box(
                    Modifier
                        .size(5.dp)
                        .background(if (a > 0.5f) c.accent.copy(alpha = 0.4f + 0.6f * a) else c.line, CircleShape),
                )
            }
        }
    }
}

// Duenne Zeitleiste in Akzent, die letzten 10 s in Bernstein
@Composable
private fun TimeBar(fraction: Float) {
    val c = Aapps.colors
    val f = fraction.coerceIn(0f, 1f)
    val color = if (f < 1f / 6f) Amber else c.accent
    Canvas(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 20.dp)) {
        val y = size.height / 2
        drawLine(c.line, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), StrokeCap.Round)
        if (f > 0f) {
            val end = size.width * f
            drawGlow(color, Offset(end, y), 14.dp.toPx(), 0.4f)
            drawLine(color, Offset(0f, y), Offset(end, y), 3.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun PauseOverlay(score: Int, onResume: () -> Unit, onQuit: () -> Unit) {
    val c = Aapps.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg.copy(alpha = 0.96f))
            // Tipps gehen nicht ans Spiel darunter
            .clickable(interactionSource = null, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val title = stringResource(R.string.pause)
            BasicText(
                title.uppercase(),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = Aapps.type.label.copy(color = c.fgMuted),
            )
            Spacer(Modifier.height(10.dp))
            BasicText(formatPoints(score), style = Aapps.type.headline.copy(color = c.fg, fontSize = Aapps.type.headline.fontSize * 1.3f))
            Spacer(Modifier.height(32.dp))
            AappsPillButton(stringResource(R.string.resume), onResume, Modifier.widthIn(min = 176.dp), icon = ThinkrIcons.Play, active = true)
            Spacer(Modifier.height(8.dp))
            AappsTextButton(stringResource(R.string.quit), onQuit)
        }
    }
}
