package solutions.xapient.hudr.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Strichschrift wie im Flugzeug-HUD: abgeschraegte Ecken, gleich breite Ziffern, "1" mit Fuss.
 * Glyphen als Linienzuege im Feld x 0..1, y 0..1 (oben 0).
 */
object HudFont {
    private val GLYPHS: Map<Char, List<FloatArray>> = mapOf(
        '0' to listOf(f(0.22f, 0f, 0.78f, 0f, 1f, 0.12f, 1f, 0.88f, 0.78f, 1f, 0.22f, 1f, 0f, 0.88f, 0f, 0.12f, 0.22f, 0f)),
        '1' to listOf(f(0.18f, 0.2f, 0.56f, 0f, 0.56f, 1f), f(0.16f, 1f, 0.94f, 1f)),
        '2' to listOf(f(0f, 0.14f, 0.2f, 0f, 0.8f, 0f, 1f, 0.14f, 1f, 0.38f, 0f, 1f, 1f, 1f)),
        '3' to listOf(f(0f, 0f, 1f, 0f, 0.42f, 0.42f, 0.8f, 0.42f, 1f, 0.56f, 1f, 0.86f, 0.8f, 1f, 0.2f, 1f, 0f, 0.86f)),
        '4' to listOf(f(0.72f, 1f, 0.72f, 0f, 0f, 0.68f, 1f, 0.68f)),
        '5' to listOf(f(1f, 0f, 0.06f, 0f, 0f, 0.46f, 0.78f, 0.4f, 1f, 0.55f, 1f, 0.86f, 0.8f, 1f, 0.2f, 1f, 0f, 0.86f)),
        '6' to listOf(f(0.86f, 0f, 0.36f, 0f, 0f, 0.42f, 0f, 0.86f, 0.2f, 1f, 0.8f, 1f, 1f, 0.86f, 1f, 0.6f, 0.8f, 0.46f, 0.2f, 0.46f, 0f, 0.58f)),
        '7' to listOf(f(0f, 0f, 1f, 0f, 1f, 0.12f, 0.4f, 1f)),
        '8' to listOf(
            f(0.22f, 0f, 0.78f, 0f, 0.95f, 0.11f, 0.95f, 0.35f, 0.78f, 0.46f, 0.22f, 0.46f, 0.05f, 0.35f, 0.05f, 0.11f, 0.22f, 0f),
            f(0.22f, 0.46f, 0f, 0.58f, 0f, 0.88f, 0.2f, 1f, 0.8f, 1f, 1f, 0.88f, 1f, 0.58f, 0.78f, 0.46f),
        ),
        '9' to listOf(f(0.14f, 1f, 0.64f, 1f, 1f, 0.58f, 1f, 0.14f, 0.8f, 0f, 0.2f, 0f, 0f, 0.14f, 0f, 0.4f, 0.2f, 0.54f, 0.8f, 0.54f, 1f, 0.42f)),
        '–' to listOf(f(0.28f, 0.5f, 0.72f, 0.5f)),
        '.' to listOf(f(0.5f, 0.98f, 0.5f, 1f)),
        ',' to listOf(f(0.56f, 0.92f, 0.4f, 1.12f)),
    )

    private fun f(vararg v: Float) = v

    // Satzzeichen schmal, alles andere eine Zelle
    private fun narrow(c: Char) = c == '.' || c == ','

    fun advance(c: Char, h: Float, aspect: Float, gap: Float): Float = h * (if (narrow(c)) 0.3f else aspect + gap)

    fun width(text: String, h: Float, aspect: Float = ASPECT, gap: Float = GAP): Float =
        text.sumOf { advance(it, h, aspect, gap).toDouble() }.toFloat() - if (text.isEmpty()) 0f else h * gap

    // Linienzug des Texts ab (0, 0), Hoehe h
    fun path(text: String, h: Float, aspect: Float = ASPECT, gap: Float = GAP): Path {
        val p = Path()
        var x = 0f
        text.forEach { c ->
            val w = if (narrow(c)) h * 0.3f else h * aspect
            GLYPHS[c]?.forEach { line ->
                for (i in line.indices step 2) {
                    val px = x + line[i] * w
                    val py = line[i + 1] * h
                    if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                }
            }
            x += advance(c, h, aspect, gap)
        }
        return p
    }

    // Strich mit optionalem Lichthof (nicht im HUD: der streut in der Scheibe)
    fun DrawScope.drawHud(text: String, topLeft: Offset, h: Float, color: Color, glow: Boolean, aspect: Float = ASPECT, weight: Float = WEIGHT) {
        val p = path(text, h, aspect)
        val w = h * weight
        translate(topLeft.x, topLeft.y) {
            if (glow) drawPath(p, color, alpha = GLOW_ALPHA, style = Stroke(w * GLOW_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(p, color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    // Lichthof: Zahl und Einheit bleiben darauf >= 4,5:1 (ContrastTest)
    const val GLOW_ALPHA = 0.12f
    // Lichthof-Breite relativ zum Strich
    const val GLOW_WIDTH = 2f
    const val ASPECT = 0.5f
    const val GAP = 0.16f
    const val WEIGHT = 0.11f
}
