package solutions.xapient.dozr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Mondrose: letzter Schein der Daemmerung; keine andere aapps-App nutzt Rosa; auf dunklem Grund > 9:1
val DozrAccent = Color(0xFFFF9EC7)

// Mond selbst: warmes Elfenbein
val MoonLight = Color(0xFFF4EEDB)
val MoonShade = Color(0xFFCFC6B4)

// Nachthimmel: oben fast schwarz, zum Horizont tiefblau
val SkyTop = Color(0xFF090D18)
val SkyLow = Color(0xFF16203F)
val Water = Color(0xFF070B13)
val WaterLine = Color(0xFF2B3658)
val Hills = Color(0xFF0B111E)

// Bahn und Zahlen am Himmel
val TrackDot = Color(0xFF566487)
val SkyLabel = Color(0xFFA2AEC8)

// Linien-Icons nur fuer dozr, gleicher Strich wie AappsIcons
object DozrIcons {
    val Play: ImageVector by lazy {
        line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() }
    }

    val Stop: ImageVector by lazy {
        line("stop") {
            moveTo(8.5f, 6.5f); lineTo(15.5f, 6.5f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f); lineTo(17.5f, 15.5f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f); lineTo(8.5f, 17.5f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f); lineTo(6.5f, 8.5f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f); close()
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
