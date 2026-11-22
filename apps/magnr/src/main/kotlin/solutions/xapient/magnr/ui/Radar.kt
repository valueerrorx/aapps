package solutions.xapient.magnr.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.magnr.R
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.domain.Sensitivity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val START = 135f
private const val SWEEP = 270f
private val SIN45 = sqrt(0.5f)
// Breite zu Hoehe des Radars samt Rand
const val RADAR_ASPECT = 1.12f

private fun angleOf(fraction: Float) = START + SWEEP * fraction.coerceIn(0f, 1f)

private fun Offset.polar(r: Float, deg: Float): Offset {
    val a = deg * PI.toFloat() / 180f
    return Offset(x + r * cos(a), y + r * sin(a))
}

private class Geo(val center: Offset, val r: Float, val stroke: Float)

// Bogen mittig in der Flaeche; Rand fasst den Schein (1,5 Strich) und die Spitzen-Marke
private fun Density.geo(size: Size): Geo {
    val stroke = (min(size.width, size.height) * 0.05f).coerceIn(9.dp.toPx(), 20.dp.toPx())
    val m = 1.5f * stroke + 8.dp.toPx()
    val r = min((size.width - 2 * m) / 2, (size.height - 2 * m) / (1 + SIN45)).coerceAtLeast(40.dp.toPx())
    val cy = (size.height - r * (1 + SIN45)) / 2 + r
    return Geo(Offset(size.width / 2, cy), r, stroke)
}

/**
 * Hero: Radar mit 270°-Bogen (Ausschlag), Spitzen-Marke, Ringen, Wellen und Lichthof, innen der Betrag.
 * Alle Messwerte nur in der Zeichenphase; empty = kein Sensor (nur Skala, Inhalt darueber).
 */
@Composable
fun Radar(
    state: DetectorState,
    sensitivity: Sensitivity,
    live: Boolean,
    empty: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = Aapps.colors
    val type = Aapps.type
    val measurer = rememberTextMeasurer()
    val sep = decimalSeparator()
    val labelSp = cappedSp(11f, 15f)
    val unitSp = cappedSp(12f, 15f)
    val levelSp = cappedSp(13f, 17f)
    // nicht in Versalien: aus µT wuerde MT
    val unit = stringResource(R.string.unit_total)
    val levels = Level.entries.map { stringResource(levelRes(it)).uppercase() }
    val limit = stringResource(R.string.at_limit).uppercase()
    val settling = stringResource(R.string.settling).uppercase()
    Box(
        modifier.drawWithCache {
            val g = geo(size)
            val center = g.center
            val r = g.r
            val w = g.stroke
            val arcTopLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(2 * r, 2 * r)
            // Ausschlag: violett, zum Ende hin heller
            val end = SWEEP / 360f
            val brush = Brush.sweepGradient(
                0f to MagnrAccent.copy(alpha = ARC_START_ALPHA),
                end * 0.7f to MagnrAccent,
                end to AccentLight,
                1f to MagnrAccent.copy(alpha = ARC_START_ALPHA),
                center = center,
            )
            // Skala innen: grosse Striche je Viertel, kleine je Zwanzigstel
            val tickOuter = r - w / 2 - 7.dp.toPx()
            val major = Path()
            val minor = Path()
            for (i in 0..20) {
                val a = angleOf(i / 20f)
                val big = i % 5 == 0
                val p = if (big) major else minor
                val len = if (big) 9.dp.toPx() else 4.dp.toPx()
                val o = center.polar(tickOuter, a)
                val n = center.polar(tickOuter - len, a)
                p.moveTo(o.x, o.y)
                p.lineTo(n.x, n.y)
            }
            val labelStyle = type.number.copy(fontSize = labelSp)
            // Enden ohne Zahl: unten bleibt Platz fuer die Stufe
            val labels = listOf(0.25f, 0.5f, 0.75f).map { f ->
                val l = measurer.measure(formatScale(sensitivity.fullScale * f, sep), labelStyle)
                val rr = tickOuter - 9.dp.toPx() - 6.dp.toPx() - maxOf(l.size.width, l.size.height) / 2f
                val at = center.polar(rr, angleOf(f))
                l to Offset(at.x - l.size.width / 2f, at.y - l.size.height / 2f)
            }
            // Zahl: Groesse aus der Geometrie, unabhaengig von der Schriftgroesse
            val probe = measurer.measure("888.8", type.display.copy(fontSize = 100.sp))
            val k = min(r * 0.5f / probe.size.height, r * 1.2f / probe.size.width)
            val numStyle = type.display.copy(fontSize = 100.sp * k)
            val unitLayout = measurer.measure(unit, type.label.copy(fontSize = unitSp, color = c.fgMuted))
            // Stufe zwischen den Bogenenden, Punkt und Abstand eingerechnet
            val lvMax = r * 1.0f
            val fit = { t: String ->
                val base = type.label.copy(fontSize = levelSp)
                val l = measurer.measure(t, base)
                if (l.size.width <= lvMax) l else measurer.measure(t, base.copy(fontSize = base.fontSize * (lvMax / l.size.width)))
            }
            val levelLayouts = levels.map(fit)
            val limitLayout = fit(limit)
            val settlingLayout = fit(settling)
            val trackStroke = Stroke(w, cap = StrokeCap.Round)
            val ringStroke = Stroke(1.dp.toPx())
            val rippleStroke = Stroke(2.dp.toPx())
            val markW = 2.dp.toPx()
            onDrawBehind {
                // je Messwert neu zeichnen, ohne Neukomposition
                state.tick
                val alpha = if (live) 1f else PAUSED_ALPHA
                val reading = state.reading
                val s = if (empty) 0f else if (reading.saturated) 1f else reading.strength
                if (!empty) {
                    drawGlow(MagnrAccent, center, r * (0.6f + 0.45f * s), GLOW_MAX * (0.2f + 0.8f * s) * alpha)
                }
                // Radar-Ringe
                drawCircle(c.line, r * 0.34f, center, style = ringStroke)
                drawCircle(c.line, r * 0.62f, center, style = ringStroke)
                // Wellen ab "schwach" zwischen den Ringen: nie hinter Einheit oder Skalenzahlen
                if (live && s >= Level.WEAK.from) {
                    val phase = state.seconds * (0.5f + 1.3f * s)
                    for (i in 0 until 3) {
                        val f = ((phase + i / 3f) % 1f + 1f) % 1f
                        drawCircle(
                            MagnrAccent.copy(alpha = RIPPLE_MAX * s * (1f - f)),
                            r * (0.36f + 0.26f * f), center, style = rippleStroke,
                        )
                    }
                }
                drawArc(c.surfaceHigh, START, SWEEP, false, arcTopLeft, arcSize, style = trackStroke)
                drawPath(minor, c.fgFaint, style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(major, c.fgMuted, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                labels.forEach { (l, at) -> drawText(l, c.fgMuted, at) }
                if (empty) return@onDrawBehind

                val sweep = SWEEP * s
                if (sweep > 0.3f) {
                    rotate(START, center) {
                        // weicher Schein in zwei Lagen, dann der Bogen selbst
                        drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = 0.08f * alpha, style = Stroke(w * 3f, cap = StrokeCap.Round))
                        drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = 0.18f * alpha, style = Stroke(w * 1.8f, cap = StrokeCap.Round))
                        drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = alpha, style = trackStroke)
                    }
                }
                if (!reading.total.isNaN() && !reading.settling) {
                    val tip = center.polar(r, angleOf(s))
                    if (live && s > 0f) drawGlow(MagnrAccent, tip, w * 2.2f, 0.5f)
                    drawCircle(c.fg.copy(alpha = alpha), w * 0.3f, tip)
                }
                // Spitzen-Marke quer ueber die Spur
                val peak = reading.peak
                if (!peak.isNaN()) {
                    val a = angleOf(sensitivity.strength(peak))
                    drawLine(c.fg.copy(alpha = alpha), center.polar(r - w / 2 - 2.dp.toPx(), a), center.polar(r + w / 2 + 5.dp.toPx(), a), markW, StrokeCap.Round)
                }

                // Mitte: Betrag, Einheit, Stufe
                val shown = state.shown
                val num = measurer.measure(formatUt(shown.total, sep), numStyle)
                val numTop = center.y - num.size.height * 0.6f
                drawText(num, c.fg, Offset(center.x - num.size.width / 2f, numTop))
                val unitTop = numTop + num.size.height * 0.92f
                drawText(unitLayout, topLeft = Offset(center.x - unitLayout.size.width / 2f, unitTop))
                if (shown.total.isNaN()) return@onDrawBehind
                val (label, dot) = when {
                    shown.saturated -> limitLayout to Warn
                    shown.settling -> settlingLayout to c.fgMuted
                    else -> levelLayouts[shown.level.ordinal] to (if (shown.level == Level.QUIET) c.fgMuted else MagnrAccent)
                }
                val dotR = 4.dp.toPx()
                val gap = 8.dp.toPx()
                val total = dotR * 2 + gap + label.size.width
                val cy = maxOf(center.y + r * 0.56f, unitTop + unitLayout.size.height + label.size.height * 0.9f)
                val x0 = center.x - total / 2
                drawCircle(dot.copy(alpha = alpha), dotR, Offset(x0 + dotR, cy))
                drawText(label, c.fg, Offset(x0 + dotR * 2 + gap, cy - label.size.height / 2f))
            }
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
