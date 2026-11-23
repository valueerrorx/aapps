package solutions.xapient.sleepr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DisplayTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    @Test fun defaultsShowEverything() {
        val d = Display(ctx)
        assertTrue(d.showWaves)
        assertTrue(d.showMixes)
    }

    @Test fun survivesNewInstance() {
        Display(ctx).apply { showWaves = false; showMixes = false }
        val d = Display(ctx)
        assertFalse(d.showWaves)
        assertFalse(d.showMixes)
    }

    @Test fun waveSliderOnlyWhenWaveOnAndShown() {
        assertTrue(Display.waveBoxVisible(waveOn = true, showWaves = true))
        assertFalse(Display.waveBoxVisible(waveOn = true, showWaves = false))
        assertFalse(Display.waveBoxVisible(waveOn = false, showWaves = true))
        assertFalse(Display.waveBoxVisible(waveOn = false, showWaves = false))
    }

    @Test fun hidingDoesNotTouchSound() {
        val before = Mixer.waves.copyOf()
        Display(ctx).showWaves = false
        assertEquals(before.toList(), Mixer.waves.toList())
    }
}
