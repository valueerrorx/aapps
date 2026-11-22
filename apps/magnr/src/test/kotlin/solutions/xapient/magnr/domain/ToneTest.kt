package solutions.xapient.magnr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneTest {
    @Test fun silentBelowWeak() {
        assertTrue(ToneMap.silent(0.1f))
        assertTrue(ToneMap.intervalMs(0.1f).isInfinite())
        assertTrue(!ToneMap.silent(Level.WEAK.from))
    }

    @Test fun pitchSpansTwoOctaves() {
        assertEquals(520f, ToneMap.pitchHz(Level.WEAK.from), 0.01f)
        assertEquals(2080f, ToneMap.pitchHz(1f), 0.01f)
        assertEquals(1040f, ToneMap.pitchHz((Level.WEAK.from + 1f) / 2f), 0.5f)
    }

    @Test fun beepsGetDenserThenContinuous() {
        assertEquals(600f, ToneMap.intervalMs(Level.WEAK.from), 0.01f)
        var last = Float.MAX_VALUE
        for (i in 12..94) {
            val v = ToneMap.intervalMs(i / 100f)
            assertTrue("$i: $v", v < last)
            last = v
        }
        assertTrue(last >= ToneMap.MIN_INTERVAL_MS)
        assertEquals(0f, ToneMap.intervalMs(ToneMap.CONTINUOUS), 0f)
    }

    private fun render(strength: Float, seconds: Float, synth: BeepSynth = BeepSynth(RATE)): ShortArray {
        val out = ShortArray((RATE * seconds).toInt())
        // in kleinen Bloecken wie der Audio-Thread
        var off = 0
        val block = ShortArray(480)
        while (off < out.size) {
            val n = minOf(block.size, out.size - off)
            synth.render(block, n, strength)
            block.copyInto(out, off, 0, n)
            off += n
        }
        return out
    }

    @Test fun silenceIsZero() {
        assertTrue(render(0.05f, 0.5f).all { it == 0.toShort() })
    }

    @Test fun continuousToneHasPitch() {
        val pcm = render(1f, 1f)
        // Nulldurchgaenge nach oben zaehlen
        var ups = 0
        for (i in 1 until pcm.size) if (pcm[i - 1] < 0 && pcm[i] >= 0) ups++
        assertEquals(2080.0, ups.toDouble(), 3.0)
    }

    @Test fun beepRateMatchesInterval() {
        val s = 0.5f
        val pcm = render(s, 2f)
        // Einsaetze: Uebergang von Stille zu Ton
        var onsets = 0
        var quiet = RATE
        for (v in pcm) {
            if (v.toInt() != 0) {
                if (quiet > RATE / 200) onsets++
                quiet = 0
            } else {
                quiet++
            }
        }
        val expected = 2000f / ToneMap.intervalMs(s)
        assertEquals(expected.toDouble(), onsets.toDouble(), 1.5)
    }

    @Test fun noClicks() {
        // groesster Sprung zwischen zwei Samples: Sinus bei 2080 Hz, keine Kanten
        val synth = BeepSynth(RATE)
        val pcm = render(0.7f, 1f, synth) + render(1f, 0.3f, synth) + render(0f, 0.2f, synth)
        val amp = 0.3f * Short.MAX_VALUE
        val maxStep = amp * 2 * Math.PI * 2080 / RATE * 1.05 + 20
        for (i in 1 until pcm.size) assertTrue("Sprung bei $i", abs(pcm[i] - pcm[i - 1]) <= maxStep)
        assertEquals(0, pcm.last().toInt())
    }

    private companion object {
        const val RATE = 48_000
    }
}
