package solutions.xapient.hertzr.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.hertzr.R
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Oscillator
import solutions.xapient.hertzr.domain.Wave
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Bogen: 270 Grad, Luecke unten; 0 Grad = 3 Uhr, im Uhrzeigersinn
private const val ARC_START = 135f
private const val ARC_SWEEP = 270f
private val DECADES = listOf(1.0 to "1", 10.0 to "10", 100.0 to "100", 1_000.0 to "1k", 10_000.0 to "10k", 20_000.0 to "20k")

// Hero: Log-Skala 1 Hz..22 kHz, Fuellung bis zur Frequenz, Notenname, grosse Zahl, Oszilloskop.
// hz und frame werden nur in der Zeichenphase gelesen (Sweep, laufende Welle).
@Composable
fun FreqDial(
    hz: () -> Double,
    // Grundlage fuer das Drehen (eingestellte Frequenz)
    baseHz: Double,
    maxHz: Double,
    wave: Wave,
    playing: Boolean,
    frame: () -> Long,
    // Sweep-Bereich auf dem Bogen, sonst null
    band: Pair<Double, Double>?,
    haptics: Haptics?,
    // null = Drehen aus (Sweep); true = Frequenz hat sich geaendert
    onHz: ((Double) -> Boolean)?,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = Aapps.colors
    val view = LocalView.current
    val set by rememberUpdatedState(onHz)
    val base by rememberUpdatedState(baseHz)
    val top by rememberUpdatedState(maxHz)
    val notes = stringArrayResource(R.array.note_names)
    val dec = stringResource(R.string.decimal_sep).first()
    val unit = stringResource(R.string.hz)
    val desc = stringResource(R.string.frequency)
    val shownHz = if (band != null && !playing) band.first else baseHz
    val state = noteState(shownHz, notes, dec)
    val measurer = rememberTextMeasurer()
    val type = Aapps.type
    val fs = LocalDensity.current.fontScale

    // Wellenwechsel morpht im Bild; danach keine Frames mehr
    var shown by remember { mutableStateOf(wave) }
    var prev by remember { mutableStateOf(wave) }
    val morph = remember { Animatable(1f) }
    LaunchedEffect(wave) {
        if (wave != shown) {
            prev = shown
            shown = wave
            morph.snapTo(0f)
            morph.animateTo(1f, Springs.gentle())
        }
    }

    BoxWithConstraints(
        modifier
            .semantics {
                contentDescription = desc
                stateDescription = state
                if (onHz != null) {
                    progressBarRangeInfo = ProgressBarRangeInfo(Freq.toPos(baseHz), 0f..1f)
                    setProgress { v -> onHz(Freq.quantize(Freq.fromPos(v))); true }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (set == null) return@awaitEachGesture
                    val g = ring(Size(size.width.toFloat(), size.height.toFloat()))
                    val cx = g.center.x
                    val cy = g.center.y
                    // nahe der Mitte ist der Winkel unruhig
                    val inner = min(size.width, size.height) * 0.1f
                    fun angle(p: Offset) = Math.toDegrees(atan2((p.y - cy).toDouble(), (p.x - cx).toDouble()))
                    var last = angle(down.position)
                    val start = base
                    var total = 0.0
                    var semi = Freq.semitone(start)
                    var atLimit = false
                    val slop = awaitTouchSlopOrCancellation(down.id) { ch, _ -> ch.consume() } ?: return@awaitEachGesture
                    fun follow(p: Offset) {
                        val a = angle(p)
                        if (hypot(p.x - cx, p.y - cy) > inner) {
                            var d = (a - last) % 360.0
                            if (d > 180) d -= 360.0
                            if (d < -180) d += 360.0
                            total += d
                            val raw = start * Math.pow(2.0, total / 360.0)
                            val target = Freq.rotate(start, total, top)
                            val changed = set?.invoke(target) ?: false
                            val s = Freq.semitone(target)
                            if (changed) {
                                atLimit = false
                                if (s != semi) haptics?.tick()
                            } else if (raw < Freq.MIN || raw > top) {
                                if (!atLimit && haptics != null) {
                                    view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
                                }
                                atLimit = true
                                // nicht ueber den Rand hinaus aufsummieren
                                total -= d
                            }
                            semi = s
                        }
                        last = a
                    }
                    follow(slop.position)
                    drag(slop.id) { ch ->
                        follow(ch.position)
                        ch.consume()
                    }
                }
            }
            .drawWithCache {
                val g = ring(size)
                val r = g.r
                val mid = g.center
                val stroke = (r * 0.06f).coerceIn(7.dp.toPx(), 13.dp.toPx())
                val arcTopLeft = Offset(mid.x - r, mid.y - r)
                val arcSize = Size(2 * r, 2 * r)
                fun at(pos: Float, rad: Float): Offset {
                    val a = Math.toRadians((ARC_START + ARC_SWEEP * pos).toDouble())
                    return Offset(mid.x + rad * cos(a).toFloat(), mid.y + rad * sin(a).toFloat())
                }
                // Skala aussen: Dekaden lang, 2..9 kurz
                val r0 = r + stroke / 2 + 6.dp.toPx()
                val major = Path()
                val minor = Path()
                var d = 1.0
                while (d <= 10_000.0) {
                    for (k in 1..9) {
                        val f = d * k
                        if (f > Freq.MAX) break
                        val p = Freq.toPos(f)
                        val len = if (k == 1) 9.dp.toPx() else 4.dp.toPx()
                        val path = if (k == 1) major else minor
                        val a = at(p, r0)
                        val b = at(p, r0 + len)
                        path.moveTo(a.x, a.y)
                        path.lineTo(b.x, b.y)
                    }
                    d *= 10
                }
                val labelStyle = type.number.copy(fontSize = min(11.sp.toPx() * fs.coerceAtMost(1.5f), r * 0.085f).toSp())
                val labels = DECADES.map { (f, t) ->
                    val l = measurer.measure(t, labelStyle)
                    val p = at(Freq.toPos(f), r0 + 13.dp.toPx() + l.size.width * 0.35f + l.size.height * 0.3f)
                    l to Offset(p.x - l.size.width / 2f, p.y - l.size.height / 2f)
                }
                // Zahl: feste Groesse fuer den breitesten Wert, damit nichts springt
                val probe = measurer.measure("00000", type.display.copy(fontSize = 100.sp))
                val numScale = min(r * 1.3f / probe.size.width, r * 0.42f / probe.size.height)
                val numStyle = type.display.copy(color = c.fg, fontSize = (100.sp.toPx() * numScale).toSp())
                val smallStyle = type.label.copy(fontSize = min(type.label.fontSize.toPx() * 1.15f * fs, r * 0.1f).toSp())
                val unitLayout = measurer.measure(unit, smallStyle.copy(color = c.fgMuted, letterSpacing = 0.06.sp))
                // Oszilloskop-Streifen unten im Kreis
                val scopeW = r * 1.2f
                val scopeTop = mid.y + r * 0.36f
                val scopeH = g.scopeH
                val scopeMid = scopeTop + scopeH / 2
                val wavePath = Path()
                val fade = Brush.horizontalGradient(
                    0f to c.accent.copy(alpha = 0f), 0.18f to c.accent, 0.82f to c.accent, 1f to c.accent.copy(alpha = 0f),
                    startX = mid.x - scopeW / 2, endX = mid.x + scopeW / 2,
                )
                val baseLine = Brush.horizontalGradient(
                    0f to c.line.copy(alpha = 0f), 0.5f to c.line, 1f to c.line.copy(alpha = 0f),
                    startX = mid.x - scopeW / 2, endX = mid.x + scopeW / 2,
                )
                val far = hypot(size.width, size.height) / 2
                val waveStroke = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val waveGlow = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    val f = hz()
                    val pos = Freq.toPos(f)
                    drawGlow(c.accent, mid, far, if (playing) GLOW_PLAY else GLOW_IDLE)
                    // Skala und Spur
                    drawPath(minor, c.fgFaint, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(major, c.fgMuted, style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
                    labels.forEach { (l, o) -> drawText(l, c.fgMuted, o) }
                    drawArc(c.line, ARC_START, ARC_SWEEP, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (band != null) {
                        // Sweep-Bereich hell, gelaufener Teil voll
                        val a = Freq.toPos(band.first)
                        val b = Freq.toPos(band.second)
                        val lo = min(a, b)
                        drawArc(c.accent.copy(alpha = 0.32f), ARC_START + ARC_SWEEP * lo, ARC_SWEEP * kotlin.math.abs(b - a), false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        if (playing) {
                            val from = min(a, pos)
                            drawArc(c.accent, ARC_START + ARC_SWEEP * from, ARC_SWEEP * kotlin.math.abs(pos - a), false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                    } else {
                        drawArc(c.accent.copy(alpha = 0.14f), ARC_START, ARC_SWEEP * pos, false, arcTopLeft, arcSize, style = Stroke(stroke * 2.4f, cap = StrokeCap.Round))
                        drawArc(c.accent, ARC_START, ARC_SWEEP * pos, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    // Marke
                    val m = at(pos, r)
                    drawGlow(c.accent, m, stroke * 3.2f, if (playing) 0.7f else 0.45f)
                    drawCircle(c.accent, stroke * 0.95f, m)
                    drawCircle(c.fg, stroke * 0.5f, m)

                    // Notenname, Zahl, Einheit
                    val nl = measurer.measure(noteLabel(f, notes), smallStyle.copy(color = c.fgMuted))
                    drawText(nl, topLeft = Offset(mid.x - nl.size.width / 2f, mid.y - r * 0.5f - nl.size.height / 2f))
                    val num = measurer.measure(Freq.format(f, dec), numStyle)
                    val numTop = mid.y - r * 0.08f - num.size.height / 2f
                    drawText(num, topLeft = Offset(mid.x - num.size.width / 2f, numTop))
                    drawText(unitLayout, topLeft = Offset(mid.x - unitLayout.size.width / 2f, numTop + num.size.height * 0.94f))

                    // Oszilloskop: Zahl der Perioden waechst mit der Frequenz, laeuft beim Spielen
                    drawLine(baseLine, Offset(mid.x - scopeW / 2, scopeMid), Offset(mid.x + scopeW / 2, scopeMid), 1.dp.toPx())
                    val cycles = 1.25 + 3.5 * pos
                    val shift = if (playing) frame() / 1e9 * 0.6 else 0.0
                    val k = morph.value.toDouble()
                    // was wirklich klingt: hohe Toene werden wie im Oszillator zum Sinus
                    val blend = Oscillator.sineBlend(f / SCOPE_RATE)
                    fun shape(w: Wave, t: Double): Double {
                        val raw = Oscillator.naive(w, t)
                        return if (blend <= 0.0 || w == Wave.SINE) raw else (1 - blend) * raw + blend * Oscillator.fundamental(w) * kotlin.math.sin(2 * Math.PI * t)
                    }
                    wavePath.rewind()
                    val n = 160
                    for (i in 0..n) {
                        val x = i / n.toDouble()
                        var t = (x * cycles - shift) % 1.0
                        if (t < 0) t += 1.0
                        val v = if (k >= 1.0) shape(shown, t) else k * shape(shown, t) + (1 - k) * shape(prev, t)
                        val px = mid.x - scopeW / 2 + scopeW * x.toFloat()
                        val py = scopeMid - (scopeH / 2) * 0.86f * v.toFloat()
                        if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                    }
                    val alpha = if (playing) 1f else WAVE_IDLE_ALPHA
                    drawPath(wavePath, fade, alpha = 0.16f * alpha, style = waveGlow)
                    drawPath(wavePath, fade, alpha = alpha, style = waveStroke)
                }
            },
        contentAlignment = Alignment.TopStart,
    ) {
        // Tippen auf die Zahl: direkte Eingabe
        if (onEdit != null) {
            val density = LocalDensity.current
            val g = with(density) { ring(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())) }
            val edit = stringResource(R.string.enter_frequency)
            Box(
                Modifier
                    .offset { IntOffset((g.center.x - g.r * 0.65f).roundToInt(), (g.center.y - g.r * 0.5f).roundToInt()) }
                    .size(with(density) { (g.r * 1.3f).toDp() }, with(density) { (g.r * 0.8f).toDp() })
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = edit) {
                        haptics?.press()
                        onEdit()
                    }
                    .semantics { contentDescription = edit },
            )
        }
    }
}

// "A4", "A4 +12 ct", unter C-1 ein Strich
private fun noteLabel(hz: Double, names: Array<String>): String {
    val n = Freq.note(hz) ?: return "—"
    val name = names[n.pitchClass] + n.octave
    return when {
        n.cents == 0 -> name
        n.cents > 0 -> "$name  +${n.cents} ct"
        else -> "$name  −${-n.cents} ct"
    }
}

private fun noteState(hz: Double, names: Array<String>, dec: Char): String = "${Freq.short(hz, dec)}, ${noteLabel(hz, names)}"

// Oszilloskop nimmt die uebliche Rate an; nur fuer das Bild
private const val SCOPE_RATE = 48_000.0

internal class Ring(val r: Float, val center: Offset, val scopeH: Float)

// Bogen offen nach unten: oben Platz fuer Skala (40 dp), unten bis 0,71 r plus Beschriftung (34 dp).
// Ueberschuessige Hoehe bekommt das Oszilloskop in der Bogenluecke, der Rest wird oben/unten gleich verteilt.
internal fun Density.ring(size: Size): Ring {
    val top = 40.dp.toPx()
    val bottom = 34.dp.toPx()
    val r = min(size.width / 2 - top, (size.height - top - bottom) / 1.71f).coerceAtLeast(40.dp.toPx())
    val extra = (size.height - top - r * 1.71f - bottom).coerceAtLeast(0f)
    val scopeH = (r * 0.3f + extra * 0.6f).coerceAtMost(r * 0.8f)
    val below = max(r * 0.71f + bottom, r * 0.36f + scopeH + 16.dp.toPx())
    val slack = ((size.height - top - r - below) / 2).coerceAtLeast(0f)
    return Ring(r, Offset(size.width / 2, top + r + slack), scopeH)
}
