package solutions.xapient.hertzr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import solutions.xapient.hertzr.domain.Mode
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.hzText
import solutions.xapient.hertzr.ui.waveName

// Haelt den Ton bei ausgeschaltetem Bildschirm am Leben: Vordergrund (mediaPlayback), Wakelock, Stopp in der Benachrichtigung
class ToneService : Service() {
    private val scope = MainScope()
    private lateinit var store: ToneStore
    private var wake: PowerManager.WakeLock? = null
    private var foreground = false
    private var collecting = false
    private var shown: Settings? = null
    // AudioManager.OnModeChangedListener ab API 31
    private var modeListener: Any? = null

    // Kopfhoerer ab: nicht ploetzlich laut aus dem Lautsprecher
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) store.stop()
        }
    }
    private var noisyRegistered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = ToneStore.get(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.notify_channel), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) store.stop()
        // Pflicht nach startForegroundService: sofort in den Vordergrund
        if (!goForeground(store.settings.value)) return START_NOT_STICKY
        if (!store.playing.value) {
            finish()
            return START_NOT_STICKY
        }
        acquireWake()
        if (!noisyRegistered) {
            ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            noisyRegistered = true
        }
        if (!collecting) {
            collecting = true
            // Stopp sofort, Aenderungen (Rad, Halten) entprellt
            scope.launch { store.playing.collect { if (!it) finish() } }
            scope.launch {
                store.settings.collectLatest { s ->
                    delay(NOTIFY_DEBOUNCE_MS)
                    if (store.playing.value) goForeground(s)
                }
            }
            watchCalls()
        }
        // nach Prozess-Tod nicht von selbst weiterspielen
        return START_NOT_STICKY
    }

    // Telefonat: Ton aus
    private fun watchCalls() {
        val audio = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= 31) {
            val l = AudioManager.OnModeChangedListener { if (inCall(it)) store.stop() }
            audio.addOnModeChangedListener(mainExecutor, l)
            modeListener = l
        } else {
            // ohne Listener: Modus jede Sekunde pruefen, nur der Wechsel in ein Gespraech stoppt
            scope.launch {
                var was = inCall(audio.mode)
                while (true) {
                    delay(1_000)
                    val now = inCall(audio.mode)
                    if (now && !was) store.stop()
                    was = now
                }
            }
        }
    }

    private fun inCall(mode: Int) = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 31) {
            (modeListener as? AudioManager.OnModeChangedListener)?.let { getSystemService(AudioManager::class.java).removeOnModeChangedListener(it) }
        }
        scope.cancel()
        releaseWake()
        if (noisyRegistered) {
            unregisterReceiver(noisy)
            noisyRegistered = false
        }
        super.onDestroy()
    }

    private fun finish() {
        releaseWake()
        if (foreground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foreground = false
        }
        shown = null
        stopSelf()
    }

    // false = Vordergrund nicht erlaubt; die Oberflaeche spielt dann nur, solange sie sichtbar ist
    private fun goForeground(s: Settings): Boolean {
        if (foreground && s == shown) return true
        val n = notification(s)
        if (!foreground) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(ID, n)
                }
            } catch (_: RuntimeException) {
                stopSelf()
                return false
            }
            foreground = true
        } else {
            getSystemService(NotificationManager::class.java).notify(ID, n)
        }
        shown = s
        return true
    }

    private fun notification(s: Settings): Notification {
        val dec = getString(R.string.decimal_sep).first()
        val title = if (s.mode == Mode.SWEEP) {
            getString(R.string.notify_sweep, hzText(s.sweepFrom, dec), hzText(s.sweepTo, dec), s.sweepSec)
        } else {
            hzText(s.hz, dec)
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_hertzr)
            .setContentTitle(title)
            .setContentText(getString(R.string.notify_text, getString(waveName(s.wave)), s.levelDb))
            .setColor(HertzrAccent.toArgb())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .addAction(
                0, getString(R.string.stop),
                PendingIntent.getService(
                    this, 1, Intent(this, ToneService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()
    }

    // CPU wach, damit der Audio-Thread den Puffer rechtzeitig fuellt; nur waehrend des Spielens
    private fun acquireWake() {
        val w = wake ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hertzr:play")
            .apply { setReferenceCounted(false) }
            .also { wake = it }
        if (!w.isHeld) w.acquire(MAX_WAKE_MS)
    }

    private fun releaseWake() {
        wake?.let { if (it.isHeld) it.release() }
    }

    companion object {
        private const val CHANNEL = "play"
        private const val ID = 1
        // Obergrenze fuer eine Messsitzung
        private const val MAX_WAKE_MS = 4 * 60 * 60 * 1000L
        private const val NOTIFY_DEBOUNCE_MS = 280L
        const val ACTION_STOP = "solutions.xapient.hertzr.STOP"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, ToneService::class.java))
            } catch (_: RuntimeException) {
                // aus dem Hintergrund nicht erlaubt; spielt weiter, solange die App sichtbar ist
            }
        }
    }
}
