package solutions.xapient.hertzr

import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import solutions.xapient.hertzr.domain.Settings

// Schutz am Store: greift ohne Dienst (Vordergrund-Start kann scheitern)
@RunWith(RobolectricTestRunner::class)
class AudioGuardTest {
    private val app = RuntimeEnvironment.getApplication()
    private val store = ToneStore({ Settings() }, {}, FakePlayer()).also { AudioGuard(app, it) }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun playing() {
        idle()
        store.start()
        idle()
    }

    @Test fun headphonesUnpluggedStops() {
        playing()
        app.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        idle()
        assertFalse(store.playing.value)
    }

    @Test fun callStops() {
        playing()
        app.getSystemService(AudioManager::class.java).mode = AudioManager.MODE_IN_CALL
        idle()
        assertFalse(store.playing.value)
    }

    // gestoppt horcht nichts mit; erneut gestartet wieder
    @Test fun armedOnlyWhilePlaying() {
        playing()
        store.stop()
        idle()
        app.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        idle()
        playing()
        assertTrue(store.playing.value)
        app.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        idle()
        assertFalse(store.playing.value)
    }

    // Einstellungen werden nach der Ruhe einmal geschrieben, nicht je Schritt
    @Test fun debouncedSave() {
        val written = mutableListOf<Int>()
        val d = Debounced(400L) { v: Int -> written += v }
        repeat(5) { d.post(it) }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        assertTrue(written.isEmpty())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertEquals(listOf(4), written)
    }
}
