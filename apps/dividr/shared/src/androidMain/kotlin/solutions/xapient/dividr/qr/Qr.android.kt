package solutions.xapient.dividr.qr

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

internal actual fun qrEncode(text: String): QrMatrix {
    val code = Encoder.encode(text, ErrorCorrectionLevel.M)
    val m = code.matrix
    val dark = BooleanArray(m.width * m.height) { i -> m.get(i % m.width, i / m.width).toInt() == 1 }
    return QrMatrix(m.width, code.version.versionNumber, dark)
}

internal actual fun qrBitmap(m: QrMatrix): ImageBitmap {
    val q = QrMatrix.QUIET
    val n = m.size + 2 * q
    val px = IntArray(n * n) { QrMatrix.WHITE }
    for (y in 0 until m.size) for (x in 0 until m.size) if (m[x, y]) px[(y + q) * n + x + q] = QrMatrix.BLACK
    return Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
}
