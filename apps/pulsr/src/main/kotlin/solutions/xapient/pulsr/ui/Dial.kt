package solutions.xapient.pulsr.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.pulsr.R
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.domain.Tempo
import solutions.xapient.pulsr.domain.Wheel
import solutions.xapient.pulsr.domain.WheelSteps
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Aufblitzen nach einem Schlag: voll am Anschlag, klingt in ~ einem halben Schlag ab
fun flash(sinceMs: Float, beatMs: Float): Float = exp(-sinceMs / min(beatMs * 0.4f, 220f).coerceAtLeast(30f))

private const val WAVE_MS = 900f
private const val SCALE_TICKS = 120

// Hero: Rad mit Skala, Schlag-Segmenten, Komet und grosser BPM-Zahl. Puls nur in der Zeichenphase lesen.
@Composable
fun Dial(
    bpm: Int,
    beats: Int,
    sub: Int,
    accent: Boolean,
    playing: Boolean,
    pulse: () -> Pulse?,
    onRotate: (Int) -> Unit,
    onSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Aapps.colors
    val rotate by rememberUpdatedState(onRotate)
    val marking = Tempo.marking(bpm)
    val desc = stringResource(R.string.tempo)
    val state = stringResource(R.string.tempo_state, bpm, marking)
    val idle = idleSegment(c)
    val rest = restSegment(c)
    BoxWithConstraints(
        modifier
            .semantics {
                contentDescription = desc
                stateDescription = state
                progressBarRangeInfo = ProgressBarRangeInfo(bpm.toFloat(), Tempo.MIN.toFloat()..Tempo.MAX.toFloat(), Tempo.MAX - Tempo.MIN - 1)
                setProgress { v -> onSet(v.roundToInt()); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // nahe der Mitte ist der Winkel unruhig
                    val inner = min(size.width, size.height) * 0.08f
                    var last = Wheel.angle(down.position.x, down.position.y, cx, cy)
                    val steps = WheelSteps()
                    val start = awaitTouchSlopOrCancellation(down.id) { ch, _ -> ch.consume() } ?: return@awaitEachGesture
                    fun follow(p: Offset) {
                        val a = Wheel.angle(p.x, p.y, cx, cy)
                        if (hypot(p.x - cx, p.y - cy) > inner) {
                            val n = steps.add(Wheel.delta(last, a))
                            if (n != 0) rotate(n)
                        }
                        last = a
                    }
                    follow(start.position)
                    drag(start.id) { ch ->
                        follow(ch.position)
                        ch.consume()
                    }
                }
            }
            .drawWithCache {
                val r = ringRadius(size)
                val marked = accent && beats > 1
                val stroke = (r * 0.07f).coerceIn(8.dp.toPx(), 16.dp.toPx())
                val seg = 360f / beats
                // Luecke zwischen den Segmenten inkl. runder Enden
                val cap = Math.toDegrees((stroke / 2 / r).toDouble()).toFloat()
                val gap = (seg * 0.18f).coerceAtMost(9f) + 2 * cap
                val mid = Offset(size.width / 2, size.height / 2)
                val ticks = Path().apply {
                    val r0 = r + stroke / 2 + 10.dp.toPx()
                    for (i in 0 until SCALE_TICKS) {
                        val a = i * 2 * PI / SCALE_TICKS
                        val len = if (i % 10 == 0) 10.dp.toPx() else if (i % 5 == 0) 6.dp.toPx() else 3.5.dp.toPx()
                        moveTo(mid.x + r0 * cos(a).toFloat(), mid.y + r0 * sin(a).toFloat())
                        lineTo(mid.x + (r0 + len) * cos(a).toFloat(), mid.y + (r0 + len) * sin(a).toFloat())
                    }
                }
                val trailDeg = (seg * 0.8f).coerceAtMost(64f)
                val trail = Brush.sweepGradient(
                    0f to c.accent.copy(alpha = 0f),
                    trailDeg / 360f to c.accent.copy(alpha = 0.85f),
                    1f to c.accent.copy(alpha = 0f),
                    center = mid,
                )
                val subR = r - stroke / 2 - 9.dp.toPx()
                val far = hypot(size.width, size.height) / 2
                val tickStroke = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round)
                onDrawBehind {
                    val p = if (playing) pulse() else null
                    val f = p?.let { flash(it.sinceBeatMs, it.beatMs) } ?: 0f
                    val strength = if (p?.accent == true) 1f else 0.6f
                    // Lichthof, pulst mit dem Schlag
                    drawGlow(c.accent, center, far * 1.05f, GLOW_BASE + GLOW_FLASH * f * strength)
                    // Ring-Welle beim betonten Schlag
                    if (p != null && p.accent && p.sinceBeatMs < WAVE_MS) {
                        val t = p.sinceBeatMs / WAVE_MS
                        val wr = r + (far - r) * (1 - (1 - t) * (1 - t))
                        drawCircle(c.accent.copy(alpha = 0.5f * (1 - t) * (1 - t)), wr, center, style = Stroke((3.dp.toPx() * (1 - t)).coerceAtLeast(1f)))
                    }
                    // Skala dreht mit dem Tempo, 6 Grad je BPM
                    rotate(bpm * Wheel.DEG_PER_BPM) { drawPath(ticks, c.fgFaint, style = tickStroke) }
                    drawCircle(c.line, r, center, style = Stroke(1.dp.toPx()))
                    // gestoppt ruhig; beim Spielen treten die ruhenden Schlaege zurueck, der laufende leuchtet voll
                    val base = if (p != null) rest else idle
                    for (i in 0 until beats) {
                        val current = p != null && p.beat == i
                        val lit = if (current) litLevel(f) else 0f
                        val col = lerp(base, c.accent, lit)
                        val start = -90f + i * seg + gap / 2
                        val sweep = (seg - gap).coerceAtLeast(0.5f)
                        val w = if (i == 0 && marked) stroke * 1.25f else stroke
                        if (lit > 0f) {
                            // weicher Schein in zwei Lagen statt einem harten Band
                            drawArc(c.accent.copy(alpha = 0.07f * lit), start, sweep, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(w * 3.2f, cap = StrokeCap.Round))
                            drawArc(c.accent.copy(alpha = 0.12f * lit), start, sweep, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(w * 2f, cap = StrokeCap.Round))
                        }
                        drawArc(col, start, sweep, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(w, cap = StrokeCap.Round))
                        if (current && f > 0.05f) {
                            // heller Kern beim Anschlag
                            drawArc(Color.White.copy(alpha = 0.35f * f), start, sweep, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(w * 0.3f, cap = StrokeCap.Round))
                        }
                    }
                    // betonte Eins: Punkt innen am Ringanfang, wie unter der 1 im Zaehler
                    if (marked) drawCircle(c.accent, 3.dp.toPx(), Offset(center.x, center.y - subR))
                    // Unterteilungen als Punkte innen am Ring
                    if (sub > 1) {
                        for (i in 0 until beats) for (j in 1 until sub) {
                            val a = Math.toRadians((-90f + (i + j.toFloat() / sub) * seg).toDouble())
                            val on = p != null && p.beat == i && p.subIndex == j
                            val sf = if (on) flash(p.sinceSubMs, p.beatMs / sub) else 0f
                            val pos = Offset(center.x + subR * cos(a).toFloat(), center.y + subR * sin(a).toFloat())
                            if (sf > 0.02f) drawGlow(c.accent, pos, 12.dp.toPx(), 0.6f * sf)
                            drawCircle(lerp(c.fgMuted, c.accent, sf), 2.6.dp.toPx() * (1 + 0.5f * sf), pos)
                        }
                    }
                    // Komet: laeuft im Takt um den Ring, Schweif leuchtet nach
                    if (p != null) {
                        val head = -90f + p.barPhase * 360f
                        rotate(head - trailDeg, center) {
                            drawArc(trail, 0f, trailDeg, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(stroke * 0.45f, cap = StrokeCap.Butt))
                        }
                        val a = Math.toRadians(head.toDouble())
                        val hp = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
                        drawGlow(c.accent, hp, stroke * 2.4f, 0.55f)
                        drawCircle(c.fg, stroke * 0.42f, hp)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val r = with(density) { ringRadius(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())).toDp() }
        // Zahl atmet mit dem Schlag; nur Ebenen-Eigenschaft, keine Neukomposition
        Column(
            Modifier
                .size(r * 1.36f)
                .graphicsLayer {
                    val p = if (playing) pulse() else null
                    val s = 1f + 0.035f * (p?.let { flash(it.sinceBeatMs, it.beatMs) } ?: 0f)
                    scaleX = s
                    scaleY = s
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                marking.uppercase(),
                style = Aapps.type.label.copy(color = c.fgMuted, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = Aapps.type.label.fontSize * 1.1f, stepSize = 0.5.sp),
            )
            BasicText(
                bpm.toString(),
                // Zahl nimmt den Rest: Beschriftungen bleiben auch bei 200 % Schrift sichtbar
                Modifier.fillMaxWidth().weight(1f, fill = false),
                style = Aapps.type.display.copy(color = c.fg, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 150.sp, stepSize = 2.sp),
            )
            BasicText(
                stringResource(R.string.bpm),
                style = Aapps.type.label.copy(color = c.fgMuted, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = Aapps.type.label.fontSize * 1.1f, stepSize = 0.5.sp),
            )
        }
    }
}

// Ring mit Platz fuer Skala und Kometen-Schein
private fun Density.ringRadius(size: Size): Float = (min(size.width, size.height) / 2 - 30.dp.toPx()).coerceAtLeast(40.dp.toPx())
