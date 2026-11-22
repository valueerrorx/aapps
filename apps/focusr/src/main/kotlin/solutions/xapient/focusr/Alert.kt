package solutions.xapient.focusr

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Ende: sanfter Glockenton als Alarm-Audio und kurze Vibration, auch bei gesperrtem Bildschirm
object Alert {
    private val PATTERN = longArrayOf(0, 380, 200, 380, 200, 760)
    const val SAFETY_MS = 8000L

    private val audio: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // Andere Wiedergabe (Musik, Video) nur leiser stellen, solange die Glocke klingt
    private val focus: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audio)
        .build()

    // done wird genau einmal gerufen: nach dem Ton, bei Fehler oder spaetestens nach SAFETY_MS
    fun chime(ctx: Context, done: () -> Unit) {
        var finished = false
        val handler = Handler(Looper.getMainLooper())
        val am = ctx.getSystemService(AudioManager::class.java)
        var player: MediaPlayer? = null
        val finish = {
            if (!finished) {
                finished = true
                handler.removeCallbacksAndMessages(null)
                player?.release()
                am?.abandonAudioFocusRequest(focus)
                done()
            }
        }
        try {
            val session = am?.generateAudioSessionId() ?: AudioManager.AUDIO_SESSION_ID_GENERATE
            player = MediaPlayer.create(ctx, R.raw.chime, audio, session)
        } catch (_: RuntimeException) {
            player = null
        }
        val p = player ?: return finish()
        p.setOnCompletionListener { handler.post { finish() } }
        p.setOnErrorListener { _, _, _ ->
            handler.post { finish() }
            true
        }
        handler.postDelayed({ finish() }, SAFETY_MS)
        am?.requestAudioFocus(focus)
        p.start()
    }

    fun vibrate(ctx: Context) {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Vibrator::class.java)
        }
        if (v == null || !v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(PATTERN, -1)
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, audio)
        }
    }
}
