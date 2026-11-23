package solutions.xapient.thinkr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TonesTest {
    @Test fun tonesAreShortAudibleAndEndQuietly() {
        for (k in Tones.Kind.entries) {
            val pcm = Tones.pcm(k)
            assertTrue(k.name, pcm.size in 1_000..Tones.SAMPLE_RATE)
            assertTrue(k.name, pcm.maxOf { abs(it.toInt()) } > 3_000)
            assertTrue(k.name, abs(pcm.last().toInt()) < 200)
        }
    }

    @Test fun wavHeader() {
        val wav = Tones.wav(ShortArray(10))
        assertEquals(64, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals("data", String(wav, 36, 4))
    }
}
