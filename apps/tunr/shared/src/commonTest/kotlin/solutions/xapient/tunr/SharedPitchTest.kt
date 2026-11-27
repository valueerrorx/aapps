package solutions.xapient.tunr

import solutions.xapient.tunr.domain.Notes
import solutions.xapient.tunr.domain.PitchDetector
import solutions.xapient.tunr.domain.SlidingWindow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Kurzer Lauf auf allen Plattformen (auch iOS); die ausfuehrlichen Tests liegen im Android-Modul
class SharedPitchTest {
    @Test
    fun detectsA4ThroughWindow() {
        val rate = 48_000
        val det = PitchDetector(rate)
        val hop = rate / 40
        val win = SlidingWindow(det.size, hop)
        val pcm = ShortArray(hop)
        var t = 0L
        repeat(det.size / hop + 2) {
            for (i in 0 until hop) pcm[i] = (0.3 * 32767 * sin(2 * PI * 440.0 * t++ / rate)).toInt().toShort()
            win.push(pcm)
        }
        val p = assertNotNull(det.detect(win.data))
        assertTrue(abs(p.hz - 440f) < 1f, "erkannt ${p.hz}")
    }

    @Test
    fun noteMath() {
        assertTrue(abs(Notes.hz(69, 440f) - 440f) < 0.01f)
    }
}
