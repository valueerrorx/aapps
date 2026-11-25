package solutions.xapient.thinkr.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.domain.echo.Glyph
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val ThinkrAccent = Color(0xFFF27BE8)

// System-Animationen aus (Skala 0): Deko-Bewegung wie Atmen, Wellen, Wackeln bleibt stehen
object Motion {
    var reduced = false
    fun read(context: Context) {
        reduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    fun still(v: Float): Float = if (reduced) 0f else v
}

// Warnung laut DESIGN.md, nur fuer Fehler und knappe Frist
val Amber = Color(0xFFF2B84E)

// Glyphen: helles Glas mit Hauch von Akzent
private val GlyphTop = Color(0xFFFFFFFF)
private val GlyphBottom = Color(0xFFF7C4F1)

fun formatPoints(n: Int, locale: Locale = Locale.getDefault()): String = NumberFormat.getIntegerInstance(locale).format(n)

// Leucht-Glyphe: nur die Form zaehlt, alle in derselben Farbe
fun DrawScope.drawGlyph(g: Glyph, center: Offset, size: Float, glow: Color = ThinkrAccent, alpha: Float = 1f) {
    val r = size / 2f
    drawGlow(glow, center, size * 0.95f, 0.30f * alpha)
    val fill = Brush.verticalGradient(
        listOf(GlyphTop.copy(alpha = alpha), GlyphBottom.copy(alpha = alpha)),
        startY = center.y - r, endY = center.y + r,
    )
    val stroke = Stroke(width = r * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (g) {
        Glyph.CIRCLE -> drawCircle(fill, r * 0.74f, center)
        Glyph.RING -> drawCircle(fill, r * 0.66f, center, style = stroke)
        Glyph.TRIANGLE -> drawPath(polygon(center.copy(y = center.y + r * 0.16f), r * 0.96f, 3, -PI / 2), fill)
        Glyph.DIAMOND -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - r * 0.94f)
                lineTo(center.x + r * 0.66f, center.y)
                lineTo(center.x, center.y + r * 0.94f)
                lineTo(center.x - r * 0.66f, center.y)
                close()
            },
            fill,
        )
        Glyph.STAR -> drawPath(star(center.copy(y = center.y + r * 0.06f), r * 0.98f, r * 0.42f), fill)
        Glyph.WAVE -> drawPath(
            Path().apply {
                val w = r * 1.5f
                val amp = r * 0.34f
                val n = 48
                for (i in 0..n) {
                    val x = center.x - w / 2 + w * i / n
                    val y = center.y - amp * sin(2 * PI * 1.25 * i / n - PI / 4).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            },
            fill, style = stroke,
        )
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

// Glas: dunkle Fuellung, feine Kante, heller Reflexstreifen oben
fun DrawScope.drawGlass(topLeft: Offset, size: Size, corner: Float, edge: Color, edgeWidth: Float = 1.5.dp.toPx(), fill: Color = Color(0xFF1E2932)) {
    val cr = CornerRadius(corner, corner)
    drawRoundRect(
        Brush.verticalGradient(listOf(fill, fill.copy(alpha = 0.72f)), startY = topLeft.y, endY = topLeft.y + size.height),
        topLeft, size, cr,
    )
    val half = edgeWidth / 2
    drawRoundRect(
        edge, Offset(topLeft.x + half, topLeft.y + half), Size(size.width - edgeWidth, size.height - edgeWidth),
        CornerRadius(corner - half, corner - half), style = Stroke(edgeWidth),
    )
    val y = topLeft.y + 2.5.dp.toPx() + edgeWidth
    drawLine(
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
            startX = topLeft.x + corner * 0.6f, endX = topLeft.x + size.width - corner * 0.6f,
        ),
        Offset(topLeft.x + corner * 0.6f, y), Offset(topLeft.x + size.width - corner * 0.6f, y),
        strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round,
    )
}

// Linien-Icons nur fuer thinkr, gleicher Strich wie AappsIcons
object ThinkrIcons {
    val Pause: ImageVector by lazy {
        line("pause") { moveTo(9f, 6.5f); lineTo(9f, 17.5f); moveTo(15f, 6.5f); lineTo(15f, 17.5f) }
    }

    val Play: ImageVector by lazy {
        line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() }
    }

    val Games: ImageVector by lazy {
        line("games") {
            rect(4.5f, 4.5f, 10.5f, 10.5f)
            rect(13.5f, 4.5f, 19.5f, 10.5f)
            rect(4.5f, 13.5f, 10.5f, 19.5f)
            rect(13.5f, 13.5f, 19.5f, 19.5f)
        }
    }

    // Echo: Karte mit Glyphe, dahinter ihr Nachhall
    val Echo: ImageVector by lazy {
        line("echo") {
            moveTo(9f, 7.5f); lineTo(9f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 15f); lineTo(16f, 15f)
            rect(4.5f, 8.5f, 15f, 19.5f)
            moveTo(9.75f, 11.6f); lineTo(12.3f, 16.2f); lineTo(7.2f, 16.2f); close()
        }
    }

    // Weichen: Abzweig, am Ende ein Tor
    val Switches: ImageVector by lazy {
        line("switches") {
            moveTo(12f, 20.5f); lineTo(12f, 14f); lineTo(6.5f, 8.5f); lineTo(6.5f, 4f)
            moveTo(12f, 14f); lineTo(17.5f, 8.5f)
            moveTo(17.5f, 8.5f); arcTo(2.3f, 2.3f, 0f, false, true, 17.5f, 3.9f); arcTo(2.3f, 2.3f, 0f, false, true, 17.5f, 8.5f)
        }
    }

    // Sternbild: drei Sterne, durch Linien verbunden, einer als Funke
    val Stars: ImageVector by lazy {
        line("stars") {
            moveTo(7.6f, 15.4f); lineTo(10.6f, 9.4f)
            moveTo(13.2f, 8.6f); lineTo(16.6f, 10.6f)
            dot(6.5f, 17.5f, 2f)
            dot(11.5f, 7.5f, 1.8f)
            moveTo(18.5f, 9.5f); lineTo(19.3f, 11.7f); lineTo(21.5f, 12.5f); lineTo(19.3f, 13.3f)
            lineTo(18.5f, 15.5f); lineTo(17.7f, 13.3f); lineTo(15.5f, 12.5f); lineTo(17.7f, 11.7f); close()
        }
    }

    // Tinte: Tropfen mit Glanzlinie
    val Ink: ImageVector by lazy {
        line("ink") {
            moveTo(12f, 3.5f)
            curveTo(12f, 3.5f, 5.5f, 10.5f, 5.5f, 14.5f)
            arcTo(6.5f, 6.5f, 0f, false, false, 18.5f, 14.5f)
            curveTo(18.5f, 10.5f, 12f, 3.5f, 12f, 3.5f)
            close()
            moveTo(9f, 15f); arcTo(3f, 3f, 0f, false, false, 11.5f, 17.8f)
        }
    }

    // Schwarm: grosser Pfeil in der Mitte, zwei kleine daneben
    val Swarm: ImageVector by lazy {
        line("swarm") {
            moveTo(7.5f, 12.5f); lineTo(12f, 7.5f); lineTo(16.5f, 12.5f)
            moveTo(12f, 7.5f); lineTo(12f, 17.5f)
            moveTo(2.8f, 18.2f); lineTo(5.5f, 15.2f); lineTo(8.2f, 18.2f)
            moveTo(15.8f, 18.2f); lineTo(18.5f, 15.2f); lineTo(21.2f, 18.2f)
        }
    }

    // Bande: Lichtweg, der an einem Prisma um 90° abknickt
    val Bounce: ImageVector by lazy {
        line("bounce") {
            moveTo(6.1f, 15.5f); lineTo(14.5f, 15.5f); lineTo(14.5f, 5f)
            moveTo(11.5f, 18.5f); lineTo(17.5f, 12.5f)
            moveTo(12.3f, 7.2f); lineTo(14.5f, 5f); lineTo(16.7f, 7.2f)
            dot(4.5f, 15.5f, 1.6f)
        }
    }

    // Tropfen: kleiner Tropfen ueber einer Welle, daneben ein Plus
    val Drops: ImageVector by lazy {
        line("drops") {
            moveTo(10f, 3.5f)
            curveTo(10f, 3.5f, 6f, 8f, 6f, 10.5f)
            arcTo(4f, 4f, 0f, false, false, 14f, 10.5f)
            curveTo(14f, 8f, 10f, 3.5f, 10f, 3.5f)
            close()
            moveTo(17.5f, 4.5f); lineTo(17.5f, 9.5f); moveTo(15f, 7f); lineTo(20f, 7f)
            moveTo(3.5f, 19f); curveTo(6f, 16.8f, 8.5f, 16.8f, 11f, 19f); curveTo(13.5f, 21.2f, 16f, 21.2f, 20.5f, 18.2f)
        }
    }

    // Rundblick: Ring um einen Punkt, am Ring ein Funke
    val Glance: ImageVector by lazy {
        line("glance") {
            moveTo(12f, 4f); arcTo(8f, 8f, 0f, true, false, 20f, 12f)
            dot(12f, 12f, 1.6f)
            moveTo(18.5f, 2.8f); lineTo(19.2f, 4.8f); lineTo(21.2f, 5.5f); lineTo(19.2f, 6.2f)
            lineTo(18.5f, 8.2f); lineTo(17.8f, 6.2f); lineTo(15.8f, 5.5f); lineTo(17.8f, 4.8f); close()
        }
    }

    // Slalom: Gleiter zwischen zwei Kristallen
    val Slalom: ImageVector by lazy {
        line("slalom") {
            moveTo(12f, 9f); lineTo(15.5f, 18.5f); lineTo(12f, 16.5f); lineTo(8.5f, 18.5f); close()
            moveTo(4.5f, 13f); lineTo(6f, 5.5f); lineTo(7.5f, 13f)
            moveTo(16.5f, 10.5f); lineTo(18f, 3.5f); lineTo(19.5f, 10.5f)
        }
    }

    // Loeschen: Taste mit Pfeil nach links
    val Erase: ImageVector by lazy {
        line("erase") {
            moveTo(9f, 6f); lineTo(19.5f, 6f); lineTo(19.5f, 18f); lineTo(9f, 18f); lineTo(3.5f, 12f); close()
            moveTo(11.5f, 9.5f); lineTo(16.5f, 14.5f); moveTo(16.5f, 9.5f); lineTo(11.5f, 14.5f)
        }
    }

    val Yes: ImageVector by lazy {
        line("yes") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
    }

    val No: ImageVector by lazy {
        line("no") { moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f); moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f) }
    }

    val Up: ImageVector by lazy {
        line("up") { moveTo(12f, 19f); lineTo(12f, 5.5f); moveTo(6.5f, 11f); lineTo(12f, 5.5f); lineTo(17.5f, 11f) }
    }

    val Down: ImageVector by lazy {
        line("down") { moveTo(12f, 5f); lineTo(12f, 18.5f); moveTo(6.5f, 13f); lineTo(12f, 18.5f); lineTo(17.5f, 13f) }
    }

    val Same: ImageVector by lazy {
        line("same") { moveTo(5.5f, 12f); lineTo(18.5f, 12f) }
    }

    // Funke: Tagestraining erledigt
    val Spark: ImageVector by lazy {
        line("spark") {
            moveTo(12f, 3.5f); lineTo(13.8f, 10.2f); lineTo(20.5f, 12f); lineTo(13.8f, 13.8f)
            lineTo(12f, 20.5f); lineTo(10.2f, 13.8f); lineTo(3.5f, 12f); lineTo(10.2f, 10.2f); close()
        }
    }

    private fun PathBuilder.rect(l: Float, t: Float, r: Float, b: Float) {
        val k = 1.6f
        moveTo(l + k, t); lineTo(r - k, t); quadTo(r, t, r, t + k); lineTo(r, b - k); quadTo(r, b, r - k, b)
        lineTo(l + k, b); quadTo(l, b, l, b - k); lineTo(l, t + k); quadTo(l, t, l + k, t); close()
    }

    // kleiner Kreis als Strichpfad
    private fun PathBuilder.dot(x: Float, y: Float, r: Float) {
        moveTo(x - r, y); arcTo(r, r, 0f, true, true, x + r, y); arcTo(r, r, 0f, true, true, x - r, y); close()
    }

    private fun line(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()
}
