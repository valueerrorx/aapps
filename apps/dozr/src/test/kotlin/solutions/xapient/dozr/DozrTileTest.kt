package solutions.xapient.dozr

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import solutions.xapient.dozr.domain.Mode

// Kachel: Tipp startet mit dem letzten Wert, zweiter Tipp stoppt
@RunWith(RobolectricTestRunner::class)
class DozrTileTest {
    @Before fun setUp() {
        SleepStore.reset()
        SleepService.alive = false
    }

    @After fun tearDown() = SleepStore.reset()

    @Test fun clickStartsWithLastValueThenStops() {
        val app = RuntimeEnvironment.getApplication()
        val store = SleepStore.get(app)
        store.choose(45)
        val tile = Robolectric.buildService(DozrTile::class.java).create().get()
        tile.onClick()
        val s = store.state.value
        assertEquals(Mode.RUNNING, s.mode)
        assertEquals(45 * 60_000L, s.totalMs)
        assertEquals(SleepService::class.java.name, shadowOf(app).nextStartedService.component!!.className)
        tile.onClick()
        assertEquals(Mode.IDLE, store.state.value.mode)
        assertEquals(45, store.state.value.setMin)
    }
}
