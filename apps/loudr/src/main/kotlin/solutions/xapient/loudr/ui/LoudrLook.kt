package solutions.xapient.loudr.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.text.DecimalFormatSymbols
import kotlin.math.min
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import solutions.xapient.loudr.R
import solutions.xapient.loudr.domain.Example
import solutions.xapient.loudr.domain.Zone

// Orange laut Markenfarben; zugleich Farbe der Zone "laut"
val LoudrAccent = Color(0xFFFF8A3D)
val Bg = Color(0xFF10161B)

fun zoneColor(z: Zone): Color = when (z) {
    Zone.QUIET -> Color(0xFF5ED6A6)
    Zone.MODERATE -> Color(0xFFE9D35B)
    Zone.LOUD -> LoudrAccent
    Zone.VERY_LOUD -> Color(0xFFFF5C5C)
}

// Pausiert: Pegelbogen und Verlauf gedimmt, aber >= 3:1 zum Grund (ContrastTest)
const val PAUSED_ALPHA = 0.8f
// Zonen-Rand am Bogen: dauerhaft sichtbar, >= 3:1 zum Grund
const val RIM_ALPHA = 0.7f
// Lichthof hinter der Zahl, hoechstens so stark (Text bleibt >= 4,5:1)
const val GLOW_MAX = 0.13f

// Schrift waechst mit der Systemgroesse, aber hoechstens bis max (sichtbare sp)
@Composable
fun cappedSp(base: Float, max: Float): TextUnit {
    val f = LocalDensity.current.fontScale
    return (min(base * f, max) / f).sp
}

// Dezimalzeichen der aktuellen Sprache
@Composable
fun decimalSeparator(): Char =
    DecimalFormatSymbols.getInstance(LocalConfiguration.current.locales[0]).decimalSeparator

fun exampleRes(e: Example): Int = when (e) {
    Example.SILENCE -> R.string.ex_silence
    Example.WHISPER -> R.string.ex_whisper
    Example.QUIET_ROOM -> R.string.ex_quiet_room
    Example.OFFICE -> R.string.ex_office
    Example.CONVERSATION -> R.string.ex_conversation
    Example.VACUUM -> R.string.ex_vacuum
    Example.TRAFFIC -> R.string.ex_traffic
    Example.MOWER -> R.string.ex_mower
    Example.MOTORBIKE -> R.string.ex_motorbike
    Example.CONCERT -> R.string.ex_concert
    Example.SIREN -> R.string.ex_siren
}

// Linien-Icons nur fuer loudr, gleicher Strich wie AappsIcons
object LoudrIcons {
    val Play: ImageVector by lazy { line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() } }
    val Pause: ImageVector by lazy { line("pause") { moveTo(8.5f, 6f); lineTo(8.5f, 18f); moveTo(15.5f, 6f); lineTo(15.5f, 18f) } }
    val Plus: ImageVector by lazy { line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Minus: ImageVector by lazy { line("minus") { moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Reset: ImageVector by lazy {
        line("reset") {
            moveTo(5f, 12f)
            arcTo(7f, 7f, 0f, true, false, 7.1f, 7f)
            moveTo(6.5f, 3f); lineTo(6.8f, 7.3f); lineTo(11f, 7f)
        }
    }
    val Mic: ImageVector by lazy {
        line("mic") {
            moveTo(9f, 7f)
            arcTo(3f, 3f, 0f, false, true, 15f, 7f)
            lineTo(15f, 12f)
            arcTo(3f, 3f, 0f, false, true, 9f, 12f)
            close()
            moveTo(5.5f, 11.5f)
            arcTo(6.5f, 6.5f, 0f, false, false, 18.5f, 11.5f)
            moveTo(12f, 18f); lineTo(12f, 21f)
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
