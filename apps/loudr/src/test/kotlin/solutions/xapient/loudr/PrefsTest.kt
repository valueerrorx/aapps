package solutions.xapient.loudr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import solutions.xapient.loudr.domain.TimeWeighting

@RunWith(RobolectricTestRunner::class)
class PrefsTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    @Test fun defaults() {
        val p = Prefs(ctx)
        assertEquals(Settings(), p.settings)
        assertEquals(false, p.askedMic)
    }

    @Test fun roundTrip() {
        Prefs(ctx).settings = Settings(TimeWeighting.SLOW, -3.5f, false)
        Prefs(ctx).askedMic = true
        val p = Prefs(ctx)
        assertEquals(Settings(TimeWeighting.SLOW, -3.5f, false), p.settings)
        assertTrue(p.askedMic)
    }

    @Test fun badValuesFallBack() {
        ctx.getSharedPreferences("loudr", 0).edit().putString("weighting", "MEDIUM").putFloat("offset", 99f).commit()
        val s = Prefs(ctx).settings
        assertEquals(TimeWeighting.FAST, s.weighting)
        assertEquals(20f, s.offset, 0f)
    }
}
