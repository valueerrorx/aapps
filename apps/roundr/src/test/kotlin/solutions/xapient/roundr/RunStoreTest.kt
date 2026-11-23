package solutions.xapient.roundr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.roundr.domain.Presets
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Session

class RunStoreTest {
    private var now = 0L
    private val saved = ArrayList<Run?>()

    private fun store(loaded: Run?, boot: Int) = RunStore({ loaded }, { saved += it }, { now }, boot)

    // Nach Neustart angehalten und sofort gespeichert
    @Test fun restoredStateIsSavedRightAway() {
        now = 600_000
        val s = store(Run(Presets.default, Session(12_000, 1_000), 1, boot = 3), boot = 4)
        val r = s.state.value!!
        assertFalse(r.session.running)
        assertEquals(12_000, r.session.elapsed(now))
        assertEquals(listOf<Run?>(r), saved)
    }

    @Test fun unchangedStateIsNotRewritten() {
        now = 5_000
        store(Run(Presets.default, Session(0, 1_000), 1, boot = 4), boot = 4)
        assertTrue(saved.isEmpty())
    }

    @Test fun checkpointMovesOffsetWhileRunning() {
        val s = store(null, boot = 4)
        s.start(Presets.default)
        now = 12_000
        s.checkpoint()
        assertEquals(Session(12_000, 12_000), saved.last()!!.session)
        assertEquals(4, saved.last()!!.boot)
        s.pause()
        val n = saved.size
        s.checkpoint()
        assertEquals(n, saved.size)
    }
}
