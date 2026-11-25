package solutions.xapient.thinkr.ui

import android.provider.Settings
import org.robolectric.RuntimeEnvironment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Animationsskala 0 im System schaltet Deko-Bewegung ab
@RunWith(RobolectricTestRunner::class)
class MotionTest {
    private val ctx = RuntimeEnvironment.getApplication()

    @After fun reset() { Motion.reduced = false }

    @Test fun scaleZeroIsReduced() {
        Settings.Global.putFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Motion.read(ctx)
        assertTrue(Motion.reduced)
        assertEquals(0f, Motion.still(0.08f), 0f)
    }

    @Test fun normalScaleMoves() {
        Settings.Global.putFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f)
        Motion.read(ctx)
        assertFalse(Motion.reduced)
        assertEquals(0.08f, Motion.still(0.08f), 0f)
    }
}
