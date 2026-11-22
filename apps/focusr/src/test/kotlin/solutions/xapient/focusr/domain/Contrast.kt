package solutions.xapient.focusr.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// WCAG-2-Kontrast zweier ARGB-Farben; Alpha wird ueber den Hintergrund gemischt
object Contrast {
    fun over(fg: Int, bg: Int): Int {
        val a = (fg ushr 24 and 0xFF) / 255.0
        fun ch(shift: Int): Int {
            val f = fg shr shift and 0xFF
            val b = bg shr shift and 0xFF
            return (f * a + b * (1 - a)).toInt()
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun luminance(c: Int): Double {
        fun lin(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin(c shr 16 and 0xFF) + 0.7152 * lin(c shr 8 and 0xFF) + 0.0722 * lin(c and 0xFF)
    }

    fun ratio(fg: Int, bg: Int): Double {
        val l1 = luminance(over(fg, bg))
        val l2 = luminance(bg)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }
}
