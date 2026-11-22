package solutions.xapient.hertzr.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import solutions.xapient.hertzr.R
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Oscillator
import solutions.xapient.hertzr.domain.Wave

// Violett: Akzent nur fuer Rad, Welle und Hauptaktion
val HertzrAccent = Color(0xFF9D8CFF)
// Bernstein laut DESIGN.md: hoher Pegel
val Amber = Color(0xFFF2B84E)

// Lichthof hinter dem Rad: gestoppt kaum, spielend etwas mehr; Text bleibt >= 4,5:1 (ContrastTest)
const val GLOW_IDLE = 0.05f
const val GLOW_PLAY = 0.14f
// Welle im Leerlauf gedaempft, beim Spielen voll
const val WAVE_IDLE_ALPHA = 0.6f

@StringRes
fun waveName(w: Wave): Int = when (w) {
    Wave.SINE -> R.string.wave_sine
    Wave.SQUARE -> R.string.wave_square
    Wave.TRIANGLE -> R.string.wave_triangle
    Wave.SAW -> R.string.wave_saw
}

// "440 Hz", "1.5 kHz"
fun hzText(hz: Double, decimal: Char): String = Freq.short(hz, decimal)

// Linien-Icons nur fuer hertzr, gleicher Strich wie AappsIcons
object HertzrIcons {
    val Play: ImageVector by lazy { line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() } }
    val Stop: ImageVector by lazy { line("stop") { moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close() } }
    val Plus: ImageVector by lazy { line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Minus: ImageVector by lazy { line("minus") { moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Arrow: ImageVector by lazy { line("arrow") { moveTo(5f, 12f); lineTo(19f, 12f); moveTo(14f, 7f); lineTo(19f, 12f); lineTo(14f, 17f) } }

    // Wellen-Symbole aus derselben Formel wie der Ton
    fun wave(w: Wave): ImageVector = when (w) {
        Wave.SINE -> Sine
        Wave.SQUARE -> Square
        Wave.TRIANGLE -> Triangle
        Wave.SAW -> Saw
    }

    private val Sine by lazy { shape("sine", Wave.SINE) }
    private val Square by lazy { shape("square", Wave.SQUARE) }
    private val Triangle by lazy { shape("triangle", Wave.TRIANGLE) }
    private val Saw by lazy { shape("saw", Wave.SAW) }

    // eine Periode ueber 18 Einheiten, Amplitude 6; Spruenge als senkrechte Linie
    private fun shape(name: String, w: Wave): ImageVector = line(name) {
        val n = 72
        for (i in 0..n) {
            val t = i / n.toDouble()
            val x = 3f + 18f * t.toFloat()
            val y = 12f - 6f * Oscillator.naive(w, minOf(t, 0.99999)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
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
