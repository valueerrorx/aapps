package solutions.xapient.roundr

import android.os.Looper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPowerManager
import solutions.xapient.roundr.domain.Workout
import java.time.Duration

// Dienst: Wakelock nur waehrend des Laufs, nach Fertig erst Signal ausklingen lassen, dann Stopp
@RunWith(RobolectricTestRunner::class)
class TimerServiceTest {
    private fun idle(ms: Long = 0) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test fun wakeLockFollowsRunAndStopWaitsForDoneSignal() {
        val store = RunStore.get(RuntimeEnvironment.getApplication())
        store.start(Workout(prepSec = 0, workSec = 5, restSec = 0, rounds = 1))
        val service = Robolectric.buildService(TimerService::class.java).create().startCommand(0, 1).get()
        idle()
        val wake = ShadowPowerManager.getLatestWakeLock()
        assertTrue(wake.isHeld)

        store.toggle()
        idle()
        assertFalse("angehalten", wake.isHeld)
        store.toggle()
        idle()
        assertTrue(wake.isHeld)

        idle(5_100)
        assertTrue("Fertig-Signal klingt aus", wake.isHeld)
        assertFalse(shadowOf(service).isStoppedBySelf)

        idle(TimerService.DONE_LINGER_MS)
        assertFalse(wake.isHeld)
        assertTrue(shadowOf(service).isStoppedBySelf)
        store.clear()
    }
}
