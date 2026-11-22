package solutions.xapient.hertzr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.hertzr.domain.Level
import solutions.xapient.hertzr.domain.Mode
import solutions.xapient.hertzr.domain.Params
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.domain.SweepSpec
import solutions.xapient.hertzr.domain.Wave

// Ablauf ueber den echten Store mit einem Player ohne Audio
class FakePlayer(var works: Boolean = true) : Player {
    var started: Params? = null
    val updates = mutableListOf<Params>()
    var stops = 0
    var live: Double? = 1_234.0
    override var onEnd: (() -> Unit)? = null
    override fun start(p: Params): Boolean {
        if (!works) return false
        started = p
        return true
    }
    override fun update(p: Params) {
        updates += p
    }
    override fun stop() {
        stops++
    }
    override fun liveHz(): Double? = live
    override fun progress(): Float? = 0.4f
}

class ToneStoreTest {
    private var saved: Settings? = null

    private fun store(player: Player = FakePlayer(), load: Settings = Settings(), maxHz: Double = 22_000.0) =
        ToneStore({ load }, { saved = it }, player, maxHz)

    @Test fun loadsSaneAndQuietSettings() {
        val s = store(load = Settings(hz = 99_999.0, levelDb = 0), maxHz = 21_609.0)
        assertEquals(21_609.0, s.settings.value.hz, 0.0)
        // nach Neustart nie lauter als -12 dB
        assertEquals(-12, s.settings.value.levelDb)
        assertEquals(-30, store(load = Settings(levelDb = -30)).settings.value.levelDb)
    }

    @Test fun changesAreSavedAndClamped() {
        val s = store()
        assertTrue(s.nudge(1))
        assertEquals(441.0, saved!!.hz, 0.0)
        assertTrue(s.octave(true))
        assertEquals(882.0, saved!!.hz, 0.0)
        s.setHz(0.1)
        assertEquals(1.0, s.settings.value.hz, 0.0)
        assertFalse(s.nudge(-1))
        assertTrue(s.setLevel(-6))
        assertFalse(s.setLevel(-6))
        s.setLevel(9)
        assertEquals(0, saved!!.levelDb)
    }

    @Test fun liveChangesReachPlayerOnlyWhilePlaying() {
        val p = FakePlayer()
        val s = store(p)
        s.setWave(Wave.SQUARE)
        assertTrue(p.updates.isEmpty())
        assertTrue(s.start())
        assertEquals(Wave.SQUARE, p.started!!.wave)
        s.setHz(1_000.0)
        assertEquals(1_000.0, p.updates.last().hz, 0.0)
        s.setLevel(-40)
        assertEquals(0.01f, p.updates.last().amp, 1e-6f)
    }

    @Test fun sweepParamsAndModeSwitchStops() {
        val p = FakePlayer()
        val s = store(p)
        s.setMode(Mode.SWEEP)
        s.setSweepFrom(50.0)
        s.nextDuration()
        s.start()
        assertEquals(SweepSpec(50.0, 20_000.0, 20), p.started!!.sweep)
        assertEquals(1_234.0, s.shownHz(), 0.0)
        assertEquals(0.4f, s.progress()!!, 0f)
        s.setMode(Mode.TONE)
        assertFalse(s.playing.value)
        assertEquals(1, p.stops)
        assertEquals(440.0, s.shownHz(), 0.0)
    }

    // gestoppt zeigt der Sweep seinen Start
    @Test fun shownHzWhenIdle() {
        val s = store(load = Settings(mode = Mode.SWEEP, sweepFrom = 30.0))
        assertEquals(30.0, s.shownHz(), 0.0)
        assertNull(s.progress())
    }

    // Kopfhoerer kommt waehrend des Spielens: Pegel auf -30 dB; leiser bleibt, gestoppt bleibt
    @Test fun headphonesAddedWhilePlayingLowerLevel() {
        val p = FakePlayer()
        val s = store(p, load = Settings(levelDb = -12))
        s.setHeadphones(true)
        assertEquals(-12, s.settings.value.levelDb)
        s.setHeadphones(false)
        s.start()
        s.setHeadphones(true)
        assertEquals(-30, s.settings.value.levelDb)
        assertEquals(Level.amp(-30), p.updates.last().amp, 1e-6f)
        // weiter dran (zweites Geraet): nichts mehr
        s.setLevel(-20)
        s.setHeadphones(true)
        assertEquals(-20, s.settings.value.levelDb)
        val quiet = store(FakePlayer(), load = Settings(levelDb = -40))
        quiet.start()
        quiet.setHeadphones(true)
        assertEquals(-40, quiet.settings.value.levelDb)
    }

    @Test fun startFailureStaysStopped() {
        val s = store(FakePlayer(works = false))
        assertFalse(s.start())
        assertFalse(s.playing.value)
    }

    // Sweep fertig oder Ausgabe weg (Audio-Thread): gestoppt
    @Test fun endFromAudioStops() {
        val p = FakePlayer()
        val s = store(p)
        s.start()
        p.onEnd!!.invoke()
        assertFalse(s.playing.value)
    }

    @Test fun stopIsIdempotent() {
        val p = FakePlayer()
        val s = store(p)
        s.stop()
        assertEquals(0, p.stops)
        s.toggle()
        s.toggle()
        s.stop()
        assertEquals(1, p.stops)
    }
}
