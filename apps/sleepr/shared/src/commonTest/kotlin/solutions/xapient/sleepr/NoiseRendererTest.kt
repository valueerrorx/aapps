package solutions.xapient.sleepr

import solutions.xapient.sleepr.domain.FS
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoiseRendererTest {
    private val n = 4096
    private val l = FloatArray(n)
    private val r = FloatArray(n)

    @Test
    fun outputStaysBelowOne() {
        val mix = Mix().apply { levels[0] = 100; master = 100 }
        val nr = NoiseRenderer(mix, 42L)
        repeat(40) { nr.render(l, r, n) }
        assertTrue(l.all { abs(it) < 1f } && r.all { abs(it) < 1f })
        assertTrue(l.any { abs(it) > 0.01f })
    }

    @Test
    fun stopFadesOutWithinHalfASecond() {
        val nr = NoiseRenderer(Mix(), 1L)
        repeat(5) { assertFalse(nr.render(l, r, n)) }
        nr.stop()
        var blocks = 0
        while (!nr.render(l, r, n)) blocks++
        assertTrue(blocks * n < FS / 2)
        assertTrue(l.takeLast(100).all { it == 0f })
    }

    @Test
    fun timerCountsDown() {
        val mix = Mix().apply { timer = 15 }
        val nr = NoiseRenderer(mix, 1L)
        nr.render(l, r, 0)
        assertEquals(15 * 60 * 1000L, nr.remainingMs())
        assertFalse(nr.fading())
        // eine Sekunde spielen (12 Bloecke = 49152 Frames)
        repeat(12) { assertFalse(nr.render(l, r, n)) }
        assertEquals(15 * 60 * 1000L - 12L * n * 1000 / FS, nr.remainingMs())
    }

    @Test
    fun timerRestartsWhenChanged() {
        val mix = Mix().apply { timer = 15 }
        val nr = NoiseRenderer(mix, 1L)
        repeat(3) { nr.render(l, r, n) }
        mix.timer = 0
        nr.restartTimer()
        nr.render(l, r, n)
        assertEquals(-1L, nr.remainingMs())
    }

    @Test
    fun presetIsRecognisedUntilChanged() {
        val mix = Mix()
        val ocean = Mix.PRESETS.first { it.id == "ocean" }
        mix.applyPreset(ocean)
        assertTrue(mix.matches(ocean))
        assertTrue(mix.waves[2] && mix.waveSec[2] == 9)
        mix.levels[2] = 71
        assertFalse(mix.matches(ocean))
    }

    @Test
    fun formats() {
        assertEquals("1,5", oneDecimal(1.46, ','))
        assertEquals("30:00", fmtTime(30 * 60 * 1000L))
        assertEquals("1:00:01", fmtTime(3_600_500L))
    }
}
