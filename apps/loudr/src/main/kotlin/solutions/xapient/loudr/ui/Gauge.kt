package solutions.xapient.loudr.ui

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.loudr.R
import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.domain.Example
import solutions.xapient.loudr.domain.Scale
import solutions.xapient.loudr.domain.TimeWeighting
import solutions.xapient.loudr.domain.Zone
import solutions.xapient.loudr.domain.formatDb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val START = 135f
private const val SWEEP = 270f
private val SIN45 = sqrt(0.5f)
// Breite zu Hoehe des Bogens samt Rand
const val GAUGE_ASPECT = 1.12f

private fun angleOf(db: Float) = START + SWEEP * Scale.fraction(db)

private fun Offset.polar(r: Float, deg: Float): Offset {
    val a = deg * PI.toFloat() / 180f
    return Offset(x + r * cos(a), y + r * sin(a))
}

private class Geo(val center: Offset, val r: Float, val stroke: Float)

// Bogen mittig in der Flaeche; Rand fasst Schein (1,5 Strich), Zonen-Rand und Max-Marke
private fun Density.geo(size: Size): Geo {
    val stroke = (min(size.width, size.height) * 0.055f).coerceIn(10.dp.toPx(), 22.dp.toPx())
    val m = maxOf(1.5f * stroke, stroke / 2 + 12.dp.toPx()) + 12.dp.toPx()
    val r = min((size.width - 2 * m) / 2, (size.height - 2 * m) / (1 + SIN45)).coerceAtLeast(40.dp.toPx())
    val cy = (size.height - r * (1 + SIN45)) / 2 + r
    return Geo(Offset(size.width / 2, cy), r, stroke)
}

// Zonenfarben entlang des Bogens, weich verblendet; Winkel ab Bogenanfang
private fun zoneBrush(center: Offset): Brush {
    val stops = mutableListOf<Pair<Float, Color>>()
    val edges = listOf(Zone.MODERATE.from, Zone.LOUD.from, Zone.VERY_LOUD.from)
    val blend = 0.012f
    stops += 0f to zoneColor(Zone.QUIET)
    edges.forEachIndexed { i, e ->
        val f = Scale.fraction(e) * SWEEP / 360f
        stops += (f - blend) to zoneColor(Zone.entries[i])
        stops += (f + blend) to zoneColor(Zone.entries[i + 1])
    }
    // hinter dem Bogenende zurueck zu "ruhig": die runde Kappe am Anfang liegt knapp unter 0° und darf nicht rot werden
    val end = SWEEP / 360f
    stops += (end + 0.02f) to zoneColor(Zone.VERY_LOUD)
    stops += (end + 0.12f) to zoneColor(Zone.QUIET)
    stops += 1f to zoneColor(Zone.QUIET)
    return Brush.sweepGradient(*stops.toTypedArray(), center = center)
}

/**
 * Hero: 270°-Bogen 20–120 dB mit Zonen-Rand, Skala, Pegelbogen, Max-Marke und grosser Zahl.
 * update() und alle Werte nur in der Zeichenphase; empty = Mikrofon fehlt (nur Skala, Inhalt darueber).
 */
@Composable
fun Gauge(
    display: MeterDisplay,
    update: () -> Unit,
    weighting: TimeWeighting,
    offset: Float,
    measuring: Boolean,
    empty: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val c = Aapps.colors
    val type = Aapps.type
    val measurer = rememberTextMeasurer()
    val labelSp = cappedSp(11f, 15f)
    val unitSp = cappedSp(12f, 15f)
    val exampleSp = cappedSp(13f, 17f)
    val unit = "${stringResource(R.string.unit)} · ${stringResource(if (weighting == TimeWeighting.FAST) R.string.fast else R.string.slow)}".uppercase()
    val examples = Example.entries.map { stringResource(exampleRes(it)).uppercase() }
    val overload = stringResource(R.string.overload).uppercase()
    Box(
        modifier.drawWithCache {
            val g = geo(size)
            val center = g.center
            val r = g.r
            val w = g.stroke
            val arcTopLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(2 * r, 2 * r)
            val brush = zoneBrush(center)
            // Skala innen: grosse Striche je 10 dB, kleine je 2 dB
            val tickOuter = r - w / 2 - 7.dp.toPx()
            val major = Path()
            val minor = Path()
            for (db in Scale.MIN.toInt()..Scale.MAX.toInt() step 2) {
                val a = angleOf(db.toFloat())
                val big = db % 10 == 0
                val p = if (big) major else minor
                val len = if (big) 9.dp.toPx() else 4.dp.toPx()
                val o = center.polar(tickOuter, a)
                val i = center.polar(tickOuter - len, a)
                p.moveTo(o.x, o.y)
                p.lineTo(i.x, i.y)
            }
            val labelStyle = type.number.copy(fontSize = labelSp)
            // Enden ohne Zahl: unten bleibt Platz fuer den Vergleich
            val labels = (40..100 step 20).map { db ->
                val l = measurer.measure(db.toString(), labelStyle)
                val rr = tickOuter - 9.dp.toPx() - 6.dp.toPx() - maxOf(l.size.width, l.size.height) / 2f
                val at = center.polar(rr, angleOf(db.toFloat()))
                l to Offset(at.x - l.size.width / 2f, at.y - l.size.height / 2f)
            }
            // Zonen-Rand aussen, mit kleinen Luecken an den Grenzen
            val rimR = r + w / 2 + 7.dp.toPx()
            val rimW = 3.dp.toPx()
            val rims = Zone.entries.map { z ->
                val from = if (z == Zone.QUIET) Scale.MIN else z.from
                val to = Zone.entries.getOrNull(z.ordinal + 1)?.from ?: Scale.MAX
                val a0 = angleOf(from) + if (z == Zone.QUIET) 0f else 1.2f
                val a1 = angleOf(to) - if (z == Zone.VERY_LOUD) 0f else 1.2f
                Triple(zoneColor(z), a0, a1 - a0)
            }
            // Zahl: Groesse aus der Geometrie, unabhaengig von der Schriftgroesse
            val probe = measurer.measure("88", type.display.copy(fontSize = 100.sp))
            // "120" ist etwa anderthalbmal so breit wie "88"; Schriftfaktor kuerzt sich heraus
            val k = min(r * 0.62f / probe.size.height, r * 1.15f / (probe.size.width * 1.5f))
            val numStyle = type.display.copy(fontSize = 100.sp * k)
            val unitStyle = type.label.copy(fontSize = unitSp)
            val unitLayout = measurer.measure(unit, unitStyle.copy(color = c.fgMuted))
            // zwischen den Bogenenden, Punkt und Abstand eingerechnet
            val exMax = r * 0.9f
            val fit = { t: String ->
                val base = type.label.copy(fontSize = exampleSp)
                val l = measurer.measure(t, base)
                if (l.size.width <= exMax) l else measurer.measure(t, base.copy(fontSize = base.fontSize * (exMax / l.size.width)))
            }
            val exLayouts = examples.map(fit)
            val overLayout = fit(overload)
            val trackStroke = Stroke(w, cap = StrokeCap.Round)
            val markW = 2.dp.toPx()
            onDrawBehind {
                if (!empty) update()
                // Zonen-Rand und Spur, auch ohne Messung sichtbar
                rims.forEach { (col, a0, sw) ->
                    drawArc(col.copy(alpha = RIM_ALPHA), a0, sw, false, Offset(center.x - rimR, center.y - rimR), Size(2 * rimR, 2 * rimR), style = Stroke(rimW, cap = StrokeCap.Butt))
                }
                drawArc(c.surfaceHigh, START, SWEEP, false, arcTopLeft, arcSize, style = trackStroke)
                drawPath(minor, c.fgFaint, style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(major, c.fgMuted, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                labels.forEach { (l, at) -> drawText(l, c.fgMuted, at) }
                if (empty) return@onDrawBehind

                val level = Calibration.apply(display.arc, offset)
                val alpha = if (measuring) 1f else PAUSED_ALPHA
                if (!level.isNaN()) {
                    val frac = Scale.fraction(level)
                    val zc = zoneColor(Zone.of(level))
                    if (measuring) drawGlow(zc, center, r * 1.05f, GLOW_MAX * (0.3f + 0.7f * frac))
                    val sweep = SWEEP * frac
                    if (sweep > 0.3f) {
                        rotate(START, center) {
                            // weicher Schein in zwei Lagen, dann der Bogen selbst
                            drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = 0.08f * alpha, style = Stroke(w * 3f, cap = StrokeCap.Round))
                            drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = 0.16f * alpha, style = Stroke(w * 1.8f, cap = StrokeCap.Round))
                            drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = alpha, style = trackStroke)
                        }
                    }
                    val tip = center.polar(r, angleOf(level))
                    if (measuring) drawGlow(zc, tip, w * 2.2f, 0.5f)
                    drawCircle(c.fg.copy(alpha = alpha), w * 0.3f, tip)
                }
                // Max-Marke quer ueber Spur und Rand
                val max = Calibration.apply(display.shown.max(weighting), offset)
                if (!max.isNaN()) {
                    val a = angleOf(max)
                    drawLine(c.fg.copy(alpha = alpha), center.polar(r - w / 2 - 2.dp.toPx(), a), center.polar(rimR + 5.dp.toPx(), a), markW, StrokeCap.Round)
                }
                val readout = Readout(numStyle, unitLayout, exLayouts, overLayout)
                drawReadout(center, r, display.shown.level(weighting).let { Calibration.apply(it, offset) }, display.shown.overload, readout, measurer, alpha, c.fg)
            }
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private class Readout(
    val numStyle: TextStyle,
    val unit: TextLayoutResult,
    val examples: List<TextLayoutResult>,
    val overload: TextLayoutResult,
)

// Zahl, Einheit und Alltagsvergleich in der Mitte; uebersteuert: "≥" vor der Zahl und Hinweis statt Vergleich
private fun DrawScope.drawReadout(
    center: Offset,
    r: Float,
    level: Float,
    over: Boolean,
    o: Readout,
    measurer: androidx.compose.ui.text.TextMeasurer,
    alpha: Float,
    fg: Color,
) {
    val num = measurer.measure(formatDb(level), o.numStyle)
    val numTop = center.y - num.size.height * 0.56f
    val numLeft = center.x - num.size.width / 2f
    drawText(num, fg, Offset(numLeft, numTop))
    val ex = Example.of(level)
    val showOver = over && ex != null
    if (showOver) {
        // "≥" gezeichnet statt gesetzt: nicht jede Schrift hat das Zeichen
        val s = num.size.height * 0.2f
        val x1 = numLeft - s * 0.4f
        val x0 = x1 - s
        val cy = numTop + num.size.height * 0.5f
        val sw = maxOf(s * 0.16f, 2.dp.toPx())
        val col = fg.copy(alpha = alpha)
        drawLine(col, Offset(x0, cy - s * 0.6f), Offset(x1, cy - s * 0.15f), sw, StrokeCap.Round)
        drawLine(col, Offset(x1, cy - s * 0.15f), Offset(x0, cy + s * 0.3f), sw, StrokeCap.Round)
        drawLine(col, Offset(x0, cy + s * 0.65f), Offset(x1, cy + s * 0.65f), sw, StrokeCap.Round)
    }
    val unitTop = numTop + num.size.height * 0.9f
    drawText(o.unit, topLeft = Offset(center.x - o.unit.size.width / 2f, unitTop))
    if (ex == null) return
    val l = if (showOver) o.overload else o.examples[ex.ordinal]
    val dot = 4.dp.toPx()
    val gap = 8.dp.toPx()
    val total = dot * 2 + gap + l.size.width
    val cy = maxOf(center.y + r * 0.58f, unitTop + o.unit.size.height + l.size.height * 0.9f)
    val x0 = center.x - total / 2
    val dotColor = zoneColor(if (showOver) Zone.VERY_LOUD else Zone.of(level))
    drawCircle(dotColor.copy(alpha = alpha), dot, Offset(x0 + dot, cy))
    drawText(l, fg, Offset(x0 + dot * 2 + gap, cy - l.size.height / 2f))
}
