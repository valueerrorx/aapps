package solutions.xapient.magnr.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.magnr.R
import solutions.xapient.magnr.domain.Level
import java.text.DecimalFormatSymbols
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// Magnet-Violett; zugleich Farbe des Ausschlags
val MagnrAccent = Color(0xFFC08EFF)
// heller Akzent fuer das Ende des Ausschlags
val AccentLight = Color(0xFFE2CCFF)
val Warn = Color(0xFFF2B84E)

// Pausiert: Ausschlag und Verlauf gedimmt, aber >= 3:1 zum Grund (ContrastTest)
const val PAUSED_ALPHA = 0.75f
// Lichthof hinter der Zahl, hoechstens so stark (Text bleibt >= 4,5:1)
const val GLOW_MAX = 0.16f
// Spitzen-Bogen: duenn, nachleuchtend
const val PEAK_ALPHA = 0.85f

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

// µT mit einer Nachkommastelle, ab 1000 ganzzahlig; signed: immer mit Vorzeichen
fun formatUt(v: Float, sep: Char = '.', signed: Boolean = false): String {
    if (v.isNaN()) return "–"
    val big = abs(v) >= 999.95f
    val tenths = (abs(v) * 10f).roundToInt()
    val body = if (big) (tenths / 10f).roundToInt().toString() else "${tenths / 10}$sep${tenths % 10}"
    val sign = when {
        tenths == 0 -> if (signed) "±" else ""
        v < 0f -> "−"
        signed -> "+"
        else -> ""
    }
    return sign + body
}

// Skalenwert: ganze Zahl wenn moeglich
fun formatScale(v: Float, sep: Char = '.'): String =
    if (v == v.roundToInt().toFloat()) v.roundToInt().toString() else formatUt(v, sep)

fun levelRes(l: Level): Int = when (l) {
    Level.QUIET -> R.string.level_quiet
    Level.WEAK -> R.string.level_weak
    Level.NEAR -> R.string.level_near
    Level.STRONG -> R.string.level_strong
}

// Linien-Icons nur fuer magnr, gleicher Strich wie AappsIcons
object MagnrIcons {
    val Play: ImageVector by lazy { line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() } }
    val Pause: ImageVector by lazy { line("pause") { moveTo(8.5f, 6f); lineTo(8.5f, 18f); moveTo(15.5f, 6f); lineTo(15.5f, 18f) } }
    val Sound: ImageVector by lazy {
        line("sound") {
            speaker()
            moveTo(15f, 9f); arcTo(4f, 4f, 0f, false, true, 15f, 15f)
            moveTo(17.5f, 6.5f); arcTo(7.5f, 7.5f, 0f, false, true, 17.5f, 17.5f)
        }
    }
    val Mute: ImageVector by lazy {
        line("mute") {
            speaker()
            moveTo(15.5f, 9.5f); lineTo(20.5f, 14.5f)
            moveTo(20.5f, 9.5f); lineTo(15.5f, 14.5f)
        }
    }
    private fun PathBuilder.speaker() {
        moveTo(4f, 9.5f); lineTo(7.5f, 9.5f); lineTo(11.5f, 5.5f); lineTo(11.5f, 18.5f); lineTo(7.5f, 14.5f); lineTo(4f, 14.5f); close()
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
