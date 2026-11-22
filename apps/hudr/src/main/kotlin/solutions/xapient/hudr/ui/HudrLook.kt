package solutions.xapient.hudr.ui

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
import java.text.DecimalFormatSymbols
import kotlin.math.min
import solutions.xapient.hudr.R
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.domain.SpeedUnit

// HUD-Gruen als Markenfarbe
val HudrAccent = Color(0xFF46F0A8)
val Amber = Color(0xFFF2B84E)
val Red = Color(0xFFFF5C5C)

// HUD: reines Schwarz, gesaettigte Farben fuer die Spiegelung in der Scheibe
val HudBg = Color(0xFF000000)
val HudGreen = Color(0xFF4DFFA6)
val HudAmber = Color(0xFFFFC24D)
val HudRed = Color(0xFFFF6B6B)
// Nebentext im HUD (Einheit, Status), >= 4,5:1 auf Schwarz
const val HUD_MUTED_ALPHA = 0.6f

fun alertColor(a: Alert, accent: Color): Color = when (a) {
    Alert.NONE -> accent
    Alert.NEAR -> Amber
    Alert.OVER -> Red
}

fun hudColor(a: Alert): Color = when (a) {
    Alert.NONE -> HudGreen
    Alert.NEAR -> HudAmber
    Alert.OVER -> HudRed
}

// Signal weg: Bogen und Zahl gedimmt, aber >= 3:1 zum Grund (ContrastTest)
const val LOST_ALPHA = 0.72f
// Lichthof hinter der Zahl, hoechstens so stark (Text bleibt >= 4,5:1)
const val GLOW_MAX = 0.11f

fun unitRes(u: SpeedUnit): Int = when (u) {
    SpeedUnit.KMH -> R.string.unit_kmh
    SpeedUnit.MPH -> R.string.unit_mph
    SpeedUnit.KN -> R.string.unit_kn
}

fun distRes(u: SpeedUnit): Int = when (u) {
    SpeedUnit.KMH -> R.string.dist_km
    SpeedUnit.MPH -> R.string.dist_mi
    SpeedUnit.KN -> R.string.dist_nm
}

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

// Linien-Icons nur fuer hudr, gleicher Strich wie AappsIcons
object HudrIcons {
    val Plus: ImageVector by lazy { line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Minus: ImageVector by lazy { line("minus") { moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Reset: ImageVector by lazy {
        line("reset") {
            moveTo(5f, 12f)
            arcTo(7f, 7f, 0f, true, false, 7.1f, 7f)
            moveTo(6.5f, 3f); lineTo(6.8f, 7.3f); lineTo(11f, 7f)
        }
    }

    // Windschutzscheibe mit gespiegelter Zahl angedeutet
    val Hud: ImageVector by lazy {
        line("hud") {
            moveTo(3.5f, 8f)
            quadTo(12f, 4f, 20.5f, 8f)
            lineTo(18.5f, 17f)
            quadTo(12f, 15f, 5.5f, 17f)
            close()
            moveTo(10f, 9.5f); lineTo(14f, 9.5f); lineTo(14f, 13.5f)
        }
    }

    val Pin: ImageVector by lazy {
        line("pin") {
            moveTo(12f, 21f)
            curveTo(12f, 21f, 5f, 14.5f, 5f, 9.5f)
            arcTo(7f, 7f, 0f, false, true, 19f, 9.5f)
            curveTo(19f, 14.5f, 12f, 21f, 12f, 21f)
            close()
            moveTo(9.5f, 9.5f)
            arcTo(2.5f, 2.5f, 0f, true, false, 14.5f, 9.5f)
            arcTo(2.5f, 2.5f, 0f, true, false, 9.5f, 9.5f)
        }
    }

    val Close: ImageVector by lazy { line("close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) } }

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
