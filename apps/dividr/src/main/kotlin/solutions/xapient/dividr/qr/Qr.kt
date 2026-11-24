package solutions.xapient.dividr.qr

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

// QR-Matrix ohne Ruhezone; ECC M, Version waechst mit der Datenmenge (bis 40)
class QrMatrix(val size: Int, val version: Int, private val dark: BooleanArray) {
    operator fun get(x: Int, y: Int) = dark[y * size + x]

    // 1 Pixel je Modul, inkl. Ruhezone von 4 Modulen; gross gezeichnet ohne Glaettung = scharf
    fun bitmap(fg: Int = BLACK, bg: Int = WHITE, quiet: Int = QUIET): ImageBitmap {
        val n = size + 2 * quiet
        val px = IntArray(n * n) { bg }
        for (y in 0 until size) for (x in 0 until size) if (this[x, y]) px[(y + quiet) * n + x + quiet] = fg
        return Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    companion object {
        const val QUIET = 4
        private const val WHITE = -0x1
        private const val BLACK = -0xF4EFEC // #0B1014

        fun encode(text: String): QrMatrix {
            val code = Encoder.encode(text, ErrorCorrectionLevel.M)
            val m = code.matrix
            val dark = BooleanArray(m.width * m.height) { i -> m.get(i % m.width, i / m.width).toInt() == 1 }
            return QrMatrix(m.width, code.version.versionNumber, dark)
        }
    }
}

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

// Nur Vorschau: Module in Farbe auf transparentem Grund, nicht zum Scannen gedacht
@Composable
fun QrPreview(matrix: QrMatrix, color: Color, description: String, modifier: Modifier = Modifier) {
    val argb = color.toArgb()
    val img = remember(matrix, argb) { matrix.bitmap(fg = argb, bg = 0, quiet = 0) }
    Canvas(modifier.aspectRatio(1f).semantics { contentDescription = description }) {
        val side = minOf(size.width, size.height).toInt()
        val off = IntOffset((size.width.toInt() - side) / 2, (size.height.toInt() - side) / 2)
        drawImage(img, srcSize = IntSize(img.width, img.height), dstOffset = off, dstSize = IntSize(side, side), filterQuality = FilterQuality.None)
    }
}
