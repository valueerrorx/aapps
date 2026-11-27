package solutions.xapient.hertzr

import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Level
import solutions.xapient.hertzr.domain.Params
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.domain.ToneRenderer
import solutions.xapient.hertzr.domain.params
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Kurzer Lauf auf allen Plattformen (auch iOS); die ausfuehrlichen Tests liegen im Android-Modul
class SharedToneTest {
    private class FakePlayer : Player {
        var started = 0
        override fun start(p: Params): Boolean { started++; return true }
        override fun update(p: Params) {}
        override fun stop() {}
        override fun liveHz(): Double? = null
        override fun progress(): Float? = null
        override var onEnd: (() -> Unit)? = null
    }

    @Test
    fun rendererFadesInAndStaysBelowOne() {
        val r = ToneRenderer(48_000, Settings(hz = 1_000.0).params())
        val buf = FloatArray(2 * 4_800)
        r.render(buf, 4_800)
        val peak = buf.maxOf { abs(it) }
        assertTrue(peak > 0.01f && peak <= 1f, "Spitze $peak")
        r.release()
        repeat(10) { r.render(buf, 480) }
        assertTrue(r.done)
    }

    @Test
    fun storeSavesAndRestoresQuietly() {
        var saved: Settings? = null
        val store = ToneStore({ Settings(levelDb = 0) }, { saved = it }, FakePlayer())
        assertTrue(store.settings.value.levelDb <= Level.RESTORE_MAX_DB)
        assertTrue(store.setHz(440.0) || store.settings.value.hz == 440.0)
        assertTrue(store.start())
        assertTrue(store.playing.value)
        store.stop()
        assertFalse(store.playing.value)
        store.setHz(880.0)
        assertEquals(880.0, saved?.hz)
    }

    @Test
    fun formats() {
        assertEquals("440 Hz", Freq.short(440.0))
        assertEquals("1,5 kHz", Freq.short(1_500.0, ','))
    }
}
