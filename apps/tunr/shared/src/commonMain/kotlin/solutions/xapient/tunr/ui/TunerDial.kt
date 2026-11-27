package solutions.xapient.tunr.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import solutions.xapient.aapps.core.ui.decimalSeparator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.tunr.domain.Accidental
import solutions.xapient.tunr.domain.Notation
import solutions.xapient.tunr.domain.Notes
import solutions.xapient.tunr.domain.TunerMotion.Companion.INNER_RATIO
import solutions.xapient.tunr.domain.TunerMotion.Companion.STROBE_INNER
import solutions.xapient.tunr.domain.TunerMotion.Companion.STROBE_OUTER
import solutions.xapient.tunr.res.Res
import solutions.xapient.tunr.res.cent
import solutions.xapient.tunr.res.hz
import solutions.xapient.tunr.res.play_a_note
import solutions.xapient.tunr.res.state_flat
import solutions.xapient.tunr.res.state_in_tune
import solutions.xapient.tunr.res.state_listening
import solutions.xapient.tunr.res.state_sharp
import solutions.xapient.tunr.res.tuner

// Skala: +-50 Cent ueber +-120 Grad, oben Mitte = 0
const val DEG_PER_CENT = 2.4f

private fun angleOf(cents: Float) = -90f + cents * DEG_PER_CENT

// Hero: Strobe-Ring, Bogenskala mit Zeiger, mittig Note, Cent und Hz. Feinwerte nur in der Zeichenphase.
@Composable
fun TunerDial(state: TunerState, notation: Notation, hint: String?, modifier: Modifier = Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer(cacheSize = 48)
    val display = Aapps.type.display
    val number = Aapps.type.number
    val label = Aapps.type.label
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val dec = decimalSeparator()
    val play = stringResource(Res.string.play_a_note)
    val centWord = stringResource(Res.string.cent)
    val hzWord = stringResource(Res.string.hz)
    val desc = stringResource(Res.string.tuner)
    val coarse = state.coarse
    val noteText = if (coarse.target >= 0) Notes.name(coarse.target, notation).toString() else ""
    val stateText = when {
        hint != null -> hint
        !coarse.heard || coarse.target < 0 -> stringResource(Res.string.state_listening)
        coarse.inTune -> stringResource(Res.string.state_in_tune, noteText)
        // Ansage in 5-Cent-Schritten, bleibt so aktuell ohne Neukomposition je Frame
        coarse.dir < 0 -> stringResource(Res.string.state_flat, noteText, abs(coarse.cents5))
        coarse.dir > 0 -> stringResource(Res.string.state_sharp, noteText, abs(coarse.cents5))
        else -> noteText
    }
    val centerHint = hint ?: if (!coarse.heard) play else null
    val idle = idleLine()
    val zoneOff = zoneDim()
    Spacer(
        modifier
            .semantics {
                contentDescription = desc
                stateDescription = stateText
            }
            .drawWithCache {
                val r = min(size.width, size.height) / 2 - 6.dp.toPx()
                val mid = Offset(size.width / 2, size.height / 2)
                val rOuter = r * 0.935f
                val rInner = r * 0.855f
                val rg = r * 0.72f
                val outerW = r * 0.045f
                val innerW = r * 0.022f
                val outerDash = dash(rOuter, STROBE_OUTER, 0.46f)
                val innerDash = dash(rInner, STROBE_INNER, 0.4f)
                val ticks = Path().apply {
                    for (cent in -50..50 step 5) {
                        val len = when {
                            cent == 0 -> r * 0.11f
                            cent % 25 == 0 -> r * 0.085f
                            cent % 10 == 0 -> r * 0.06f
                            else -> r * 0.035f
                        }
                        val a = angleOf(cent.toFloat()).toDouble() * PI / 180
                        val r0 = rg - r * 0.03f
                        moveTo(mid.x + r0 * cos(a).toFloat(), mid.y + r0 * sin(a).toFloat())
                        lineTo(mid.x + (r0 - len) * cos(a).toFloat(), mid.y + (r0 - len) * sin(a).toFloat())
                    }
                }
                val tickStroke = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round)
                val gaugeTL = Offset(mid.x - rg, mid.y - rg)
                val gaugeSize = Size(2 * rg, 2 * rg)
                val labelPx = min(label.fontSize.toPx() * 1.1f * fs, r * 0.085f)
                val labelStyle = label.copy(fontSize = labelPx.toSp(), textAlign = TextAlign.Center)
                val letterStyle = display.copy(fontSize = (r * 0.5f).toSp(), letterSpacing = 0.sp)
                val octaveStyle = number.copy(fontSize = (r * 0.13f).toSp())
                val centStyle = number.copy(fontSize = (r * 0.12f).toSp())
                val hintLayout = centerHint?.let {
                    measurer.measure(it.uppercase(), labelStyle, constraints = Constraints(maxWidth = (rg * 1.3f).toInt()))
                }
                // Note und Vorzeichen aendern sich selten: einmal je Groesse/Note messen
                val name = if (coarse.target >= 0) Notes.name(coarse.target, notation) else null
                val letter = name?.let { measurer.measure(it.letter, letterStyle) }
                val octave = name?.let { measurer.measure(it.octave.toString(), octaveStyle) }
                val unit = measurer.measure(centWord.uppercase(), labelStyle)
                // Cent und Hz nur neu setzen, wenn sich der gerundete Wert aendert
                var numKey = Int.MIN_VALUE
                var numLayout: TextLayoutResult? = null
                var hzKey = Int.MIN_VALUE
                var hzLayout: TextLayoutResult? = null
                onDrawBehind {
                    state.frame
                    val rd = state.reading
                    val m = state.motion
                    val pres = m.presence.value.coerceIn(0f, 1f)
                    val lock = m.lock.value.coerceIn(0f, 1.2f)
                    val needle = m.needle.value
                    val col = stateColor(needle)
                    val live = lerp(idle, col, pres)

                    drawGlow(col, mid, r * 1.2f, GLOW_BASE * pres + GLOW_TONE * pres + GLOW_LOCK * lock.coerceAtMost(1f))
                    // Strobe: aussen dreht mit der Abweichung, innen halb so schnell
                    rotate(m.strobe, mid) {
                        drawCircle(live, rOuter, mid, style = Stroke(outerW, pathEffect = outerDash))
                    }
                    rotate(m.strobe * INNER_RATIO, mid) {
                        drawCircle(live.copy(alpha = 0.55f + 0.45f * (1 - pres)), rInner, mid, style = Stroke(innerW, pathEffect = innerDash))
                    }
                    // Bogen und Teilstriche
                    drawArc(c.line, 150f, 240f, false, gaugeTL, gaugeSize, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(ticks, idle, style = tickStroke)
                    // Zone gestimmt: ohne Ton grau wie die Skala, mit Ton gedimmt gruen, eingerastet voll
                    val zone = lerp(lerp(idle, zoneOff, pres), TunrGreen, lock.coerceAtMost(1f))
                    if (lock > 0.01f) {
                        drawArc(TunrGreen.copy(alpha = 0.16f * lock.coerceAtMost(1f)), angleOf(-5f), 10 * DEG_PER_CENT, false, gaugeTL, gaugeSize, style = Stroke(r * 0.09f, cap = StrokeCap.Round))
                    }
                    drawArc(zone, angleOf(-5f), 10 * DEG_PER_CENT, false, gaugeTL, gaugeSize, style = Stroke(r * 0.03f, cap = StrokeCap.Round))
                    // Glanz einmal nach dem Einrasten
                    if (m.shine < 1f) {
                        val t = m.shine
                        val e = 1 - (1 - t) * (1 - t)
                        drawCircle(TunrGreen.copy(alpha = 0.55f * (1 - t) * (1 - t)), rg + (r * 1.02f - rg) * e, mid, style = Stroke((3.dp.toPx() * (1 - t)).coerceAtLeast(1f)))
                    }
                    // Abweichung als Bogen von 0 bis zum Zeiger, dann der Zeiger
                    if (pres > 0.01f) {
                        val sweep = needle * DEG_PER_CENT
                        if (abs(sweep) > 0.5f) {
                            drawArc(col.copy(alpha = 0.22f * pres), -90f, sweep, false, gaugeTL, gaugeSize, style = Stroke(r * 0.06f, cap = StrokeCap.Round))
                            drawArc(col.copy(alpha = pres), -90f, sweep, false, gaugeTL, gaugeSize, style = Stroke(r * 0.022f, cap = StrokeCap.Round))
                        }
                        val a = angleOf(needle).toDouble() * PI / 180
                        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
                        val tip = mid + dir * rg
                        drawGlow(col, tip, r * 0.16f, 0.55f * pres)
                        // kurzer Strich quer durch den Bogen
                        drawLine(col.copy(alpha = pres), mid + dir * (rg - r * 0.075f), mid + dir * (rg + r * 0.075f), 3.dp.toPx(), StrokeCap.Round)
                        drawCircle(col.copy(alpha = pres), r * 0.032f, tip)
                        drawCircle(Color.White.copy(alpha = 0.85f * pres), r * 0.013f, tip)
                    }
                    // Mitte: Hinweis oder Note mit Cent und Hz als eine Gruppe
                    if (hintLayout != null) {
                        drawText(hintLayout, c.fgMuted, Offset(mid.x - hintLayout.size.width / 2f, mid.y - hintLayout.size.height / 2f))
                    } else if (letter != null && octave != null && name != null) {
                        val ink = lerp(c.fgMuted, c.fg, pres)
                        val noteY = mid.y - r * 0.1f
                        val lx = mid.x - letter.size.width / 2f
                        val ly = noteY - letter.size.height / 2f
                        drawText(letter, ink, Offset(lx, ly))
                        val side = lx + letter.size.width + r * 0.02f
                        val base = ly + letter.firstBaseline
                        // Vorzeichen oben an der Versalhoehe, Oktave auf der Grundlinie
                        if (name.accidental != Accidental.NONE) {
                            drawAccidental(name.accidental, Offset(side, base - r * 0.36f), r * 0.17f, ink)
                        }
                        drawText(octave, lerp(c.fgMuted, ink, 0.6f), Offset(side, base - octave.firstBaseline))
                        if (pres > 0.01f) {
                            val cents = rd.cents.roundToInt()
                            if (cents != numKey || numLayout == null) {
                                val sign = if (cents > 0) "+" else if (cents < 0) "−" else "±"
                                numLayout = measurer.measure(sign + abs(cents), centStyle)
                                numKey = cents
                            }
                            val num = numLayout!!
                            val gap = r * 0.03f
                            val w = num.size.width + gap + unit.size.width
                            val cy = mid.y + r * 0.27f
                            val x0 = mid.x - w / 2
                            drawText(num, col.copy(alpha = pres), Offset(x0, cy - num.size.height / 2f))
                            val baseY = cy - num.size.height / 2f + num.firstBaseline
                            drawText(unit, c.fgMuted.copy(alpha = pres), Offset(x0 + num.size.width + gap, baseY - unit.firstBaseline))
                            val hz10 = (rd.hz * 10f).roundToInt()
                            if (hz10 != hzKey || hzLayout == null) {
                                hzLayout = measurer.measure("${hz10 / 10}$dec${hz10 % 10} $hzWord", labelStyle.copy(letterSpacing = 0.04.sp))
                                hzKey = hz10
                            }
                            val hz = hzLayout!!
                            drawText(hz, c.fgMuted.copy(alpha = pres), Offset(mid.x - hz.size.width / 2f, mid.y + r * 0.43f - hz.size.height / 2f))
                        }
                    }
                }
            },
    )
}

// gleichmaessige Striche rund um den Kreis
private fun dash(radius: Float, count: Int, on: Float): PathEffect {
    val seg = (2 * PI * radius / count).toFloat()
    return PathEffect.dashPathEffect(floatArrayOf(seg * on, seg * (1 - on)), 0f)
}

// Kreuz und Be als eigene Striche: unabhaengig von den Glyphen der Schrift
fun DrawScope.drawAccidental(a: Accidental, topLeft: Offset, s: Float, color: Color) {
    val x = topLeft.x
    val y = topLeft.y
    when (a) {
        Accidental.SHARP -> {
            val v = s * 0.075f
            drawLine(color, Offset(x + s * 0.36f, y + s * 0.04f), Offset(x + s * 0.36f, y + s * 1.0f), v, StrokeCap.Round)
            drawLine(color, Offset(x + s * 0.64f, y + s * 0.0f), Offset(x + s * 0.64f, y + s * 0.96f), v, StrokeCap.Round)
            val h = s * 0.13f
            drawLine(color, Offset(x + s * 0.12f, y + s * 0.40f), Offset(x + s * 0.88f, y + s * 0.30f), h, StrokeCap.Round)
            drawLine(color, Offset(x + s * 0.12f, y + s * 0.74f), Offset(x + s * 0.88f, y + s * 0.64f), h, StrokeCap.Round)
        }
        Accidental.FLAT -> {
            val w = s * 0.09f
            drawLine(color, Offset(x + s * 0.28f, y), Offset(x + s * 0.28f, y + s), w, StrokeCap.Round)
            val bowl = Path().apply {
                moveTo(x + s * 0.28f, y + s * 0.62f)
                cubicTo(x + s * 0.55f, y + s * 0.42f, x + s * 0.88f, y + s * 0.50f, x + s * 0.72f, y + s * 0.70f)
                cubicTo(x + s * 0.62f, y + s * 0.83f, x + s * 0.42f, y + s * 0.93f, x + s * 0.28f, y + s)
            }
            drawPath(bowl, color, style = Stroke(w, cap = StrokeCap.Round))
        }
        Accidental.NONE -> Unit
    }
}
