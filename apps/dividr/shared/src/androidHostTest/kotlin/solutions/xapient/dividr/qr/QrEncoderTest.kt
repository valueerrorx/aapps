package solutions.xapient.dividr.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.decoder.Version
import com.google.zxing.qrcode.decoder.Mode
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.dividr.domain.Base45
import kotlin.random.Random

// Eigener Encoder (iOS) gegen ZXing (Android): gleiche Tabellen, lesbar, moeglichst gleiche Matrix
class QrEncoderTest {
    private fun read(size: Int, dark: (Int, Int) -> Boolean, scale: Int = 3): String {
        val q = 4
        val n = (size + 2 * q) * scale
        val lum = ByteArray(n * n) { -1 }
        for (y in 0 until size) for (x in 0 until size) if (dark(x, y)) {
            for (dy in 0 until scale) for (dx in 0 until scale) lum[((y + q) * scale + dy) * n + (x + q) * scale + dx] = 0
        }
        val src = PlanarYUVLuminanceSource(lum, n, n, 0, 0, n, n, false)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(src)), mapOf(DecodeHintType.TRY_HARDER to true)).text
    }

    @Test fun tablesMatchZxing() {
        for (v in 1..40) {
            val ver = Version.getVersionForNumber(v)
            val ec = ver.getECBlocksForLevel(ErrorCorrectionLevel.M)
            assertEquals("total $v", ver.totalCodewords, QrEncoder.totalCodewords(v))
            assertEquals("ecc $v", ec.ecCodewordsPerBlock, QrEncoder.eccPerBlock(v))
            assertEquals("blocks $v", ec.numBlocks, QrEncoder.blocks(v))
        }
    }

    // Laengen ueber alle Versionen; Base45-Text wie in dividr. Maskenwahl darf abweichen (beide gueltig),
    // mit gleicher Maske muss die Matrix aber Bit fuer Bit der von ZXing entsprechen
    @Test fun readsBackAndMatchesZxing() {
        val rnd = Random(7)
        var same = 0
        val lengths = listOf(1, 2, 20, 47, 100, 300, 700, 1200, 1800, 2400, 3000, 3387)
        for (len in lengths) {
            val bytes = ByteArray(len * 2 / 3 + 1) { rnd.nextInt(256).toByte() }
            val text = Base45.encode(bytes).take(len)
            val r = QrEncoder.encode(text)
            assertEquals(text, read(r.size, { x, y -> r.dark[y * r.size + x] }))
            val z = Encoder.encode(text, ErrorCorrectionLevel.M)
            assertEquals("version $len", z.version.versionNumber, r.version)
            fun equal(m: ByteMatrix) = (0 until r.size * r.size).all { i -> (m.get(i % r.size, i / r.size).toInt() == 1) == r.dark[i] }
            if (equal(z.matrix)) same++
            if (z.mode != Mode.ALPHANUMERIC) continue // ZXing nimmt fuer reine Ziffern NUMERIC
            val anyMask = (0..7).any { mask -> equal(Encoder.encode(text, ErrorCorrectionLevel.M, mapOf(EncodeHintType.QR_MASK_PATTERN to mask)).matrix) }
            assertTrue("matrix $len", anyMask)
        }
        println("QrEncoder: $same von ${lengths.size} Matrizen gleich wie ZXing")
    }

    @Test fun byteModeReadsBack() {
        val text = "dividr äöü https://example.org/?a=1"
        val r = QrEncoder.encode(text)
        assertEquals(text, read(r.size, { x, y -> r.dark[y * r.size + x] }))
        assertTrue(r.version >= 2)
    }
}
