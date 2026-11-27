package solutions.xapient.dividr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.dividr.domain.CodecTest
import solutions.xapient.dividr.domain.FrameCollector
import solutions.xapient.dividr.domain.Frames
import solutions.xapient.dividr.domain.ScanResult
import solutions.xapient.dividr.domain.WireCodec
import solutions.xapient.dividr.qr.QrMatrix
import kotlin.random.Random

// ZXing erzeugt, ZXing liest zurueck (ohne Kamera)
class QrRoundTripTest {
    private fun read(m: QrMatrix, scale: Int = 3): String {
        val q = QrMatrix.QUIET
        val n = (m.size + 2 * q) * scale
        val lum = ByteArray(n * n) { -1 }
        for (y in 0 until m.size) for (x in 0 until m.size) if (m[x, y]) {
            for (dy in 0 until scale) for (dx in 0 until scale) lum[((y + q) * scale + dy) * n + (x + q) * scale + dx] = 0
        }
        val src = PlanarYUVLuminanceSource(lum, n, n, 0, 0, n, n, false)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(src)), mapOf(DecodeHintType.TRY_HARDER to true)).text
    }

    @Test fun fullFrameFitsVersion40AndReadsBack() {
        // unkomprimierbare Daten: Rahmen in voller Groesse
        val rnd = Random(4)
        val msg = ByteArray(Frames.CHUNK - 10) { rnd.nextInt(256).toByte() }
        val parts = Frames.pack(msg)
        assertEquals(1, parts.size)
        val m = QrMatrix.encode(parts[0])
        println("dividr: voller Rahmen ${parts[0].length} Zeichen -> QR-Version ${m.version}")
        assertTrue(m.version <= 40)
        assertEquals(parts[0], read(m))
    }

    @Test fun realisticTripIsOneCode() {
        val g = CodecTest().trip(60)
        val raw = WireCodec.encode(g)
        val parts = Frames.pack(g)
        assertEquals(1, parts.size)
        val m = QrMatrix.encode(parts[0])
        println("dividr: 6 Personen, 60 Ausgaben -> ${raw.size} B roh, ${Frames.compress(raw).size} B komprimiert, ${parts[0].length} Zeichen, QR-Version ${m.version}")
        assertEquals(ScanResult.Done(g), FrameCollector().accept(read(m)))
    }
}
