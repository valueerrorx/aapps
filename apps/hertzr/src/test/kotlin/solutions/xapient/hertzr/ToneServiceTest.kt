package solutions.xapient.hertzr

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
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
import solutions.xapient.hertzr.domain.Settings

// Dienst: Vordergrund und Wakelock nur waehrend des Spielens; Stopp aus der Benachrichtigung
@RunWith(RobolectricTestRunner::class)
class ToneServiceTest {
    private val store = ToneStore({ Settings() }, {}, FakePlayer())

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @After fun reset() = ToneStore.install(null)

    private fun started(): ToneService {
        ToneStore.install(store)
        store.start()
        val service = Robolectric.buildService(ToneService::class.java).create().startCommand(0, 1).get()
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
        service.onStartCommand(Intent(service, ToneService::class.java).setAction(ToneService.ACTION_STOP), 0, 2)
        idle()
        assertFalse(store.playing.value)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    // Band ziehen: eine Benachrichtigung nach der Ruhe, nicht eine je Schritt
    @Test fun notificationDebounced() {
        started()
        val nm = shadowOf(RuntimeEnvironment.getApplication().getSystemService(NotificationManager::class.java))
        fun title() = nm.allNotifications.single().extras.getString(Notification.EXTRA_TITLE)
        assertEquals("440 Hz", title())
        repeat(5) { store.nudge(1) }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals("440 Hz", title())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        assertEquals("445 Hz", title())
    }

    @Test fun notPlayingDoesNotStay() {
        ToneStore.install(store)
        val service = Robolectric.buildService(ToneService::class.java).create().startCommand(0, 1).get()
        idle()
        assertTrue(shadowOf(service).isStoppedBySelf)
    }
}
