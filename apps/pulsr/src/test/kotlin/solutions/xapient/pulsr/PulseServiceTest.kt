package solutions.xapient.pulsr

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPowerManager
import solutions.xapient.pulsr.domain.Settings

// Dienst: Vordergrund und Wakelock nur waehrend des Spielens; Stopp aus Benachrichtigung und bei Kopfhoerer ab
@RunWith(RobolectricTestRunner::class)
class PulseServiceTest {
    private val store = PulseStore({ Settings() }, {}, FakePlayer())

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @After fun reset() = PulseStore.install(null)

    private fun started(): PulseService {
        PulseStore.install(store)
        store.start()
        val service = Robolectric.buildService(PulseService::class.java).create().startCommand(0, 1).get()
        idle()
        return service
    }

    @Test fun wakeLockWhilePlayingThenStops() {
        val service = started()
        val wake = ShadowPowerManager.getLatestWakeLock()
        assertTrue(wake.isHeld)
        assertTrue(shadowOf(service).isLastForegroundNotificationAttached)
        store.stop()
        idle()
        assertFalse(wake.isHeld)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test fun stopActionStopsPlaying() {
        val service = started()
        service.onStartCommand(Intent(service, PulseService::class.java).setAction(PulseService.ACTION_STOP), 0, 2)
        idle()
        assertFalse(store.playing.value)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test fun headphonesUnpluggedStops() {
        started()
        RuntimeEnvironment.getApplication().sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        idle()
        assertFalse(store.playing.value)
    }

    @Test fun callStops() {
        started()
        RuntimeEnvironment.getApplication().getSystemService(AudioManager::class.java).mode = AudioManager.MODE_IN_CALL
        idle()
        assertFalse(store.playing.value)
    }

    // Rad drehen: eine Benachrichtigung nach der Ruhe, nicht eine je BPM
    @Test fun notificationDebounced() {
        started()
        val nm = shadowOf(RuntimeEnvironment.getApplication().getSystemService(NotificationManager::class.java))
        fun title() = nm.allNotifications.single().extras.getString(Notification.EXTRA_TITLE)
        val before = title()
        repeat(5) { store.nudge(1) }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals(before, title())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        assertTrue(title()!!.contains("125"))
    }

    @Test fun notPlayingDoesNotStay() {
        PulseStore.install(store)
        val service = Robolectric.buildService(PulseService::class.java).create().startCommand(0, 1).get()
        idle()
        assertTrue(shadowOf(service).isStoppedBySelf)
    }
}
