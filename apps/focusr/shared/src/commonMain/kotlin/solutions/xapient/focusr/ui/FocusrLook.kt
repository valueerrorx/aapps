package solutions.xapient.focusr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.lerp
import solutions.xapient.aapps.core.ui.AappsColors
import androidx.compose.ui.unit.dp

// Koralle: warmes Licht, auf dunklem Grund > 6:1
val FocusrAccent = Color(0xFFFF6E5A)

// Sektor: innen heller, aussen tiefer
val AccentLight = Color(0xFFFF9A7E)
val AccentDeep = Color(0xFFE5483A)

// Zahlen am Zifferblatt: etwas heller als FG_MUTED, damit sie auch im Lichthof AA erreichen
fun dialLabel(c: AappsColors): Color = lerp(c.fgMuted, c.fg, 0.3f)

// Mulde unter der Scheibe, etwas dunkler als BG
val Well = Color(0xFF0B1014)

// Linien-Icons nur fuer focusr, gleicher Strich wie AappsIcons
object FocusrIcons {
    val Play: ImageVector by lazy {
        line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() }
    }

    val Pause: ImageVector by lazy {
        line("pause") { moveTo(9f, 6f); lineTo(9f, 18f); moveTo(15f, 6f); lineTo(15f, 18f) }
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
