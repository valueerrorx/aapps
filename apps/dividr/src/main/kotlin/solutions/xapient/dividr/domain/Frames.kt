package solutions.xapient.dividr.domain

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

// Nachricht -> Deflate -> Rahmen [magic | Version | CRC32 | Teil | Teile | Daten] -> Base45
object Frames {
    const val MAGIC = 'D'.code
    const val VERSION = 1
    const val HEADER = 8
    // Normalfall genau ein Code: Rahmen <= 2258 Byte = 3387 Zeichen, passt in QR-Version 40 (ECC M, alphanumerisch 3391)
    const val CHUNK = 2250
    const val MAX_PARTS = 64
    const val MAX_INFLATED = 256 * 1024

    fun compress(data: ByteArray): ByteArray {
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

    // null bei kaputten Daten oder mehr als MAX_INFLATED
    fun inflate(data: ByteArray): ByteArray? {
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
                if (out.size() > MAX_INFLATED) return null
            }
            return out.toByteArray()
        } catch (_: Exception) {
            return null
        } finally {
            inf.end()
        }
    }

    fun crc(data: ByteArray): Int = CRC32().apply { update(data) }.value.toInt()

    // Gruppe -> QR-Texte; leer, wenn mehr als MAX_PARTS Teile noetig waeren
    fun pack(group: Group): List<String> = pack(WireCodec.encode(group))

    // Mehrere Teile nur als Notfall, wenn selbst komprimiert kein einzelner Code reicht
    fun pack(message: ByteArray, chunk: Int = CHUNK): List<String> {
        val z = compress(message)
        val parts = maxOf(1, (z.size + chunk - 1) / chunk)
        if (parts > MAX_PARTS) return emptyList()
        val c = crc(z)
        return (0 until parts).map { i ->
            val piece = z.copyOfRange(i * chunk, minOf(z.size, (i + 1) * chunk))
            val head = byteArrayOf(
                MAGIC.toByte(), VERSION.toByte(),
                (c ushr 24).toByte(), (c ushr 16).toByte(), (c ushr 8).toByte(), c.toByte(),
                i.toByte(), parts.toByte(),
            )
            Base45.encode(head + piece)
        }
    }
}

sealed interface ScanResult {
    data object NotOurs : ScanResult // fremder Code (URL, Text)
    data object Newer : ScanResult // neuere App-Version
    data object Broken : ScanResult // unser Code, aber nicht lesbar
    data class Progress(val have: Set<Int>, val total: Int) : ScanResult
    data class Done(val group: Group) : ScanResult
}

// Sammelt Teile nach CRC; doppelte ignoriert, anderer CRC = neu beginnen
class FrameCollector {
    private var crc = 0
    private var total = 0
    private val parts = HashMap<Int, ByteArray>()

    val have: Set<Int> get() = parts.keys.toSet()
    val expected get() = total

    fun reset() {
        parts.clear(); total = 0; crc = 0
    }

    fun accept(text: String): ScanResult {
        val raw = Base45.decode(text.trim()) ?: return ScanResult.NotOurs
        if (raw.size < Frames.HEADER || (raw[0].toInt() and 0xFF) != Frames.MAGIC) return ScanResult.NotOurs
        val version = raw[1].toInt() and 0xFF
        if (version > Frames.VERSION) return ScanResult.Newer
        if (version != Frames.VERSION) return ScanResult.NotOurs
        val c = ((raw[2].toInt() and 0xFF) shl 24) or ((raw[3].toInt() and 0xFF) shl 16) or
            ((raw[4].toInt() and 0xFF) shl 8) or (raw[5].toInt() and 0xFF)
        val index = raw[6].toInt() and 0xFF
        val count = raw[7].toInt() and 0xFF
        if (count !in 1..Frames.MAX_PARTS || index >= count) return ScanResult.Broken
        if (parts.isNotEmpty() && (c != crc || count != total)) parts.clear()
        crc = c
        total = count
        parts.getOrPut(index) { raw.copyOfRange(Frames.HEADER, raw.size) }
        if (parts.size < total) return ScanResult.Progress(have, total)
        val z = ByteArrayOutputStream().apply { (0 until total).forEach { write(parts.getValue(it)) } }.toByteArray()
        reset()
        if (Frames.crc(z) != c) return ScanResult.Broken
        val msg = Frames.inflate(z) ?: return ScanResult.Broken
        return try {
            ScanResult.Done(WireCodec.decode(msg))
        } catch (_: NewerFormatException) {
            ScanResult.Newer
        } catch (_: CodecException) {
            ScanResult.Broken
        }
    }
}
