package solutions.xapient.dividr.domain

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.zlib.Z_DEFAULT_STRATEGY
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.deflateBound
import platform.zlib.deflateEnd
import platform.zlib.deflateInit2_
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream
import platform.zlib.zlibVersion

// System-zlib wie java.util.zip: Stufe 9, raw (windowBits -15), in einem Schritt
@OptIn(ExperimentalForeignApi::class)
internal actual object Deflate {
    actual fun compress(data: ByteArray): ByteArray = memScoped {
        val s = alloc<z_stream>()
        s.zalloc = null
        s.zfree = null
        s.opaque = null
        s.avail_in = 0u
        val version = zlibVersion()?.toKString()
        check(deflateInit2_(s.ptr, 9, Z_DEFLATED, -15, 8, Z_DEFAULT_STRATEGY, version, sizeOf<z_stream>().convert()) == Z_OK)
        try {
            val input = if (data.isEmpty()) ByteArray(1) else data
            val out = ByteArray(deflateBound(s.ptr, data.size.convert()).toInt() + 64)
            input.usePinned { i ->
                out.usePinned { o ->
                    s.next_in = i.addressOf(0).reinterpret()
                    s.avail_in = data.size.convert()
                    s.next_out = o.addressOf(0).reinterpret()
                    s.avail_out = out.size.convert()
                    check(platform.zlib.deflate(s.ptr, Z_FINISH) == Z_STREAM_END)
                }
            }
            out.copyOf(s.total_out.toInt())
        } finally {
            deflateEnd(s.ptr)
        }
    }

    actual fun inflate(data: ByteArray, max: Int): ByteArray? = memScoped {
        if (data.isEmpty()) return@memScoped null
        val s = alloc<z_stream>()
        s.zalloc = null
        s.zfree = null
        s.opaque = null
        s.next_in = null
        s.avail_in = 0u
        val version = zlibVersion()?.toKString()
        if (inflateInit2_(s.ptr, -15, version, sizeOf<z_stream>().convert()) != Z_OK) return@memScoped null
        try {
            // ein Byte Reserve: voller Puffer = zu gross
            val out = ByteArray(max + 1)
            val r = data.usePinned { i ->
                out.usePinned { o ->
                    s.next_in = i.addressOf(0).reinterpret()
                    s.avail_in = data.size.convert()
                    s.next_out = o.addressOf(0).reinterpret()
                    s.avail_out = out.size.convert()
                    platform.zlib.inflate(s.ptr, Z_FINISH)
                }
            }
            val n = s.total_out.toLong()
            if (r != Z_STREAM_END || n > max) null else out.copyOf(n.toInt())
        } finally {
            inflateEnd(s.ptr)
        }
    }
}
