package solutions.xapient.focusr

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import solutions.xapient.focusr.domain.Timer

// Alarm zum Ende, Receiver und Benachrichtigungen
@RunWith(RobolectricTestRunner::class)
class EndAlarmTest {
    private val ctx: Application get() = RuntimeEnvironment.getApplication()
    private val am get() = ctx.getSystemService(AlarmManager::class.java)
    private val nm get() = ctx.getSystemService(NotificationManager::class.java)
    private val min = Timer.MINUTE

    @Before fun setUp() {
        shadowOf(ctx).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Visibility.visible = false
    }

    @After fun tearDown() {
        Visibility.visible = false
    }

    @Test fun runningSchedulesExactAlarmAtEnd() {
        val now = System.currentTimeMillis()
        EndAlarm.sync(ctx, Timer(10 * min).start(now), now)
        val a = shadowOf(am).peekNextScheduledAlarm()
        assertNotNull(a)
        assertEquals(now + 10 * min, a!!.triggerAtMs)
        assertEquals(AlarmManager.RTC_WAKEUP, a.type)
        assertTrue(a.isAllowWhileIdle)
    }

    @Test fun pauseResetAndDoneCancel() {
        val now = System.currentTimeMillis()
        val run = Timer(10 * min).start(now)
        EndAlarm.sync(ctx, run, now)
        EndAlarm.sync(ctx, run.pause(now + 1000), now + 1000)
        assertNull(shadowOf(am).peekNextScheduledAlarm())
        EndAlarm.sync(ctx, run, now)
        EndAlarm.sync(ctx, run.reset(), now)
        assertNull(shadowOf(am).peekNextScheduledAlarm())
        EndAlarm.sync(ctx, run, now + 11 * min)
        assertNull(shadowOf(am).peekNextScheduledAlarm())
    }

    // Nur ein Alarm, auch wenn mehrfach gesetzt wird
    @Test fun rescheduleReplaces() {
        val now = System.currentTimeMillis()
        val run = Timer(10 * min).start(now)
        EndAlarm.sync(ctx, run, now)
        EndAlarm.sync(ctx, run.set(3 * min, now), now)
        assertEquals(1, shadowOf(am).scheduledAlarms.size)
        assertEquals(now + 3 * min, shadowOf(am).peekNextScheduledAlarm()!!.triggerAtMs)
    }

    private fun fire(t: Timer) {
        Prefs(ctx).apply {
            timer = t
            sound = false
        }
        EndReceiver().onReceive(ctx, Intent(EndAlarm.ACTION))
    }

    @Test fun receiverShowsTimeIsUpInBackground() {
        val now = System.currentTimeMillis()
        Notifier.showRunning(ctx, now + 5000)
        fire(Timer(min).start(now - min - 100))
        val n = shadowOf(nm).getNotification(Notifier.ID_DONE)
        assertNotNull(n)
        assertEquals("Time's up", n.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("1:00 are over", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        // Countdown ist weg
        assertNull(shadowOf(nm).getNotification(Notifier.ID_RUNNING))
    }

    @Test fun receiverQuietWhenAppVisible() {
        Visibility.visible = true
        fire(Timer(min).start(System.currentTimeMillis() - 2 * min))
        assertNull(shadowOf(nm).getNotification(Notifier.ID_DONE))
    }

    // Veralteter Alarm nach Pause oder Verlaengern
    @Test fun receiverIgnoresStaleAlarm() {
        val now = System.currentTimeMillis()
        fire(Timer(10 * min).start(now).pause(now + 1000))
        assertNull(shadowOf(nm).getNotification(Notifier.ID_DONE))
        fire(Timer(10 * min).start(now))
        assertNull(shadowOf(nm).getNotification(Notifier.ID_DONE))
    }

    @Test fun receiverIgnoresOtherActions() {
        Prefs(ctx).timer = Timer(min).start(System.currentTimeMillis() - 2 * min)
        EndReceiver().onReceive(ctx, Intent("x"))
        assertNull(shadowOf(nm).getNotification(Notifier.ID_DONE))
    }

    @Test fun noNotificationWithoutPermission() {
        shadowOf(ctx).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        fire(Timer(min).start(System.currentTimeMillis() - 2 * min))
        assertNull(shadowOf(nm).getNotification(Notifier.ID_DONE))
    }

    @Test fun runningNotificationCountsDownSilently() {
        val end = System.currentTimeMillis() + 7 * min
        Notifier.showRunning(ctx, end)
        val n = shadowOf(nm).getNotification(Notifier.ID_RUNNING)
        assertNotNull(n)
        assertEquals(end, n.`when`)
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(NotificationManager.IMPORTANCE_LOW, nm.getNotificationChannel(n.channelId).importance)
    }
}
