package solutions.xapient.scorr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ScorrAccent = Color(0xFFFFD24A)

// Spielerfarben: satt, auf dunklem Grund gut unterscheidbar, Tinte darauf mit Kontrast > 7:1
val PlayerColors = listOf(
    Color(0xFFFFD24A), Color(0xFFFF7A6B), Color(0xFF5CC8FF), Color(0xFF5BE3A6),
    Color(0xFFB59BFF), Color(0xFFFF8AC9), Color(0xFFFFA24C), Color(0xFFC6F24E),
)

// Dunkle Schrift auf den Farbflaechen
val Ink = Color(0xFF0E1318)

fun playerColor(i: Int): Color = PlayerColors[i.mod(PlayerColors.size)]

// Linien-Icons nur fuer scorr, gleicher Strich wie AappsIcons
object ScorrIcons {
    val Undo: ImageVector by lazy {
        line("undo") {
            moveTo(9f, 14f); lineTo(4.5f, 9.5f); lineTo(9f, 5f)
            moveTo(4.5f, 9.5f); lineTo(14.5f, 9.5f)
            arcTo(5f, 5f, 0f, false, true, 14.5f, 19.5f)
            lineTo(11f, 19.5f)
        }
    }

    val Table: ImageVector by lazy {
        line("table") {
            moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 19f); lineTo(5f, 19f); close()
            moveTo(5f, 10f); lineTo(19f, 10f)
            moveTo(5f, 14.5f); lineTo(19f, 14.5f)
            moveTo(10f, 5f); lineTo(10f, 19f)
        }
    }

    val Crown: ImageVector by lazy {
        line("crown") {
            moveTo(4.5f, 17.5f); lineTo(3.5f, 8f); lineTo(8.5f, 12f); lineTo(12f, 5.5f)
            lineTo(15.5f, 12f); lineTo(20.5f, 8f); lineTo(19.5f, 17.5f); close()
        }
    }

    val Trophy: ImageVector by lazy {
        line("trophy") {
            moveTo(7.5f, 4.5f); lineTo(16.5f, 4.5f); lineTo(16.5f, 9.5f)
            arcTo(4.5f, 4.5f, 0f, false, true, 7.5f, 9.5f); close()
            moveTo(7.5f, 6f); lineTo(4.5f, 6f); quadTo(4.5f, 10.5f, 8f, 11f)
            moveTo(16.5f, 6f); lineTo(19.5f, 6f); quadTo(19.5f, 10.5f, 16f, 11f)
            moveTo(12f, 14f); lineTo(12f, 19.5f)
            moveTo(8.5f, 19.5f); lineTo(15.5f, 19.5f)
        }
    }

    val Plus: ImageVector by lazy {
        line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    }

    val Minus: ImageVector by lazy {
        line("minus") { moveTo(5f, 12f); lineTo(19f, 12f) }
    }

    val Close: ImageVector by lazy {
        line("close") { moveTo(7f, 7f); lineTo(17f, 17f); moveTo(17f, 7f); lineTo(7f, 17f) }
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
