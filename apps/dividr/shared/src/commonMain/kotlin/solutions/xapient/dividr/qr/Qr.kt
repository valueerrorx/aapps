package solutions.xapient.dividr.qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

// QR-Matrix ohne Ruhezone; ECC M, Version waechst mit der Datenmenge (bis 40)
class QrMatrix(val size: Int, val version: Int, private val dark: BooleanArray) {
    operator fun get(x: Int, y: Int) = dark[y * size + x]

    // 1 Pixel je Modul, inkl. Ruhezone von 4 Modulen; gross gezeichnet ohne Glaettung = scharf
    fun bitmap(): ImageBitmap = qrBitmap(this)

    companion object {
        const val QUIET = 4
        internal const val WHITE = -0x1
        internal const val BLACK = -0xF4EFEC // #0B1014

        fun encode(text: String): QrMatrix = qrEncode(text)
    }
}

/** Android: ZXing, iOS: [QrEncoder] */
internal expect fun qrEncode(text: String): QrMatrix

internal expect fun qrBitmap(m: QrMatrix): ImageBitmap

// Schwarz auf weisser Karte; Module ganzzahlig skaliert
@Composable
fun QrImage(matrix: QrMatrix, description: String, modifier: Modifier = Modifier, corner: Dp = 18.dp) {
    val img = remember(matrix) { matrix.bitmap() }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(corner))
            .background(Color.White)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize().padding(2.dp)) {
            val n = img.width
            val side = minOf(size.width, size.height).toInt()
            val scale = maxOf(1, side / n)
            val s = n * scale
            val off = IntOffset((size.width.toInt() - s) / 2, (size.height.toInt() - s) / 2)
            drawImage(img, srcSize = IntSize(n, n), dstOffset = off, dstSize = IntSize(s, s), filterQuality = FilterQuality.None)
        }
    }
}
