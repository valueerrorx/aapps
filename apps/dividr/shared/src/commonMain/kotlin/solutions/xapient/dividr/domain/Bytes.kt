package solutions.xapient.dividr.domain

// Wachsender Byte-Puffer (statt java.io.ByteArrayOutputStream)
internal class ByteSink(capacity: Int = 256) {
    private var buf = ByteArray(capacity)
    var size = 0
        private set

    private fun room(n: Int) {
        if (size + n <= buf.size) return
        buf = buf.copyOf(maxOf(buf.size * 2, size + n))
    }

    fun write(v: Int) {
        room(1)
        buf[size++] = v.toByte()
    }

    fun write(b: ByteArray, off: Int = 0, len: Int = b.size - off) {
        room(len)
        b.copyInto(buf, size, off, off + len)
        size += len
    }

    fun toByteArray(): ByteArray = buf.copyOf(size)
}

// CRC-32 (IEEE, wie java.util.zip.CRC32)
internal object Crc32 {
    private val table = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }

    fun of(data: ByteArray): Int {
        var c = -1
        for (b in data) c = table[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
        return c.inv()
    }
}

/** Raw Deflate (RFC 1951, ohne zlib-Kopf); Android java.util.zip, iOS zlib */
internal expect object Deflate {
    fun compress(data: ByteArray): ByteArray

    // null bei kaputten Daten oder mehr als max Bytes
    fun inflate(data: ByteArray, max: Int): ByteArray?
}
