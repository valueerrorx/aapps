package solutions.xapient.focusr

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Looper
import android.os.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBroadcastPendingResult
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.ShadowMediaPlayer.MediaInfo
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import solutions.xapient.focusr.domain.Timer
import java.time.Duration

// Glocke am Ende: Abschluss genau einmal (fertig, Fehler, Zeitlimit), Player frei, Audio-Fokus zurueck
@RunWith(RobolectricTestRunner::class)
class AlertTest {
    private val ctx: Application get() = RuntimeEnvironment.getApplication()
    private val am get() = ctx.getSystemService(AudioManager::class.java)
    private val players = mutableListOf<MediaPlayer>()

    private fun sound(durationMs: Int) {
        ShadowMediaPlayer.setMediaInfoProvider { MediaInfo(durationMs, 0) }
        ShadowMediaPlayer.setCreateListener { p, _ -> players += p }
    }

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun released() = shadowOf(players.single()).state == ShadowMediaPlayer.State.END

    @Before fun setUp() {
        players.clear()
    }

    @Test fun finishesOnceAfterChime() {
        sound(3000)
        var n = 0
        Alert.chime(ctx) { n++ }
        assertEquals(0, n)
        // laeuft: andere Wiedergabe wird nur leiser
        val req = shadowOf(am).lastAudioFocusRequest
        assertNotNull(req)
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, req.durationHint)
        idle(3500)
        assertEquals(1, n)
        assertTrue(released())
        assertNotNull(shadowOf(am).lastAbandonedAudioFocusRequest)
        idle(Alert.SAFETY_MS * 2)
        assertEquals(1, n)
    }

    @Test fun finishesOnceOnError() {
        sound(3000)
        var n = 0
        Alert.chime(ctx) { n++ }
        shadowOf(players.single()).invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        idle(10)
        assertEquals(1, n)
        assertTrue(released())
        idle(Alert.SAFETY_MS * 2)
        assertEquals(1, n)
    }

    @Test fun finishesOnceAfterSafetyTimeout() {
        sound(60_000)
        var n = 0
        Alert.chime(ctx) { n++ }
        idle(Alert.SAFETY_MS - 100)
        assertEquals(0, n)
        idle(200)
        assertEquals(1, n)
        assertTrue(released())
        assertNotNull(shadowOf(am).lastAbandonedAudioFocusRequest)
        idle(120_000)
        assertEquals(1, n)
    }

    // Receiver mit Ton: haelt den Broadcast offen und gibt ihn genau einmal frei
    @Test fun receiverFinishesPendingResultOnce() {
        sound(3000)
        Prefs(ctx).apply {
            timer = Timer(Timer.MINUTE).start(SystemClock.elapsedRealtime() - 2 * Timer.MINUTE)
            sound = true
            vibrate = false
        }
        val receiver = EndReceiver()
        val pending = ReflectionHelpers.callStaticMethod<BroadcastReceiver.PendingResult>(
            ShadowBroadcastPendingResult::class.java, "create",
            ClassParameter.from(Int::class.javaPrimitiveType, 0),
            ClassParameter.from(String::class.java, null),
            ClassParameter.from(android.os.Bundle::class.java, null),
            ClassParameter.from(Boolean::class.javaPrimitiveType, false),
        )
        ReflectionHelpers.callInstanceMethod<Any>(
            receiver, "setPendingResult", ClassParameter.from(BroadcastReceiver.PendingResult::class.java, pending),
        )
        receiver.onReceive(ctx, Intent(EndAlarm.ACTION))
        val result = Shadow.extract<ShadowBroadcastPendingResult>(pending).future
        assertTrue(!result.isDone)
        idle(3500)
        assertTrue(result.isDone)
        assertTrue(released())
        // Zeitlimit danach laeuft ins Leere (Glocke selbst zaehlt die Aufrufe in den Tests oben)
        idle(Alert.SAFETY_MS * 2)
        assertTrue(result.isDone)
    }
}
