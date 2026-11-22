package solutions.xapient.pulsr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.Subdivision

// Ablauf ueber den echten Store mit einem Player ohne Audio
class FakePlayer(var works: Boolean = true) : Player {
    var started: Settings? = null
    var updates = mutableListOf<Settings>()
    var stops = 0
    override var onFailure: (() -> Unit)? = null
    override fun start(s: Settings): Boolean {
        if (!works) return false
        started = s
        return true
    }
    override fun update(s: Settings) {
        updates += s
    }
    override fun stop() {
        stops++
    }
    override fun pulse(nowNanos: Long): Pulse? = Pulse(1, 4, 0.5f, 1, 0, false, 250f, 250f, 500f)
}

class PulseStoreTest {
    private var saved: Settings? = null
    private var now = 0L

    private fun store(player: Player = FakePlayer(), load: Settings = Settings()) =
        PulseStore({ load }, { saved = it }, player) { now }

    @Test fun loadsSaneSettings() {
        val s = store(load = Settings(bpm = 900, beats = 0))
        assertEquals(300, s.settings.value.bpm)
        assertEquals(1, s.settings.value.beats)
    }

    @Test fun changesAreSavedAndClamped() {
        val s = store()
        s.nudge(+5)
        assertEquals(125, saved!!.bpm)
        s.setBpm(5)
        assertEquals(20, s.settings.value.bpm)
        s.setBeats(13)
        assertEquals(12, saved!!.beats)
    }

    @Test fun liveChangesReachPlayerOnlyWhilePlaying() {
        val p = FakePlayer()
        val s = store(p)
        s.nudge(1)
        assertTrue(p.updates.isEmpty())
        assertTrue(s.start())
        assertEquals(121, p.started!!.bpm)
        s.set(s.settings.value.copy(sub = Subdivision.TRIPLET))
        assertEquals(Subdivision.TRIPLET, p.updates.last().sub)
    }

    @Test fun startFailureStaysStopped() {
        val s = store(FakePlayer(works = false))
        assertFalse(s.start())
        assertFalse(s.playing.value)
    }

    @Test fun audioFailureStops() {
        val p = FakePlayer()
        val s = store(p)
        s.start()
        p.onFailure!!.invoke()
        assertFalse(s.playing.value)
    }

    @Test fun pulseOnlyWhilePlaying() {
        val s = store()
        assertNull(s.pulse(0))
        s.toggle()
        assertEquals(1, s.pulse(0)!!.beat)
        s.toggle()
        assertNull(s.pulse(0))
    }

    @Test fun tapSetsTempoFromSecondTap() {
        val s = store()
        now = 1_000
        assertFalse(s.tap())
        now = 1_400
        assertTrue(s.tap())
        assertEquals(150, s.settings.value.bpm)
        assertEquals(150, saved!!.bpm)
    }

    @Test fun stopIsIdempotent() {
        val p = FakePlayer()
        val s = store(p)
        s.stop()
        assertEquals(0, p.stops)
        s.start()
        s.stop()
        s.stop()
        assertEquals(1, p.stops)
    }
}
