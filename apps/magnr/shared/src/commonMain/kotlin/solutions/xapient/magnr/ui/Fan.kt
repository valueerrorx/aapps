package solutions.xapient.magnr.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.magnr.res.*
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.domain.Sensitivity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Anzahl Boegen; je Bogen ein Achtel des Vollausschlags
const val ARCS = 8
// Oeffnung des Faechers, nach oben
private const val SWEEP = 100f
private const val UP = 270f
// Platz unter dem Ursprung fuer die Ablesung, relativ zum Faecherradius
private const val READ = 0.66f
// Breite zu Hoehe des Faechers samt Ablesung
const val FAN_ASPECT = 1.25f

private val SIN_HALF = sin(SWEEP / 2 * PI / 180).toFloat()

private fun Offset.polar(r: Float, deg: Float): Offset {
    val a = deg * PI.toFloat() / 180f
    return Offset(x + r * cos(a), y + r * sin(a))
}

// Radius des Bogens i bzw. einer Fuellhoehe x (0..ARCS); innen bleibt Platz fuer den Sensorpunkt
private fun radiusAt(r: Float, x: Float) = r * (0.22f + 0.78f * x / ARCS)

private class Geo(val origin: Offset, val r: Float, val stroke: Float)

// Faecher oben, Ablesung unter dem Ursprung; seitlich Platz fuer die Skalenbeschriftung
private fun Density.geo(size: Size, labelW: Float): Geo {
    val pad = 10.dp.toPx()
    val byWidth = (size.width / 2 - pad - labelW) / SIN_HALF
    val byHeight = (size.height - 2 * pad) / (1f + READ + 0.06f)
    val r = min(byWidth, byHeight).coerceAtLeast(60.dp.toPx())
    val stroke = r * 0.78f / ARCS * 0.52f
    val used = r * (1f + READ) + stroke
    val top = (size.height - used) / 2 + stroke / 2
    return Geo(Offset(size.width / 2, top + r), r, stroke)
}

private fun DrawScope.arc(color: androidx.compose.ui.graphics.Color, radius: Float, origin: Offset, sweep: Float, stroke: Stroke, alpha: Float = 1f) {
    drawArc(
        color, UP - sweep / 2, sweep, false,
        Offset(origin.x - radius, origin.y - radius), Size(2 * radius, 2 * radius),
        alpha = alpha, style = stroke,
    )
}

/**
 * Hero: Faecher aus Boegen ueber dem Sensorpunkt, leuchtet von innen nach aussen mit der Abweichung,
 * duenner Bogen fuer die Spitze; darunter gross die Abweichung und die Stufe.
 * Alle Messwerte nur in der Zeichenphase; empty = kein Sensor (nur leere Boegen).
 */
@Composable
fun Fan(
    state: DetectorState,
    sensitivity: Sensitivity,
    live: Boolean,
    empty: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Aapps.colors
    val type = Aapps.type
    val measurer = rememberTextMeasurer()
    val sep = decimalSeparator()
    val labelSp = cappedSp(11f, 14f)
    val unitSp = cappedSp(16f, 20f)
    val levelSp = cappedSp(13f, 17f)
    // nicht in Versalien: aus µT wuerde MT
    val unit = stringResource(Res.string.unit_total)
    val levels = Level.entries.map { stringResource(levelRes(it)).uppercase() }
    val limit = stringResource(Res.string.at_limit).uppercase()
    val settling = stringResource(Res.string.settling).uppercase()
    Spacer(
        modifier.drawWithCache {
            val labelStyle = type.number.copy(fontSize = labelSp)
            // Skala klar als Abweichung: Mitte und Vollausschlag am rechten Bogenende
            val marks = listOf(ARCS / 2, ARCS).map { n ->
                val v = formatScale(sensitivity.fullScale * n / ARCS, sep)
                n to measurer.measure(if (n == ARCS) "±$v $unit" else "±$v", labelStyle)
            }
            val labelW = marks.maxOf { it.second.size.width } + 8.dp.toPx()
            val g = geo(size, labelW)
            val o = g.origin
            val r = g.r
            val w = g.stroke
            val track = Stroke(w, cap = StrokeCap.Round)
            val glow = Stroke(w * 1.9f, cap = StrokeCap.Round)
            val peakStroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
            val colors = List(ARCS) { i -> lerp(MagnrAccent, AccentLight, i / (ARCS - 1f)) }
            val labelAt = marks.map { (n, l) ->
                val end = o.polar(radiusAt(r, n - 0.5f), UP + SWEEP / 2)
                l to Offset(end.x + w / 2 + 6.dp.toPx(), end.y - l.size.height / 2f)
            }
            // Zahl: Groesse aus der Geometrie, unabhaengig von der Schriftgroesse
            val probe = measurer.measure("+888.8", type.display.copy(fontSize = 100.sp))
            val k = min(r * 0.3f / probe.size.height, r * 1.25f / probe.size.width)
            val numStyle = type.display.copy(fontSize = 100.sp * k)
            val unitLayout = measurer.measure(unit, type.number.copy(fontSize = unitSp, color = c.fgMuted))
            val lvMax = size.width * 0.9f
            val fit = { t: String ->
                val base = type.label.copy(fontSize = levelSp)
                val l = measurer.measure(t, base)
                if (l.size.width <= lvMax) l else measurer.measure(t, base.copy(fontSize = base.fontSize * (lvMax / l.size.width)))
            }
            val levelLayouts = levels.map(fit)
            val limitLayout = fit(limit)
            val settlingLayout = fit(settling)
            onDrawBehind {
                // je Messwert neu zeichnen, ohne Neukomposition
                state.tick
                val alpha = if (live) 1f else PAUSED_ALPHA
                val reading = state.reading
                val s = if (empty) 0f else if (reading.saturated) 1f else reading.strength
                if (!empty) drawGlow(MagnrAccent, o, r * (0.3f + 0.35f * s), GLOW_MAX * (0.4f + 0.6f * s) * alpha)
                // leere Boegen, darueber die leuchtenden von innen nach aussen
                for (i in 0 until ARCS) arc(c.surfaceHigh, radiusAt(r, i + 0.5f), o, SWEEP, track)
                labelAt.forEach { (l, at) -> drawText(l, c.fgMuted, at) }
                val lit = s * ARCS
                // erst alle Scheine, dann die Boegen: kein Schein liegt auf einem Nachbarbogen
                for (pass in 0..1) {
                    if (pass == 0 && !live) continue
                    for (i in 0 until ARCS) {
                        val fill = (lit - i).coerceIn(0f, 1f)
                        if (fill <= 0f) break
                        // aeusserster Bogen waechst von der Mitte zu den Seiten: sanfter Uebergang
                        val sweep = SWEEP * fill
                        val rr = radiusAt(r, i + 0.5f)
                        if (pass == 0) arc(colors[i], rr, o, sweep, glow, 0.12f) else arc(colors[i], rr, o, sweep, track, alpha)
                    }
                }
                // Spitze: duenner Bogen, wo der Ausschlag zuletzt am groessten war
                val peak = reading.peak
                if (!empty && !peak.isNaN()) {
                    val pf = sensitivity.strength(peak) * ARCS
                    if (pf > 0.05f) arc(AccentLight, radiusAt(r, pf) + w * 0.5f + 2.dp.toPx(), o, SWEEP, peakStroke, PEAK_ALPHA * alpha)
                }
                // Sensorpunkt
                val dotColor = if (empty) c.fgFaint else if (s > 0f) MagnrAccent else c.fgMuted
                drawCircle(dotColor.copy(alpha = alpha), w * 0.75f, o)
                if (empty) return@onDrawBehind

                // Ablesung: Abweichung mit Vorzeichen und Einheit, darunter die Stufe
                val shown = state.shown
                val text = if (shown.total.isNaN() || shown.settling) "–" else formatUt(shown.dev, sep, signed = true)
                val num = measurer.measure(text, numStyle)
                val gap = 6.dp.toPx()
                val total = num.size.width + gap + unitLayout.size.width
                val numTop = o.y + w * 0.75f + r * 0.08f
                val x0 = o.x - total / 2
                drawText(num, c.fg, Offset(x0, numTop))
                // Einheit auf der Grundlinie der Zahl
                val baseY = numTop + num.firstBaseline
                drawText(unitLayout, topLeft = Offset(x0 + num.size.width + gap, baseY - unitLayout.firstBaseline))
                if (shown.total.isNaN()) return@onDrawBehind
                val (label, dot) = when {
                    shown.saturated -> limitLayout to Warn
                    shown.settling -> settlingLayout to c.fgMuted
                    else -> levelLayouts[shown.level.ordinal] to (if (shown.level == Level.QUIET) c.fgMuted else MagnrAccent)
                }
                val dotR = 4.dp.toPx()
                val lw = dotR * 2 + 8.dp.toPx() + label.size.width
                val cy = numTop + num.size.height + label.size.height * 0.4f
                val lx = o.x - lw / 2
                drawCircle(dot.copy(alpha = alpha), dotR, Offset(lx + dotR, cy))
                drawText(label, c.fg, Offset(lx + dotR * 2 + 8.dp.toPx(), cy - label.size.height / 2f))
            }
        },
    )
}
