package solutions.xapient.tunr

import androidx.core.content.edit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Notation
import solutions.xapient.tunr.domain.Settings

// Einstellungen ueberstehen Prozess-Tod: neue Prefs-Instanz liest dasselbe
@RunWith(RobolectricTestRunner::class)
class PrefsTest {
    private val ctx get() = RuntimeEnvironment.getApplication()

    @Test fun defaultsThenRoundTrip() {
        assertEquals(Settings(), Prefs(ctx).settings)
        assertFalse(Prefs(ctx).askedMic)
        val s = Settings(a4 = 442, notation = Notation.GERMAN, instrument = Instrument.UKULELE, haptics = false)
        Prefs(ctx).settings = s
        Prefs(ctx).askedMic = true
        assertFalse(Prefs(ctx).micBlocked)
        Prefs(ctx).micBlocked = true
        assertEquals(s, Prefs(ctx).settings)
        assertTrue(Prefs(ctx).askedMic)
        assertTrue(Prefs(ctx).micBlocked)
    }

    @Test fun brokenValuesFallBack() {
        ctx.getSharedPreferences("tunr", 0).edit {
            putInt("a4", 1000)
            putString("instrument", "BANJO")
            putString("notation", null)
        }
        val s = Prefs(ctx).settings
        assertEquals(Settings.A4_MAX, s.a4)
        assertEquals(Instrument.CHROMATIC, s.instrument)
        assertEquals(Notation.SHARP, s.notation)
    }
}
