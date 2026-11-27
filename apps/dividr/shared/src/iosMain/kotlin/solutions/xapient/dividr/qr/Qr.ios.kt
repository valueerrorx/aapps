package solutions.xapient.dividr.qr

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint

internal actual fun qrEncode(text: String): QrMatrix {
    val r = QrEncoder.encode(text)
    return QrMatrix(r.size, r.version, r.dark)
}

// Module als 1-Pixel-Rechtecke, ohne Kantenglaettung
internal actual fun qrBitmap(m: QrMatrix): ImageBitmap {
    val q = QrMatrix.QUIET
    val n = m.size + 2 * q
    val img = ImageBitmap(n, n)
    val canvas = Canvas(img)
    val paint = Paint().apply { isAntiAlias = false; color = Color(QrMatrix.WHITE) }
    canvas.drawRect(0f, 0f, n.toFloat(), n.toFloat(), paint)
    paint.color = Color(QrMatrix.BLACK)
    for (y in 0 until m.size) for (x in 0 until m.size) {
        if (m[x, y]) canvas.drawRect((x + q).toFloat(), (y + q).toFloat(), (x + q + 1).toFloat(), (y + q + 1).toFloat(), paint)
    }
    return img
}
