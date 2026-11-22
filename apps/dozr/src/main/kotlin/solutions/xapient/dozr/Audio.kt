package solutions.xapient.dozr

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.view.KeyEvent
import solutions.xapient.dozr.domain.Volume

// Medienlautstaerke ohne Einblendung; Fehler (z. B. Bitte-nicht-stoeren-Regeln) werden geschluckt
class MusicVolume(private val am: AudioManager) : Volume {
    override val current: Int get() = am.getStreamVolume(AudioManager.STREAM_MUSIC)

    override fun set(index: Int) {
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        } catch (_: SecurityException) {
            // nicht erlaubt: Stufe bleibt, der Fader liest danach die echte Stufe
        }
    }
}

// Andere Wiedergabe beenden: Fokus holen (Player pausieren), Rueckfall Medientaste PAUSE
class Silencer(context: Context) {
    val am: AudioManager = context.getSystemService(AudioManager::class.java)
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setOnAudioFocusChangeListener { }
        .build()
    private var holding = false

    val playing: Boolean get() = am.isMusicActive

    val fixedVolume: Boolean get() = am.isVolumeFixed

    fun grabFocus() {
        holding = am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun releaseFocus() {
        if (holding) am.abandonAudioFocusRequest(request)
        holding = false
    }

    // PAUSE statt PLAY_PAUSE: startet nichts, wenn nichts laeuft
    fun pauseKey() {
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }
}
