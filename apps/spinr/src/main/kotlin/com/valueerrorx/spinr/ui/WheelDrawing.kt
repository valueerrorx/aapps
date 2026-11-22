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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.valueerrorx.aapps.core.ui.AappsColors
import com.valueerrorx.aapps.core.ui.drawGlow
import com.valueerrorx.spinr.domain.Palette
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Pastell-Palette; Akzent der App ist die erste Farbe
val SpinrPalette = listOf(
    Color(0xFFB9A2FF), Color(0xFFFF9EC4), Color(0xFFFFB48A),
    Color(0xFFFFE08A), Color(0xFF8EE6B8), Color(0xFF8ACFFF),
)
private val LabelInk = Color(0xFF1B1726)
private val PegLight = Color(0xFFF7F9FB)
private val PegDark = Color(0xFFA9B4BF)

fun segmentColor(i: Int, n: Int): Color = SpinrPalette[Palette.colorIndex(i, n, SpinrPalette.size)]

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

fun wheelGeometry(size: Size, density: Float): WheelGeometry {
    val pointer = 34f * density
    val pad = 14f * density
    val r = min(size.width / 2f - pad, (size.height - pointer - 2f * pad) / 2f).coerceIn(1f, 260f * density)
    return WheelGeometry(Offset(size.width / 2f, size.height / 2f + pointer / 2f), r)
}

// Was das Gesicht des Rads ausser den Eintraegen braucht
class FaceLook(val winner: Int?, val pop: Float)

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

// Nur die Farbflaechen; auch fuer die nachgezogenen Unschaerfe-Kopien
fun DrawScope.drawWheelColors(g: WheelGeometry, n: Int, c: AappsColors) {
    val center = g.center
    val rs = g.face
    if (n == 0) {
        drawCircle(c.surfaceHigh, rs, center)
        return
    }
    val seg = 360f / n
    val topLeft = center - Offset(rs, rs)
    val box = Size(rs * 2f, rs * 2f)
    for (i in 0 until n) {
        val color = segmentColor(i, n)
        val brush = Brush.radialGradient(
            0f to lerp(color, Color.White, 0.2f), 0.55f to color, 1f to lerp(color, Color.Black, 0.1f),
            center = center, radius = rs,
        )
        // kleine Ueberlappung gegen Haarlinien zwischen den Segmenten
        drawArc(brush, -90f + i * seg - 0.3f, seg + 0.6f, true, topLeft, box)
    }
}

// Beschriftung radial, Ende knapp vor dem Rand; eigene Ebene, damit sie bei Tempo verblassen kann
fun DrawScope.drawWheelLabels(g: WheelGeometry, labels: List<String>, measurer: TextMeasurer, textStyle: TextStyle) {
    val n = labels.size
    if (n == 0) return
    val center = g.center
    val rs = g.face
    val seg = 360f / n
    val arcAt = 2f * PI.toFloat() * rs * 0.62f / n
    val fontPx = min(rs * 0.115f, arcAt * 0.5f)
    val maxWidth = (rs * 0.6f).toInt().coerceAtLeast(1)
    val style = textStyle.copy(color = LabelInk, fontSize = fontPx.toSp())
    labels.forEachIndexed { i, text ->
        val layout = measurer.measure(
            text, style, overflow = TextOverflow.Ellipsis, maxLines = 1, softWrap = false,
            constraints = Constraints(maxWidth = maxWidth),
        )
        rotate(-90f + (i + 0.5f) * seg, center) {
            drawText(layout, topLeft = Offset(center.x + rs * 0.88f - layout.size.width, center.y - layout.size.height / 2f))
        }
    }
}

// Ueber den Farben: Ergebnis-Hervorhebung, Trennlinien, Stifte
fun DrawScope.drawWheelDetails(g: WheelGeometry, n: Int, look: FaceLook, c: AappsColors) {
    val dp = density
    val center = g.center
    val rs = g.face
    if (n == 0) return
    val seg = 360f / n
    val topLeft = center - Offset(rs, rs)
    val box = Size(rs * 2f, rs * 2f)

    // Ergebnis: die anderen treten zurueck, der Gewinner bekommt eine helle Kante
    val w = look.winner
    if (w != null && w in 0 until n && look.pop > 0f) {
        for (i in 0 until n) {
            if (i != w) drawArc(c.bg.copy(alpha = 0.55f * look.pop), -90f + i * seg - 0.3f, seg + 0.6f, true, topLeft, box)
        }
        val inset = 1.5f * dp
        drawArc(
            Color.White.copy(alpha = 0.85f * look.pop), -90f + w * seg, seg, true,
            topLeft + Offset(inset, inset), Size(box.width - 2 * inset, box.height - 2 * inset),
            style = Stroke(2.5f * dp),
        )
    }

    // Trennlinien
    if (n > 1) {
        for (i in 0 until n) {
            drawLine(Color.Black.copy(alpha = 0.22f), center, polar(center, i * seg, rs), strokeWidth = 1.2f * dp)
        }
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
