package solutions.xapient.pulsr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RendererTest {
    private fun render(s: Settings, frames: Int, block: Int, rate: Int = 48_000): FloatArray {
        val r = Renderer(rate, s)
        val all = FloatArray(frames)
        val buf = FloatArray(block)
        var at = 0
        while (at < frames) {
            val n = minOf(block, frames - at)
            r.render(buf, n)
            System.arraycopy(buf, 0, all, at, n)
            at += n
        }
        return all
    }

    private fun firstSound(a: FloatArray, from: Int): Int = (from until a.size).first { abs(a[it]) > 1e-4f }

    // Stille bis zum Klick, Klang ab genau dem Sample
    @Test fun onsetOnTheSample() {
        val a = render(Settings(bpm = 120, sound = Sound.CLICK), 48_000, 333)
        assertTrue(firstSound(a, 0) in 0..2)
        // zweiter Schlag bei 24000: davor klingt der erste aus, dann Stille
        assertTrue((20_000 until 24_000).all { a[it] == 0f })
        assertTrue(firstSound(a, 20_000) in 24_000..24_003)
    }

    @Test fun blocksSplitClickSeamlessly() {
        // Zweierpotenz-Rate: Klicks liegen exakt auf ganzen Frames, Ergebnis muss gleich sein
        val s = Settings(bpm = 120, sub = Subdivision.SIXTEENTH, sound = Sound.WOOD)
        val a = render(s, 65_536, 4_096, rate = 32_768)
        val b = render(s, 65_536, 37, rate = 32_768)
        for (i in a.indices) assertEquals("Sample $i", a[i], b[i], 1e-6f)
    }

    @Test fun accentLouderThanBeatLouderThanSub() {
        for (sound in Sound.entries) {
            val set = Clicks.make(sound, 48_000)
            val peak = { f: FloatArray -> f.maxOf { abs(it) } }
            assertTrue(sound.name, peak(set.accent) > peak(set.beat))
            assertTrue(sound.name, peak(set.beat) > peak(set.sub))
            assertTrue(sound.name, peak(set.accent) <= 1f)
            // weich ausgeblendet: letztes Sample still
            assertEquals(0f, set.beat.last(), 1e-6f)
        }
    }

    // Sechzehntel bei 300 BPM: Stimmen ueberlappen, nichts geht verloren, nichts uebersteuert
    @Test fun denseClicksStayInRange() {
        val a = render(Settings(bpm = 300, sub = Subdivision.SIXTEENTH, sound = Sound.WOOD), 48_000, 256)
        val pcm = ShortArray(a.size)
        Renderer.toPcm(a, pcm, a.size)
        assertTrue(pcm.any { it > 20_000 })
        // jede Sechzehntel (2400 Frames) hat einen Anschlag
        for (k in 0 until 19) assertTrue("Klick $k fehlt", (k * 2_400 until k * 2_400 + 40).any { abs(a[it]) > 0.05f })
    }

    @Test fun soundSwitchApplies() {
        val r = Renderer(48_000, Settings(bpm = 60, sound = Sound.BEEP))
        val buf = FloatArray(48_000)
        r.render(buf, 48_000)
        val beepLen = (buf.indexOfLast { it != 0f })
        r.apply(Settings(bpm = 60, sound = Sound.WOOD))
        r.render(buf, 48_000)
        val woodLen = (buf.indexOfLast { it != 0f })
        assertTrue("$beepLen vs $woodLen", woodLen > beepLen)
    }

    @Test fun pcmClamps() {
        val src = floatArrayOf(2f, -2f, 0.5f)
        val dst = ShortArray(3)
        Renderer.toPcm(src, dst, 3)
        assertEquals(Short.MAX_VALUE, dst[0])
        assertEquals((-Short.MAX_VALUE).toShort(), dst[1])
        assertTrue(abs(Short.MAX_VALUE / 2 - dst[2]) <= 1)
    }
}
