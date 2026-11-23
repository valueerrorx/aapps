package solutions.xapient.spinr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Linien-Icons nur fuer spinr, gleicher Strich wie AappsIcons
object SpinrIcons {
    val Plus: ImageVector by lazy {
        line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    }

    val Close: ImageVector by lazy {
        line("close") { moveTo(7f, 7f); lineTo(17f, 17f); moveTo(17f, 7f); lineTo(7f, 17f) }
    }

    val Pencil: ImageVector by lazy {
        line("pencil") {
            moveTo(5f, 19f); lineTo(5.6f, 15.4f); lineTo(15.6f, 5.4f); lineTo(18.6f, 8.4f); lineTo(8.6f, 18.4f); close()
            moveTo(13.4f, 7.6f); lineTo(16.4f, 10.6f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        line("chevronDown") { moveTo(6f, 9.5f); lineTo(12f, 15.5f); lineTo(18f, 9.5f) }
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
