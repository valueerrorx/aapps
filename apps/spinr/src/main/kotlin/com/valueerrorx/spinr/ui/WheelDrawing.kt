package com.valueerrorx.spinr.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.valueerrorx.aapps.core.ui.AappsColors
import com.valueerrorx.aapps.core.ui.drawGlow
import com.valueerrorx.spinr.domain.Palette
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Pastell-Palette; Akzent der App ist die erste Farbe
val SpinrPalette = listOf(
    Color(0xFFB9A2FF), Color(0xFFFF9EC4), Color(0xFFFFB48A),
    Color(0xFFFFE08A), Color(0xFF8EE6B8), Color(0xFF8ACFFF),
)
private val LabelInk = Color(0xFF1B1726)
private val PegLight = Color(0xFFF7F9FB)
private val PegDark = Color(0xFFA9B4BF)

fun segmentColor(i: Int, n: Int): Color = SpinrPalette[Palette.colorIndex(i, n, SpinrPalette.size)]

// Farben eines vollen Rads mit n Eintraegen
fun segmentColors(n: Int): List<Color> = List(n) { segmentColor(it, n) }

// Punkt auf dem Kreis: Grad im Uhrzeigersinn ab oben
private fun polar(center: Offset, deg: Float, radius: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(center.x + (sin(a) * radius).toFloat(), center.y - (cos(a) * radius).toFloat())
}

// Lage des Rads im Hero: oben Platz fuer den Zeiger
class WheelGeometry(val center: Offset, val radius: Float) {
    val face: Float get() = radius * 0.9f
    val pegRadius: Float get() = radius * 0.95f
    val pointerPivot: Offset get() = Offset(center.x, center.y - radius - radius * 0.02f)
}

private const val POINTER_DP = 34f
private const val PAD_DP = 14f
private const val MAX_RADIUS_DP = 260f

fun wheelGeometry(size: Size, density: Float): WheelGeometry {
    val pointer = POINTER_DP * density
    val pad = PAD_DP * density
    val r = min(size.width / 2f - pad, (size.height - pointer - 2f * pad) / 2f).coerceIn(1f, MAX_RADIUS_DP * density)
    return WheelGeometry(Offset(size.width / 2f, size.height / 2f + pointer / 2f), r)
}

// Hoehe, die das Rad bei dieser Breite braucht (ohne Leerraum)
fun wheelHeight(width: Float, density: Float): Float {
    val pad = PAD_DP * density
    val r = (width / 2f - pad).coerceIn(1f, MAX_RADIUS_DP * density)
    return 2f * r + POINTER_DP * density + 2f * pad
}

// Verlierer: dunkler und entsaettigt, damit der Gewinner leuchtet
private fun dimmed(color: Color, c: AappsColors, t: Float): Color {
    if (t <= 0f) return color
    val l = color.luminance()
    val grey = Color(l, l, l)
    return lerp(lerp(color, grey, 0.45f * t), c.bg, 0.5f * t)
}

// Farbflaechen und Trennlinien; Verlierer treten bei pop zurueck
fun DrawScope.drawWheelFace(g: WheelGeometry, colors: List<Color>, winner: Int?, pop: Float, c: AappsColors) {
    val dp = density
    val center = g.center
    val rs = g.face
    val n = colors.size
    if (n == 0) {
        drawCircle(c.surfaceHigh, rs, center)
        return
    }
    val seg = 360f / n
    val topLeft = center - Offset(rs, rs)
    val box = Size(rs * 2f, rs * 2f)
    val dim = if (winner != null && winner in 0 until n) pop.coerceIn(0f, 1f) else 0f
    for (i in 0 until n) {
        val color = if (i == winner) colors[i] else dimmed(colors[i], c, dim)
        val brush = Brush.radialGradient(
            0f to lerp(color, Color.White, 0.2f), 0.55f to color, 1f to lerp(color, Color.Black, 0.1f),
            center = center, radius = rs,
        )
        // kleine Ueberlappung gegen Haarlinien zwischen den Segmenten
        drawArc(brush, -90f + i * seg - 0.3f, seg + 0.6f, true, topLeft, box)
    }
    if (n > 1) {
        for (i in 0 until n) {
            drawLine(Color.Black.copy(alpha = 0.22f), center, polar(center, i * seg, rs), strokeWidth = 1.2f * dp)
        }
    }
}

// Bewegungsunschaerfe: Segmentfarben fliessend ineinander, mit derselben Tiefe wie die Segmente
fun DrawScope.drawWheelBlur(g: WheelGeometry, colors: List<Color>) {
    val n = colors.size
    if (n < 2) return
    val center = g.center
    val rs = g.face
    // Stopps in den Segmentmitten, Naht zwischen letztem und erstem Segment geschlossen
    val seam = lerp(colors[n - 1], colors[0], 0.5f)
    val stops = buildList {
        add(0f to seam)
        colors.forEachIndexed { i, col -> add((i + 0.5f) / n to col) }
        add(1f to seam)
    }.toTypedArray()
    // sweepGradient beginnt bei 3 Uhr, das Rad bei 12 Uhr
    rotate(-90f, center) {
        drawCircle(Brush.sweepGradient(*stops, center = center), rs, center)
    }
    drawCircle(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.18f), 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.12f),
            center = center, radius = rs,
        ),
        rs, center,
    )
}

// Beschriftung radial, Ende knapp vor dem Rand; mindestens 9 sp, bei vielen Eintraegen weiter aussen
fun DrawScope.drawWheelLabels(
    g: WheelGeometry,
    labels: List<String>,
    measurer: TextMeasurer,
    textStyle: TextStyle,
    winner: Int?,
    pop: Float,
) {
    val n = labels.size
    if (n == 0) return
    val center = g.center
    val rs = g.face
    val seg = 360f / n
    val line = 1.3f // Zeilenhoehe je Schriftgroesse
    val end = rs * 0.88f
    // gewuenscht: folgt Rad und Systemschrift (gedaempft); mindestens 9 sp
    val want = min(rs * 0.115f, 2f * PI.toFloat() * rs * 0.62f / n * 0.5f) * sqrt(fontScale)
    // hoechstens so gross, dass ausserhalb der noetigen Segmentbreite noch ~3 Zeichen Platz haben
    val fit = end / (line * n / (2f * PI.toFloat()) + 3f)
    val fontPx = min(min(max(want, 9.sp.toPx()), fit), rs * 0.14f)
    // Text beginnt erst, wo das Segment breit genug ist, und nie an der Nabe
    val start = max(rs * 0.28f, line * fontPx * n / (2f * PI.toFloat()))
    val maxWidth = (end - start).toInt().coerceAtLeast(1)
    val style = textStyle.copy(color = LabelInk, fontSize = fontPx.toSp())
    val dim = if (winner != null && winner in 0 until n) pop.coerceIn(0f, 1f) else 0f
    labels.forEachIndexed { i, text ->
        val layout = measurer.measure(
            text, style, overflow = TextOverflow.Ellipsis, maxLines = 1, softWrap = false,
            constraints = Constraints(maxWidth = maxWidth),
        )
        val alpha = if (i == winner) 1f else 1f - 0.4f * dim
        rotate(-90f + (i + 0.5f) * seg, center) {
            drawText(layout, topLeft = Offset(center.x + end - layout.size.width, center.y - layout.size.height / 2f), alpha = alpha)
        }
    }
}

// Ueber der Schrift: helle Kante am Gewinner, Stifte
fun DrawScope.drawWheelDetails(g: WheelGeometry, n: Int, winner: Int?, pop: Float) {
    val dp = density
    val center = g.center
    val rs = g.face
    if (n == 0) return
    val seg = 360f / n
    val topLeft = center - Offset(rs, rs)
    val box = Size(rs * 2f, rs * 2f)

    if (winner != null && winner in 0 until n && pop > 0f) {
        val inset = 1.5f * dp
        drawArc(
            Color.White.copy(alpha = 0.85f * pop.coerceIn(0f, 1f)), -90f + winner * seg, seg, true,
            topLeft + Offset(inset, inset), Size(box.width - 2 * inset, box.height - 2 * inset),
            style = Stroke(2.5f * dp),
        )
    }

    // Stifte an den Grenzen, auf dem Rand
    val pr = min(g.radius * 0.022f, 2f * PI.toFloat() * g.pegRadius / n * 0.2f)
    if (n > 1) {
        for (i in 0 until n) {
            val p = polar(center, i * seg, g.pegRadius)
            drawCircle(Color.Black.copy(alpha = 0.45f), pr * 1.15f, p + Offset(0f, pr * 0.4f))
            drawCircle(
                Brush.radialGradient(0f to PegLight, 1f to PegDark, center = p - Offset(pr * 0.35f, pr * 0.35f), radius = pr * 1.4f),
                pr, p,
            )
        }
    }
}

// Unten, fest: Lichthof, Schatten, Gehaeuse
fun DrawScope.drawWheelBase(g: WheelGeometry, c: AappsColors, glow: Color, pop: Float) {
    val dp = density
    val r = g.radius
    val center = g.center
    drawGlow(glow, center, r * 1.45f, 0.09f + 0.14f * pop)
    for (i in 1..4) {
        drawCircle(Color.Black.copy(alpha = 0.12f), r + i * 1.5f * dp, center + Offset(0f, i * 3.5f * dp))
    }
    drawCircle(
        Brush.radialGradient(
            0f to c.surfaceHigh, 0.7f to c.surface, 1f to c.bg,
            center = center - Offset(r * 0.2f, r * 0.25f), radius = r * 1.3f,
        ),
        r, center,
    )
    drawCircle(c.line, r - 0.75f * dp, center, style = Stroke(1.5f * dp))
    drawCircle(Color.Black.copy(alpha = 0.4f), g.face + 1.5f * dp, center, style = Stroke(3f * dp))
}

// Oben, fest: Tiefe, Glas, Nabe
fun DrawScope.drawWheelGlass(g: WheelGeometry, c: AappsColors, hub: Color) {
    val dp = density
    val center = g.center
    val rs = g.face
    val r = g.radius
    drawCircle(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.12f), 0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.32f),
            center = center - Offset(rs * 0.28f, rs * 0.32f), radius = rs * 1.3f,
        ),
        rs, center,
    )
    val rr = rs - 9f * dp
    drawArc(
        Color.White.copy(alpha = 0.16f), 200f, 52f, false,
        center - Offset(rr, rr), Size(rr * 2f, rr * 2f),
        style = Stroke(3f * dp, cap = StrokeCap.Round),
    )
    // Nabe
    val hr = r * 0.12f
    drawCircle(Color.Black.copy(alpha = 0.35f), hr * 1.08f, center + Offset(0f, hr * 0.18f))
    drawCircle(
        Brush.radialGradient(0f to c.surfaceHigh, 1f to c.bg, center = center - Offset(hr * 0.3f, hr * 0.4f), radius = hr * 1.3f),
        hr, center,
    )
    drawCircle(hub, hr, center, style = Stroke(2f * dp))
    drawGlow(hub, center, hr * 0.9f, 0.22f)
    drawCircle(Color.White.copy(alpha = 0.55f), hr * 0.16f, center - Offset(hr * 0.32f, hr * 0.32f))
}

// Zeiger mit Zunge: haengt oben, die Stifte lenken ihn aus (flap -1..1)
fun DrawScope.drawPointer(g: WheelGeometry, c: AappsColors, flap: Float, tint: Color, pop: Float) {
    val dp = density
    val pivot = g.pointerPivot
    val pb = (g.radius * 0.07f).coerceAtLeast(11f * dp)
    // Spitze reicht knapp ins Segment, ohne die Beschriftung zu verdecken
    val tip = Offset(pivot.x, g.center.y - g.face * 0.95f)
    val d = tip.y - pivot.y
    if (d <= pb) return
    val phi = Math.toDegrees(acos((pb / d).toDouble())).toFloat()
    val oval = Rect(pivot, pb)
    val body = Path().apply {
        moveTo(tip.x, tip.y)
        val a = Math.toRadians((90f + phi).toDouble())
        lineTo(pivot.x + (cos(a) * pb).toFloat(), pivot.y + (sin(a) * pb).toFloat())
        arcTo(oval, 90f + phi, 360f - 2f * phi, false)
        close()
    }
    rotate(-flap * 22f, pivot) {
        drawGlow(tint, pivot, pb * 3.2f, 0.28f + 0.25f * pop)
        translate(0f, 3f * dp) { drawPath(body, Color.Black.copy(alpha = 0.4f)) }
        drawPath(
            body,
            Brush.linearGradient(
                0f to Color(0xFFFBFAFF), 1f to Color(0xFFC9C4DA),
                start = pivot - Offset(pb, pb), end = tip,
            ),
        )
        drawPath(body, c.bg.copy(alpha = 0.35f), style = Stroke(1f * dp))
        drawCircle(tint, pb * 0.42f, pivot)
        drawCircle(Color.White.copy(alpha = 0.6f), pb * 0.13f, pivot - Offset(pb * 0.14f, pb * 0.14f))
    }
}
