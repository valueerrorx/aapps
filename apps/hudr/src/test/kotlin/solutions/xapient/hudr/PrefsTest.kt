package solutions.xapient.hudr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import solutions.xapient.hudr.domain.SpeedUnit
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class PrefsTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    @Test fun defaultsFollowLocale() {
        assertEquals(SpeedUnit.KMH, Prefs(ctx) { Locale.GERMANY }.settings.unit)
        val us = Prefs(ctx) { Locale.US }.settings
        assertEquals(SpeedUnit.MPH, us.unit)
        assertEquals(30, us.limit)
        assertFalse(us.limitOn)
        assertNull(us.activeLimit)
        assertTrue(us.mirror)
        assertFalse(Prefs(ctx).askedLocation)
    }

    @Test fun roundTrip() {
        val s = Settings(SpeedUnit.KN, limitOn = true, limit = 25, sound = false, keepOn = false, mirror = false, bright = true)
        Prefs(ctx).settings = s
        Prefs(ctx).askedLocation = true
        // gespeicherte Einheit gewinnt gegen die Locale
        val p = Prefs(ctx) { Locale.US }
        assertEquals(s, p.settings)
        assertEquals(25, p.settings.activeLimit)
        assertTrue(p.askedLocation)
    }

    @Test fun badValuesFallBack() {
        ctx.getSharedPreferences("hudr", 0).edit().putString("unit", "FURLONG").putInt("limit", 999).commit()
        val s = Prefs(ctx) { Locale.GERMANY }.settings
        assertEquals(SpeedUnit.KMH, s.unit)
        assertEquals(300, s.limit)
    }

    @Test fun unitChangeConvertsLimit() {
        val s = Settings(SpeedUnit.KMH, limit = 100).withUnit(SpeedUnit.MPH)
        assertEquals(SpeedUnit.MPH, s.unit)
        assertEquals(60, s.limit)
        assertEquals(s, s.withUnit(SpeedUnit.MPH))
    }
}
