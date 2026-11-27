package solutions.xapient.pulsr.ui

import solutions.xapient.pulsr.res.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.pulsr.domain.Subdivision

// Magenta laut Markenfarben; Akzent nur fuer Puls, Rad und Hauptaktion
val Magenta = Color(0xFFFF4FA0)
val PulsrAccent = Magenta
val Bg = Color(0xFF10161B)

// Leerlauf: Segmente gedaempft und leicht entsaettigt (ruhig, nicht "an"), aber >= 3:1 zum Grund (ContrastTest)
fun idleSegment(ui: AappsColors): Color = lerp(lerp(ui.bg, ui.accent, 0.55f), ui.fgMuted, 0.35f)

// Beim Spielen: ruhende Schlaege weit zurueck, der laufende leuchtet voll (>= 2,5:1 dazu)
const val SEGMENT_REST = 0.4f
fun restSegment(ui: AappsColors): Color = lerp(ui.bg, ui.accent, SEGMENT_REST)

// Helligkeit des laufenden Schlags: nie unter LIT_HOLD, Anschlag voll
const val LIT_HOLD = 0.8f
fun litLevel(flash: Float): Float = LIT_HOLD + (1f - LIT_HOLD) * flash

// Lichthof hinter dem Rad: Grundhelligkeit und Aufblitzen je Schlag; Summe haelt Text >= 4,5:1
const val GLOW_BASE = 0.06f
const val GLOW_FLASH = 0.16f
// Anteil des Lichthofs am Ring (Verlauf von drawGlow bei >= 35 % des Radius)
const val RING_GLOW = 0.45f

@Composable
fun subLabel(s: Subdivision): String = stringResource(
    when (s) {
        Subdivision.QUARTER -> Res.string.sub_quarter
        Subdivision.EIGHTH -> Res.string.sub_eighth
        Subdivision.TRIPLET -> Res.string.sub_triplet
        Subdivision.SIXTEENTH -> Res.string.sub_sixteenth
    },
)

// Linien-Icons und Notenzeichen nur fuer pulsr, gleicher Strich wie AappsIcons
object PulsrIcons {
    val Play: ImageVector by lazy { line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() } }
    val Stop: ImageVector by lazy {
        line("stop") { moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close() }
    }
    val Plus: ImageVector by lazy { line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Minus: ImageVector by lazy { line("minus") { moveTo(5f, 12f); lineTo(19f, 12f) } }

    fun note(s: Subdivision): ImageVector = when (s) {
        Subdivision.QUARTER -> Quarter
        Subdivision.EIGHTH -> Eighth
        Subdivision.TRIPLET -> Triplet
        Subdivision.SIXTEENTH -> Sixteenth
    }

    private val Quarter by lazy { notes("quarter", listOf(10.5f), 2.7f, 0) }
    private val Eighth by lazy { notes("eighth", listOf(6.5f, 15.5f), 2.5f, 1) }
    private val Triplet by lazy { notes("triplet", listOf(4.6f, 11f, 17.4f), 2.2f, 1) }
    private val Sixteenth by lazy { notes("sixteenth", listOf(3.4f, 8.6f, 13.8f, 19f), 1.9f, 2) }

    // Notenkoepfe gefuellt, Haelse und Balken als Strich
    private fun notes(name: String, heads: List<Float>, r: Float, beams: Int): ImageVector {
        val y = 17.5f
        val top = 5.5f
        val stems = heads.map { it + r * 0.85f }
        return ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(fill = SolidColor(Color.White)) {
                heads.forEach { x ->
                    moveTo(x + r, y)
                    arcTo(r, r * 0.8f, 0f, true, true, x - r, y)
                    arcTo(r, r * 0.8f, 0f, true, true, x + r, y)
                    close()
                }
            }
            .path(stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round) {
                stems.forEach { x -> moveTo(x, y - 0.5f); lineTo(x, top) }
            }
            .path(stroke = SolidColor(Color.White), strokeLineWidth = 2.4f, strokeLineCap = StrokeCap.Butt) {
                for (b in 0 until beams) {
                    val by = top + 1.1f + b * 3.2f
                    moveTo(stems.first() - 0.8f, by); lineTo(stems.last() + 0.8f, by)
                }
            }
            .build()
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
