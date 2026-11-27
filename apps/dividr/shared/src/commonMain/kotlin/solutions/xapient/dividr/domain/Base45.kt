package solutions.xapient.dividr.domain

// RFC 9285; Alphabet = QR-Alphanumerik, darum dicht im QR
object Base45 {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"

    fun encode(data: ByteArray): String {
        val sb = StringBuilder(data.size * 3 / 2 + 2)
        var i = 0
        while (i + 1 < data.size) {
            var n = (data[i].toInt() and 0xFF) * 256 + (data[i + 1].toInt() and 0xFF)
            repeat(3) { sb.append(ALPHABET[n % 45]); n /= 45 }
            i += 2
        }
        if (i < data.size) {
            var n = data[i].toInt() and 0xFF
            repeat(2) { sb.append(ALPHABET[n % 45]); n /= 45 }
        }
        return sb.toString()
    }

    // null bei ungueltigen Zeichen oder Laenge
    fun decode(text: String): ByteArray? {
        if (text.length % 3 == 1) return null
        val out = ByteArray(text.length / 3 * 2 + if (text.length % 3 == 2) 1 else 0)
        var o = 0
        var i = 0
        while (i < text.length) {
            val len = minOf(3, text.length - i)
            var n = 0
            for (k in len - 1 downTo 0) {
                val v = ALPHABET.indexOf(text[i + k])
                if (v < 0) return null
                n = n * 45 + v
            }
            if (len == 3) {
                if (n > 0xFFFF) return null
                out[o++] = (n shr 8).toByte()
                out[o++] = n.toByte()
            } else {
                if (n > 0xFF) return null
                out[o++] = n.toByte()
            }
            i += len
        }
        return out
    }
}
