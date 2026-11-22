package solutions.xapient.dozr

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Looper
import android.os.SystemClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowPowerManager
import org.robolectric.shadows.ShadowSensor
import solutions.xapient.dozr.domain.FadeLength
import solutions.xapient.dozr.domain.FadeMemo
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.domain.Sleep
import solutions.xapient.dozr.domain.Sleep.Companion.MINUTE
import java.time.Duration

// Dienst: Ausblenden, Fokus, Lautstaerke zurueck, Nutzer-Aenderung, Prozess-Tod, Schuetteln
@RunWith(RobolectricTestRunner::class)
class SleepServiceTest {
    private lateinit var app: Application
    private lateinit var am: AudioManager
    private lateinit var prefs: Prefs
    private var controller: ServiceController<SleepService>? = null

    private fun idle(ms: Long = 0) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private val volume get() = am.getStreamVolume(AudioManager.STREAM_MUSIC)

    @Before fun setUp() {
        SleepStore.reset()
        SleepService.alive = false
        app = RuntimeEnvironment.getApplication()
        am = app.getSystemService(AudioManager::class.java)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0)
        prefs = Prefs(app)
        prefs.fade = FadeLength.M1
        SleepService.denyForeground = false
        // Zeit vorstellen: Laufzeituhr steht im Test sonst bei fast 0
        idle(60 * MINUTE)
    }

    @After fun tearDown() {
        controller?.destroy()
        SleepStore.reset()
        SleepService.alive = false
        SleepService.denyForeground = false
    }

    private fun store() = SleepStore.get(app)

    private fun startService(intent: Intent? = null): SleepService {
        val c = Robolectric.buildService(SleepService::class.java, intent).create().startCommand(0, 1)
        controller = c
        idle()
        return c.get()
    }

    private fun runFiveMinutes(): SleepService {
        store().choose(5)
        store().start()
        return startService()
    }

    @Test fun fadesStopsAndRestoresVolume() {
        val service = runFiveMinutes()
        idle(3 * MINUTE + 59_000)
        assertEquals("vor dem Ausblenden unveraendert", 10, volume)
        idle(31_000)
        assertTrue("halb ausgeblendet", volume in 3..7)
        assertEquals(10, prefs.memo!!.orig)
        idle(30_000)
        // Ende: Fokus geholt, nach dem Pruefen zurueck auf 10
        assertNotNull(shadowOf(am).lastAudioFocusRequest)
        assertEquals(Mode.DONE, store().state.value.mode)
        idle(SleepService.KEY_AFTER_MS + 100)
        assertEquals(10, volume)
        assertNull(prefs.memo)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test fun userChangeDuringFadeIsKept() {
        runFiveMinutes()
        idle(4 * MINUTE + 30_000)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 7, 0)
        idle(40_000)
        assertEquals("nicht weiter ausgeblendet", 7, volume)
        idle(SleepService.KEY_AFTER_MS + 100)
        assertEquals("nicht ueberschrieben", 7, volume)
        assertNull(prefs.memo)
    }

    @Test fun extendDuringFadeRestoresAtOnce() {
        runFiveMinutes()
        idle(4 * MINUTE + 50_000)
        assertTrue(volume < 10)
        store().extend()
        idle()
        assertEquals(10, volume)
        assertNull(prefs.memo)
        assertEquals(Mode.RUNNING, store().state.value.mode)
    }

    @Test fun stopDuringFadeRestoresAtOnce() {
        val service = runFiveMinutes()
        idle(4 * MINUTE + 50_000)
        store().stop()
        idle()
        assertEquals(10, volume)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    // Player ignoriert Fokus und Taste: lieber still lassen, beim naechsten Oeffnen zurueck
    @Test fun stillPlayingStaysMutedUntilAppOpens() {
        runFiveMinutes()
        shadowOf(am).setIsMusicActive(true)
        idle(5 * MINUTE + SleepService.GIVE_UP_MS + 2_000)
        assertEquals(0, volume)
        assertNotNull(prefs.memo)
        val nm = app.getSystemService(NotificationManager::class.java)
        assertNotNull(shadowOf(nm).getNotification(SleepService.ID_MUTED))
        controller!!.destroy()
        controller = null
        SleepService.restorePending(app)
        assertEquals(10, volume)
        assertNull(prefs.memo)
        assertNull(shadowOf(nm).getNotification(SleepService.ID_MUTED))
    }

    // Prozess-Tod mitten im Ausblenden: neuer Prozess macht mit dem gesicherten Merker weiter
    @Test fun processDeathDuringFade() {
        runFiveMinutes()
        idle(4 * MINUTE + 30_000)
        val faded = volume
        assertTrue(faded < 10)
        controller!!.destroy()
        controller = null
        SleepStore.reset()
        startService()
        assertEquals(Mode.RUNNING, store().state.value.mode)
        idle(40_000)
        idle(SleepService.KEY_AFTER_MS + 100)
        assertEquals("Ausgangsstufe aus dem Merker", 10, volume)
    }

    // Ende lange verpasst: nicht mehr pausieren, nur zurueckstellen
    @Test fun overdueAfterLongDeathOnlyRestores() {
        val now = SystemClock.elapsedRealtime()
        prefs.sleep = Sleep(30, Mode.RUNNING, now - 40 * MINUTE, now - 10 * MINUTE)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 2, 0)
        prefs.memo = FadeMemo(orig = 10, last = 2)
        startService()
        idle()
        assertNull(shadowOf(am).lastAudioFocusRequest)
        assertEquals(Mode.DONE, store().state.value.mode)
        assertEquals(10, volume)
    }

    @Test fun wakeLockOnlyWhileRunning() {
        runFiveMinutes()
        val wake = ShadowPowerManager.getLatestWakeLock()
        assertTrue(wake.isHeld)
        store().stop()
        idle()
        assertFalse(wake.isHeld)
    }

    // Langer Lauf: keine Wecksperre bis kurz vor dem Ausblenden, stattdessen ungenauer Wecker
    @Test fun wakeLockOnlyNearFade() {
        store().choose(60)
        store().start()
        val fadeAt = store().state.value.fadeStart(FadeLength.M1.ms)
        startService()
        assertFalse(ShadowPowerManager.getLatestWakeLock()?.isHeld ?: false)
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
        val alarm = alarms.peekNextScheduledAlarm()!!
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarm.type)
        assertEquals(fadeAt - SleepService.WAKE_LEAD_MS, alarm.triggerAtMs)
        idle(49 * MINUTE + 1_000)
        assertTrue(ShadowPowerManager.getLatestWakeLock().isHeld)
        assertNull("Wecker erledigt", alarms.peekNextScheduledAlarm())
    }

    // Schuettel-Sensor erst 5 min vor dem Ausblenden
    @Test fun shakeSensorOnlyNearFade() {
        val sm = app.getSystemService(SensorManager::class.java)
        shadowOf(sm).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        store().choose(30)
        store().start()
        val service = startService()
        assertFalse(shadowOf(sm).hasListener(service))
        idle(24 * MINUTE + 1_000)
        assertTrue(shadowOf(sm).hasListener(service))
    }

    // System verweigert den Vordergrund (Neustart aus dem Hintergrund): Hinweis statt stiller Timer
    @Test fun refusedForegroundShowsHint() {
        store().choose(30)
        store().start()
        SleepService.denyForeground = true
        val service = startService()
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertFalse(SleepService.alive)
        assertEquals(Mode.RUNNING, store().state.value.mode)
        val nm = app.getSystemService(NotificationManager::class.java)
        assertNotNull(shadowOf(nm).getNotification(SleepService.ID_RESUME))
    }

    @Test fun refusedForegroundWithoutNotificationsStops() {
        shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        store().choose(30)
        store().start()
        SleepService.denyForeground = true
        startService()
        assertEquals(Mode.IDLE, store().state.value.mode)
    }

    // Verweigert nach dem Ende: abschliessen und Lautstaerke zurueck, kein Hinweis
    @Test fun refusedForegroundAfterEndRestores() {
        val now = SystemClock.elapsedRealtime()
        prefs.sleep = Sleep(30, Mode.RUNNING, now - 30 * MINUTE, now - 1_000)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 2, 0)
        prefs.memo = FadeMemo(orig = 10, last = 2)
        SleepService.denyForeground = true
        startService()
        assertEquals(Mode.DONE, store().state.value.mode)
        assertEquals(10, volume)
        assertNull(prefs.memo)
        val nm = app.getSystemService(NotificationManager::class.java)
        assertNull(shadowOf(nm).getNotification(SleepService.ID_RESUME))
    }

    // Ausgeblendet am Bluetooth-Geraet, jetzt Lautsprecher: nichts anfassen, Merker bleibt
    @Test fun restorePendingKeepsMemoForOtherOutput() {
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 2, 0)
        prefs.memo = FadeMemo(orig = 10, last = 2, device = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        SleepService.restorePending(app)
        assertEquals(2, volume)
        assertNotNull(prefs.memo)
    }

    // Sensor nur beim Lauf; drei Stoesse verlaengern um 10 min
    @Test fun shakeExtendsOnlyWhileRunning() {
        val sm = app.getSystemService(SensorManager::class.java)
        val acc = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        shadowOf(sm).addSensor(acc)
        val service = runFiveMinutes()
        assertTrue(shadowOf(sm).hasListener(service))
        val before = store().state.value.endAt
        val t = SystemClock.elapsedRealtimeNanos()
        for (i in 0 until 3) {
            service.onSensorChanged(
                SensorEventBuilder.newBuilder().setSensor(acc).setTimestamp(t + i * 300_000_000L)
                    .setValues(floatArrayOf(0f, 30f, 0f)).build(),
            )
        }
        idle()
        assertEquals(before + 10 * MINUTE, store().state.value.endAt)
        store().stop()
        idle()
        assertFalse(shadowOf(sm).hasListener(service))
    }

    @Test fun shakeCanBeTurnedOff() {
        prefs.shake = false
        val sm = app.getSystemService(SensorManager::class.java)
        shadowOf(sm).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        val service = runFiveMinutes()
        assertFalse(shadowOf(sm).hasListener(service))
    }

    // Aktionen aus der Benachrichtigung
    @Test fun notificationActions() {
        runFiveMinutes()
        val end = store().state.value.endAt
        controller!!.withIntent(Intent(app, SleepService::class.java).setAction(SleepService.ACTION_EXTEND)).startCommand(0, 2)
        idle()
        assertEquals(end + 10 * MINUTE, store().state.value.endAt)
        controller!!.withIntent(Intent(app, SleepService::class.java).setAction(SleepService.ACTION_STOP)).startCommand(0, 3)
        idle()
        assertEquals(Mode.IDLE, store().state.value.mode)
    }

    // Gespeicherter Lauf uebersteht den Prozess-Tod
    @Test fun runningStateSurvivesProcessDeath() {
        store().choose(45)
        store().start()
        val end = store().state.value.endAt
        SleepStore.reset()
        val s = store().state.value
        assertEquals(Mode.RUNNING, s.mode)
        assertEquals(end, s.endAt)
        assertEquals(45, s.setMin)
    }
}
