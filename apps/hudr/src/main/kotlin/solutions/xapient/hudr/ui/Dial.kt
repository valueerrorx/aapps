package solutions.xapient.hudr.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.SpeedUnit
import solutions.xapient.hudr.domain.Ticks
import solutions.xapient.hudr.domain.formatSpeed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val START = 135f
private const val SWEEP = 270f
private val SIN45 = sqrt(0.5f)
// Breite zu Hoehe des Bogens samt Rand
const val DIAL_ASPECT = 1.12f

private fun angleOf(f: Float) = START + SWEEP * f

private fun Offset.polar(r: Float, deg: Float): Offset {
    val a = deg * PI.toFloat() / 180f
    return Offset(x + r * cos(a), y + r * sin(a))
}

private class Geo(val center: Offset, val r: Float, val stroke: Float)

// Bogen mittig; Rand fasst Schein (1,5 Strich) und Schwellenmarke
private fun Density.geo(size: Size): Geo {
    val stroke = (min(size.width, size.height) * 0.05f).coerceIn(10.dp.toPx(), 20.dp.toPx())
    val m = 1.5f * stroke + 10.dp.toPx()
    val r = min((size.width - 2 * m) / 2, (size.height - 2 * m) / (1 + SIN45)).coerceAtLeast(40.dp.toPx())
    val cy = (size.height - r * (1 + SIN45)) / 2 + r
    return Geo(Offset(size.width / 2, cy), r, stroke)
}

// Skala je Skalenendwert, einmal je Groesse und Skala gebaut
private class Scale(val major: Path, val minor: Path, val labels: List<Pair<TextLayoutResult, Offset>>)

/**
 * Hero: 270°-Bogen mit Skala, Schwellenmarke, leuchtendem Ausschlag und riesiger Zahl.
 * update() und alle Werte nur in der Zeichenphase; empty = Standort fehlt (nur Spur, Inhalt darueber).
 */
@Composable
fun Dial(
    display: SpeedDisplay,
    update: () -> Unit,
    unit: SpeedUnit,
    limit: Int?,
    empty: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = Aapps.colors
    val type = Aapps.type
    val measurer = rememberTextMeasurer()
    val labelSp = cappedSp(12f, 15f)
    val unitSp = cappedSp(14f, 17f)
    val unitText = stringResource(unitRes(unit)).uppercase()
    Box(
        modifier.drawWithCache {
            val g = geo(size)
            val center = g.center
            val r = g.r
            val w = g.stroke
            val arcTopLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(2 * r, 2 * r)
            val trackStroke = Stroke(w, cap = StrokeCap.Round)
            val tickOuter = r - w / 2 - 8.dp.toPx()
            val labelStyle = type.number.copy(fontSize = labelSp)
            val scales = HashMap<Int, Scale>()
            fun scale(s: Int) = scales.getOrPut(s) {
                val step = Ticks.labelStep(s)
                val minor = step / 5f
                val major = Path()
                val minorPath = Path()
                var i = 0
                while (i * minor <= s + 1e-3f) {
                    val big = i % 5 == 0
                    val a = angleOf(i * minor / s)
                    val len = if (big) 10.dp.toPx() else 5.dp.toPx()
                    val o = center.polar(tickOuter, a)
                    val inner = center.polar(tickOuter - len, a)
                    (if (big) major else minorPath).apply { moveTo(o.x, o.y); lineTo(inner.x, inner.y) }
                    i++
                }
                val labels = (0..s step step).map { v ->
                    val l = measurer.measure(v.toString(), labelStyle)
                    val rr = tickOuter - 10.dp.toPx() - 7.dp.toPx() - maxOf(l.size.width, l.size.height) / 2f
                    val at = center.polar(rr, angleOf(v.toFloat() / s))
                    l to Offset(at.x - l.size.width / 2f, at.y - l.size.height / 2f)
                }
                Scale(major, minorPath, labels)
            }
            // Zahl: Groesse aus der Geometrie, fest fuer "188", damit nichts springt
            val probe = measurer.measure("188", type.display.copy(fontSize = 100.sp))
            val k = min(r * 0.78f / probe.size.height, r * 1.22f / probe.size.width)
            val numStyle = type.display.copy(fontSize = 100.sp * k, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            val numH = measurer.measure("188", numStyle).size.height
            val unitLayout = measurer.measure(unitText, type.label.copy(fontSize = unitSp, color = c.fgMuted))
            val markW = 3.dp.toPx()
            val rimR = r + w / 2 + 6.dp.toPx()
            onDrawBehind {
                // Spur, auch ohne Standort sichtbar
                drawArc(c.surfaceHigh, START, SWEEP, false, arcTopLeft, arcSize, style = trackStroke)
                if (empty) return@onDrawBehind
                update()
                val rd = display.reading ?: return@onDrawBehind
                val sc = scale(rd.scale)
                drawPath(sc.minor, c.fgFaint, style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(sc.major, c.fgMuted, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                sc.labels.forEach { (l, at) -> drawText(l, c.fgMuted, at) }

                val live = rd.phase == GpsPhase.FIX
                val alpha = if (live) 1f else LOST_ALPHA
                val col = alertColor(rd.alert, c.accent)
                val frac = display.arc
                if (live) drawGlow(col, center, r * 1.05f, GLOW_MAX * (0.35f + 0.65f * frac))
                val sweep = SWEEP * frac
                if (sweep > 0.3f) {
                    val brush = Brush.sweepGradient(
                        0f to col.copy(alpha = 0.55f),
                        (SWEEP / 360f) * frac to col,
                        // hinter dem Ende zurueck: die runde Kappe am Anfang liegt knapp unter 0°
                        1f to col.copy(alpha = 0.55f),
                        center = center,
                    )
                    rotate(START, center) {
                        // weicher Schein in zwei Lagen, dann der Bogen selbst
                        drawArc(col, 0f, sweep, false, arcTopLeft, arcSize, alpha = 0.08f * alpha, style = Stroke(w * 3f, cap = StrokeCap.Round))
                        drawArc(col, 0f, sweep, false, arcTopLeft, arcSize, alpha = 0.16f * alpha, style = Stroke(w * 1.8f, cap = StrokeCap.Round))
                        drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = alpha, style = trackStroke)
                    }
                }
                if (!rd.speed.isNaN() || frac > 0f) {
                    val tip = center.polar(r, angleOf(frac))
                    if (live) drawGlow(col, tip, w * 2.2f, 0.5f)
                    drawCircle(c.fg.copy(alpha = alpha), w * 0.3f, tip)
                }
                // Schwellenmarke quer ueber Spur und Rand
                if (limit != null) {
                    val a = angleOf(Ticks.fraction(limit.toFloat(), rd.scale))
                    drawLine(Amber, center.polar(r - w / 2 - 2.dp.toPx(), a), center.polar(rimR, a), markW, StrokeCap.Round)
                }
                // Zahl und Einheit in der Mitte
                val text = formatSpeed(rd.speed)
                val num = measurer.measure(text, numStyle)
                val numColor = when {
                    !live -> c.fgMuted
                    rd.alert == Alert.NONE -> c.fg
                    else -> col
                }
                val top = center.y - numH * 0.58f
                drawText(num, numColor, Offset(center.x - num.size.width / 2f, top))
                drawText(unitLayout, topLeft = Offset(center.x - unitLayout.size.width / 2f, top + numH * 0.92f))
            }
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
