package solutions.xapient.loudr

import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.domain.SoundMeter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Kurzer Lauf auf allen Plattformen (auch iOS); die ausfuehrlichen Tests liegen im Android-Modul
class SharedMeterTest {
    private fun tone(meter: SoundMeter, amp: Double, seconds: Int) {
        val rate = 48_000
        val buf = ShortArray(rate / 50)
        var t = 0L
        repeat(seconds * 50) {
            for (i in buf.indices) buf[i] = (amp * 32767 * sin(2 * PI * 1_000 * t++ / rate)).toInt().toShort()
            meter.feed(buf, buf.size)
        }
    }

    @Test
    fun louderToneReadsSixDbHigher() {
        val a = SoundMeter().apply { restart(48_000) }
        val b = SoundMeter().apply { restart(48_000) }
        tone(a, 0.05, 2)
        tone(b, 0.1, 2)
        val d = b.reading().fast - a.reading().fast
        assertTrue(abs(d - 6.02f) < 0.3f, "Differenz $d")
    }

    @Test
    fun calibrationLabel() {
        assertEquals(0f, Calibration.clamp(0f))
        assertTrue(Calibration.apply(Float.NaN, 3f).isNaN())
    }
}
