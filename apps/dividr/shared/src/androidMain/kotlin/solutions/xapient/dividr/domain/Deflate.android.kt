package solutions.xapient.dividr.domain

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

internal actual object Deflate {
    actual fun compress(data: ByteArray): ByteArray {
        val d = Deflater(9, true)
        try {
            d.setInput(data)
            d.finish()
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!d.finished()) out.write(buf, 0, d.deflate(buf))
            return out.toByteArray()
        } finally {
            d.end()
        }
    }

    actual fun inflate(data: ByteArray, max: Int): ByteArray? {
        val inf = Inflater(true)
        try {
            // raw Deflate braucht ein Fuell-Byte am Ende
            inf.setInput(data + byteArrayOf(0))
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var stall = 0
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary() || ++stall > 3)) return null
                out.write(buf, 0, n)
                if (out.size() > max) return null
            }
            return out.toByteArray()
        } catch (_: Exception) {
            return null
        } finally {
            inf.end()
        }
    }
}
