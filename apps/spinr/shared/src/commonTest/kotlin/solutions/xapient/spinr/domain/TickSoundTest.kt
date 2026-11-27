package solutions.xapient.spinr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.math.abs

class TickSoundTest {
    @Test
    fun clickIsShortLoudThenSilent() {
        val pcm = TickSound.pcm()
        assertTrue(pcm.size in 1000..2000)
        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue(peak in 8_000..26_000, "peak=$peak")
        assertEquals(0, pcm[0].toInt())
        // ausgeklungen, damit kein Knacken am Ende entsteht
        val tail = pcm.takeLast(100).maxOf { abs(it.toInt()) }
        assertTrue(tail < 300, "tail=$tail")
    }

    @Test
    fun wavHeaderIsValid() {
        val pcm = shortArrayOf(0, 1000, -1000, 32767)
        val wav = TickSound.wav(pcm, 44_100)
        assertEquals(44 + 8, wav.size)
        assertEquals("RIFF", wav.decodeToString(0, 0 + 4))
        assertEquals("WAVE", wav.decodeToString(8, 8 + 4))
        assertEquals("data", wav.decodeToString(36, 36 + 4))
        fun le(at: Int, len: Int) = (0 until len).sumOf { (wav[at + it].toInt() and 0xFF) shl (8 * it) }
        assertEquals(44_100, le(24, 4))
        assertEquals(16, le(34, 2))
        assertEquals(8, le(40, 4))
        assertEquals(1000, le(46, 2))
        assertEquals(0xFC18, le(48, 2)) // -1000 als 16 bit
    }
}
