package solutions.xapient.thinkr.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.math.abs

class TonesTest {
    @Test fun tonesAreShortAudibleAndEndQuietly() {
        for (k in Tones.Kind.entries) {
            val pcm = Tones.pcm(k)
            assertTrue(pcm.size in 1_000..Tones.SAMPLE_RATE, k.name)
            assertTrue(pcm.maxOf { abs(it.toInt()) } > 3_000, k.name)
            assertTrue(abs(pcm.last().toInt()) < 200, k.name)
        }
    }

    @Test fun wavHeader() {
        val wav = Tones.wav(ShortArray(10))
        assertEquals(64, wav.size)
        assertEquals("RIFF", wav.decodeToString(0, 4))
        assertEquals("WAVE", wav.decodeToString(8, 12))
        assertEquals("data", wav.decodeToString(36, 40))
    }
}
