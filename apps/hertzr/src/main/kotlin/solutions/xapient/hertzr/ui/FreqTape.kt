package solutions.xapient.hertzr.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.hertzr.R
import solutions.xapient.hertzr.domain.Freq
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow

private val BLACK_KEYS = setOf(1, 3, 6, 8, 10)
private const val STILL_MS = 40L

// Log-Frequenzband: feste Mittel-Nadel, das Band laeuft. Wischen grob (Breite = 2 Oktaven), Schwung laeuft aus,
// oben Dekaden-Skala, unten Klaviatur. Im Sweep getoent von-bis, laeuft live mit.
@Composable
fun FreqTape(
    hz: () -> Double,
    // eingestellte Frequenz (Grundlage fuers Ziehen, Neuzeichnen)
    baseHz: Double,
    maxHz: Double,
    playing: Boolean,
    frame: () -> Long,
    // Sweep-Bereich, sonst null
    band: Pair<Double, Double>?,
    haptics: Haptics?,
    // null = Ziehen aus (Sweep); true = Frequenz hat sich geaendert
    onHz: ((Double) -> Boolean)?,
    modifier: Modifier = Modifier,
) {
    val c = Aapps.colors
    val view = LocalView.current
    val set by rememberUpdatedState(onHz)
    val base by rememberUpdatedState(baseHz)
    val top by rememberUpdatedState(maxHz)
    val notes = stringArrayResource(R.array.note_names)
    val dec = stringResource(R.string.decimal_sep).first()
    val desc = stringResource(R.string.frequency)
    val a11y = listOf(
        stringResource(R.string.higher) to { h: Double -> Freq.nudge(h, 1, maxHz) },
        stringResource(R.string.lower) to { h: Double -> Freq.nudge(h, -1, maxHz) },
        stringResource(R.string.semitone_up) to { h: Double -> Freq.shift(h, 1.0 / 12, maxHz) },
        stringResource(R.string.semitone_down) to { h: Double -> Freq.shift(h, -1.0 / 12, maxHz) },
        stringResource(R.string.octave_up) to { h: Double -> Freq.octave(h, true, maxHz) },
        stringResource(R.string.octave_down) to { h: Double -> Freq.octave(h, false, maxHz) },
    )
    val measurer = rememberTextMeasurer()
    val type = Aapps.type
    val fs = LocalDensity.current.fontScale
    val fling = remember { arrayOfNulls<Job>(1) }

    Spacer(
        modifier
            .semantics {
                contentDescription = desc
                stateDescription = noteState(if (band != null && !playing) band.first else baseHz, notes, dec)
                if (onHz != null) customActions = a11y.map { (label, f) -> CustomAccessibilityAction(label) { onHz(f(baseHz)) } }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        // Beruehren haelt den Schwung an
                        fling[0]?.cancel()
                        if (set == null) return@awaitEachGesture
                        val width = size.width.toFloat()
                        val tracker = VelocityTracker()
                        tracker.addPointerInputChange(down)
                        var oct = log2(base)
                        var semi = Freq.semitone(base)
                        var atLimit = false
                        // false = am Rand: Schwung endet
                        fun move(dx: Float): Boolean {
                            val lo = log2(Freq.MIN)
                            val hi = log2(top)
                            val want = oct + Freq.tapeOctaves(dx, width)
                            oct = want.coerceIn(lo, hi)
                            val target = Freq.quantize(2.0.pow(oct))
                            if (set?.invoke(target) == true) {
                                val s = Freq.semitone(target)
                                if (s != semi) haptics?.step()
                                semi = s
                            }
                            if (want != oct) {
                                if (!atLimit && haptics != null) {
                                    view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
                                }
                                atLimit = true
                                return false
                            }
                            atLimit = false
                            return true
                        }
                        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { ch, _ -> ch.consume() } ?: return@awaitEachGesture
                        tracker.addPointerInputChange(slop)
                        // die Beruehrungsschwelle zaehlt mit, sonst bleibt das Band hinter dem Finger
                        move(slop.position.x - down.position.x)
                        var lastMove = slop.uptimeMillis
                        horizontalDrag(slop.id) { ch ->
                            tracker.addPointerInputChange(ch)
                            val dx = ch.position.x - ch.previousPosition.x
                            if (dx != 0f) lastMove = ch.uptimeMillis
                            move(dx)
                            ch.consume()
                        }
                        // Finger stand vor dem Loslassen still: kein Schwung
                        val upAt = currentEvent.changes.firstOrNull()?.uptimeMillis ?: lastMove
                        val v = if (upAt - lastMove > STILL_MS) 0f else tracker.calculateVelocity().x
                        if (kotlin.math.abs(v) > 50f) {
                            fling[0] = launch {
                                var last = 0f
                                AnimationState(0f, v).animateDecay(exponentialDecay(frictionMultiplier = 1.6f)) {
                                    if (!move(value - last)) cancelAnimation()
                                    last = value
                                }
                            }
                        }
                    }
                }
            }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val corner = CornerRadius(16.dp.toPx())
                val pxPerOct = w / Freq.TAPE_OCTAVES.toFloat()
                val labelStyle = type.number.copy(color = c.fgMuted, fontSize = min(11.sp.toPx() * fs.coerceAtMost(1.4f), h * 0.17f).toSp())
                val keyStyle = type.label.copy(color = c.fg, fontSize = min(9.sp.toPx() * fs.coerceAtMost(1.3f), h * 0.12f).toSp())
                val keysH = h * 0.32f
                val keysTop = h - keysH
                val tickBase = keysTop - 4.dp.toPx()
                val edge = Brush.horizontalGradient(
                    0f to c.surface, 0.12f to c.surface.copy(alpha = 0f), 0.88f to c.surface.copy(alpha = 0f), 1f to c.surface,
                )
                val ticks = Path()
                val minor = Path()
                // Beschriftungen einmal messen, nicht je Bild
                val labels = HashMap<String, TextLayoutResult>()
                fun label(t: String, style: TextStyle) = labels.getOrPut(t) { measurer.measure(t, style) }
                val cx = w / 2
                val tri = Path().apply {
                    moveTo(cx - 6.dp.toPx(), 0f); lineTo(cx + 6.dp.toPx(), 0f); lineTo(cx, 7.dp.toPx()); close()
                }
                onDrawBehind {
                    val f = if (playing && band != null) hz().also { frame() } else if (band != null) band.first else baseHz
                    fun x(fr: Double) = cx + (log2(fr / f) * pxPerOct).toFloat()
                    drawRoundRect(c.surface, Offset.Zero, size, corner)
                    clipRect {
                        // ausserhalb des Bereichs abgedunkelt
                        val xMin = x(Freq.MIN)
                        val xMax = x(maxHz)
                        if (xMin > 0) drawRect(c.bg.copy(alpha = 0.7f), Offset.Zero, Size(xMin, h))
                        if (xMax < w) drawRect(c.bg.copy(alpha = 0.7f), Offset(xMax, 0f), Size(w - xMax, h))
                        // Sweep-Bereich getoent
                        if (band != null) {
                            val a = x(band.first)
                            val b = x(band.second)
                            drawRect(c.accent.copy(alpha = BAND_ALPHA), Offset(min(a, b), 0f), Size(kotlin.math.abs(b - a), keysTop))
                            drawLine(c.accent.copy(alpha = 0.7f), Offset(a, 0f), Offset(a, keysTop), 1.5.dp.toPx())
                            drawLine(c.accent.copy(alpha = 0.7f), Offset(b, 0f), Offset(b, keysTop), 1.5.dp.toPx())
                        }
                        // Skala: 1..9 je Dekade mit Zahl, dazwischen Feinstriche
                        ticks.rewind()
                        minor.rewind()
                        val visLo = f * 2.0.pow(-Freq.TAPE_OCTAVES / 2 - 0.3)
                        val visHi = f * 2.0.pow(Freq.TAPE_OCTAVES / 2 + 0.3)
                        var d = 10.0.pow(floor(kotlin.math.log10(visLo)))
                        while (d <= visHi) {
                            for (k in 1..9) {
                                val fr = d * k
                                if (fr > visHi) break
                                if (fr < Freq.MIN || fr > maxHz) continue
                                val tx = x(fr)
                                val len = if (k == 1) 16.dp.toPx() else 10.dp.toPx()
                                ticks.moveTo(tx, tickBase); ticks.lineTo(tx, tickBase - len)
                                val lt = label(decadeLabel(fr), labelStyle)
                                drawText(lt, topLeft = Offset(tx - lt.size.width / 2f, tickBase - len - 3.dp.toPx() - lt.size.height))
                                // Feinteilung: unter 2 in Zehnteln, unter 5 in Fuenfteln, sonst halbiert
                                val n = if (k < 2) 10 else if (k < 5) 5 else 2
                                for (j in 1 until n) {
                                    val ff = d * (k + j / n.toDouble())
                                    if (ff > maxHz) break
                                    val mx = x(ff)
                                    minor.moveTo(mx, tickBase); minor.lineTo(mx, tickBase - 5.dp.toPx())
                                }
                            }
                            d *= 10
                        }
                        drawPath(minor, c.fgFaint, style = Stroke(1.dp.toPx()))
                        drawPath(ticks, c.fgMuted, style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
                        // Klaviatur: jeder Halbton gleich breit (log), C mit Oktavzahl
                        val pxSemi = pxPerOct / 12f
                        val m0 = ceil(69 + 12 * log2(visLo / Freq.A4)).toInt()
                        val m1 = floor(69 + 12 * log2(visHi / Freq.A4)).toInt()
                        val current = Freq.semitone(f) + 69
                        for (m in m0..m1) {
                            val fr = Freq.A4 * 2.0.pow((m - 69) / 12.0)
                            if (fr < Freq.MIN || fr > maxHz) continue
                            val kx = x(fr) - pxSemi / 2
                            val pc = Math.floorMod(m, 12)
                            val black = pc in BLACK_KEYS
                            // weiss durchgehend, schwarz als kurze Taste oben drauf
                            drawRect(KeyWhite, Offset(kx + 0.5f, keysTop), Size(pxSemi - 1f, keysH))
                            if (black) drawRect(ScopeBg, Offset(kx + 0.5f, keysTop), Size(pxSemi - 1f, keysH * 0.62f))
                            if (m == current) drawRect(c.accent.copy(alpha = 0.55f), Offset(kx + 0.5f, keysTop), Size(pxSemi - 1f, keysH))
                        }
                        // C mit Oktavzahl, nach den Tasten, damit die Nachbartaste nichts abdeckt
                        for (m in m0..m1) {
                            if (Math.floorMod(m, 12) != 0 || Freq.A4 * 2.0.pow((m - 69) / 12.0) < Freq.MIN) continue
                            val kx = x(Freq.A4 * 2.0.pow((m - 69) / 12.0)) - pxSemi / 2
                            val l = label(notes[0] + (Math.floorDiv(m, 12) - 1), keyStyle)
                            if (l.size.width < pxSemi * 2.2f) drawText(l, topLeft = Offset(kx + 2.dp.toPx(), h - l.size.height - 2.dp.toPx()))
                        }
                        drawRect(edge, Offset.Zero, size)
                    }
                    drawRoundRect(c.line, Offset.Zero, size, corner, style = Stroke(1.dp.toPx()))
                    // Nadel fest in der Mitte
                    drawGlow(c.accent, Offset(cx, h / 2), h * 0.6f, if (playing) 0.35f else 0.22f)
                    drawLine(c.accent, Offset(cx, 2.dp.toPx()), Offset(cx, h - 2.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                    drawPath(tri, c.accent)
                }
            },
    )
}

// "1", "20", "500", "1k", "20k"
private fun decadeLabel(hz: Double): String = when {
    hz >= 1_000 -> "${(hz / 1_000).toInt()}k"
    else -> hz.toInt().toString()
}
