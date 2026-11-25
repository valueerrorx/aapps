package solutions.xapient.thinkr.ui.switches

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Jede Farbe hat ihre eigene Form: Perle und Tor tragen beide, nie nur Farbe
enum class Mark { CIRCLE, TRIANGLE, SQUARE, DIAMOND, STAR, HEXAGON, CROSS, CRESCENT, HEART, RING }

data class Tint(val color: Color, val mark: Mark)

// Okabe-Ito plus Helligkeitsstufen, auf dunklem Grund aufgehellt; die ersten drei trennen sich am staerksten
val SwitchPalette = listOf(
    Tint(Color(0xFF56B4E9), Mark.CIRCLE),
    Tint(Color(0xFFE69F00), Mark.TRIANGLE),
    Tint(Color(0xFFF0E442), Mark.SQUARE),
    Tint(Color(0xFF2BC48F), Mark.DIAMOND),
    Tint(Color(0xFFCC79A7), Mark.STAR),
    Tint(Color(0xFFE8EDF2), Mark.HEXAGON),
    Tint(Color(0xFFE2652A), Mark.CROSS),
    Tint(Color(0xFF4A8FE0), Mark.CRESCENT),
    Tint(Color(0xFF9FE3D2), Mark.HEART),
    Tint(Color(0xFFC9A86A), Mark.RING),
)

// Form um c, r = halbe Ausdehnung
fun DrawScope.drawMark(mark: Mark, c: Offset, r: Float, color: Color) {
    when (mark) {
        Mark.CIRCLE -> drawCircle(color, r * 0.84f, c)
        Mark.TRIANGLE -> drawPath(polygon(Offset(c.x, c.y + r * 0.14f), r * 1.02f, 3, -PI / 2), color)
        Mark.SQUARE -> {
            val h = r * 0.74f
            drawRoundRect(color, Offset(c.x - h, c.y - h), Size(2 * h, 2 * h), CornerRadius(r * 0.16f))
        }
        Mark.DIAMOND -> drawPath(
            Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r * 0.74f, c.y)
                lineTo(c.x, c.y + r)
                lineTo(c.x - r * 0.74f, c.y)
                close()
            },
            color,
        )
        Mark.STAR -> drawPath(star(Offset(c.x, c.y + r * 0.06f), r * 1.04f, r * 0.46f), color)
        Mark.HEXAGON -> drawPath(polygon(c, r * 0.92f, 6, 0.0), color)
        Mark.CROSS -> {
            val l = r * 0.9f
            val t = r * 0.3f
            val k = CornerRadius(r * 0.1f)
            drawPath(
                Path().apply {
                    addRoundRect(RoundRect(Rect(c.x - l, c.y - t, c.x + l, c.y + t), k))
                    addRoundRect(RoundRect(Rect(c.x - t, c.y - l, c.x + t, c.y + l), k))
                },
                color,
            )
        }
        Mark.CRESCENT -> {
            val outer = Path().apply { addOval(Rect(c, r * 0.9f)) }
            val bite = Path().apply { addOval(Rect(Offset(c.x + r * 0.42f, c.y - r * 0.24f), r * 0.72f)) }
            drawPath(Path().apply { op(outer, bite, PathOperation.Difference) }, color)
        }
        Mark.HEART -> drawPath(
            Path().apply {
                val y = c.y + r * 0.08f
                moveTo(c.x, y + r * 0.82f)
                cubicTo(c.x - r * 1.3f, y - r * 0.1f, c.x - r * 0.72f, y - r * 1.2f, c.x, y - r * 0.5f)
                cubicTo(c.x + r * 0.72f, y - r * 1.2f, c.x + r * 1.3f, y - r * 0.1f, c.x, y + r * 0.82f)
                close()
            },
            color,
        )
        Mark.RING -> drawCircle(color, r * 0.68f, c, style = Stroke(r * 0.34f))
    }
}

private fun polygon(c: Offset, r: Float, n: Int, start: Double): Path = Path().apply {
    for (i in 0 until n) {
        val a = start + 2 * PI * i / n
        val x = c.x + r * cos(a).toFloat()
        val y = c.y + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun star(c: Offset, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val a = -PI / 2 + PI * i / 5
        val r = if (i % 2 == 0) outer else inner
        val x = c.x + r * cos(a).toFloat()
        val y = c.y + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
