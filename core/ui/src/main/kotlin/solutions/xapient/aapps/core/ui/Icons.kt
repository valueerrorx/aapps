package solutions.xapient.aapps.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Linien-Icons im einheitlichen Strich, ohne Material-Icon-Bibliothek
object AappsIcons {
    val Gear: ImageVector by lazy {
        line("gear") {
            val teeth = 8
            val outer = 9.6f
            val inner = 7.4f
            for (i in 0 until teeth * 4) {
                val a = (i / (teeth * 4f)) * 2 * PI - PI / 2
                val r = if ((i / 2) % 2 == 0) outer else inner
                val x = 12f + r * cos(a).toFloat()
                val y = 12f + r * sin(a).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
            circle(12f, 12f, 3.2f)
        }
    }

    val Back: ImageVector by lazy {
        line("back") {
            moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f)
        }
    }

    val Chevron: ImageVector by lazy {
        line("chevron") {
            moveTo(9.5f, 6f); lineTo(15.5f, 12f); lineTo(9.5f, 18f)
        }
    }

    val Lock: ImageVector by lazy {
        line("lock") {
            moveTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 20f); lineTo(6f, 20f); close()
            moveTo(8.5f, 11f); lineTo(8.5f, 8f)
            arcTo(3.5f, 3.5f, 0f, false, true, 15.5f, 8f)
            lineTo(15.5f, 11f)
        }
    }

    val Unlock: ImageVector by lazy {
        line("unlock") {
            moveTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 20f); lineTo(6f, 20f); close()
            moveTo(8.5f, 11f); lineTo(8.5f, 8f)
            arcTo(3.5f, 3.5f, 0f, false, true, 15.2f, 6.6f)
        }
    }

    val Target: ImageVector by lazy {
        line("target") {
            circle(12f, 12f, 7.5f)
            moveTo(12f, 2f); lineTo(12f, 6f)
            moveTo(12f, 18f); lineTo(12f, 22f)
            moveTo(2f, 12f); lineTo(6f, 12f)
            moveTo(18f, 12f); lineTo(22f, 12f)
            circle(12f, 12f, 1.2f)
        }
    }

    val Rotate: ImageVector by lazy {
        line("rotate") {
            moveTo(19f, 12f)
            arcTo(7f, 7f, 0f, true, true, 16.9f, 7f)
            moveTo(17.5f, 3f); lineTo(17.2f, 7.3f); lineTo(13f, 7f)
        }
    }

    val Check: ImageVector by lazy {
        line("check") {
            moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f)
        }
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, true, true, cx - r, cy)
        arcTo(r, r, 0f, true, true, cx + r, cy)
        close()
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

@Composable
fun AappsIcon(icon: ImageVector, contentDescription: String?, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
