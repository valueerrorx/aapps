package solutions.xapient.pulsr

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.Tempo
import solutions.xapient.pulsr.ui.PulsrAccent

// Haelt das Metronom bei gesperrtem Bildschirm am Leben: Vordergrund (mediaPlayback), Wakelock, Stopp in der Benachrichtigung
class PulseService : Service() {
    private val scope = MainScope()
    private lateinit var store: PulseStore
    private var wake: PowerManager.WakeLock? = null
    private var foreground = false
    private var collecting = false
    private var shown: Settings? = null

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
        store = PulseStore.get(this)
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
            scope.launch {
                store.settings.combine(store.playing) { s, p -> s to p }.collect { (s, p) ->
                    if (p) goForeground(s) else finish()
                }
            }
        }
        // nach Prozess-Tod nicht von selbst weiterspielen
        return START_NOT_STICKY
    }

    override fun onDestroy() {
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

    private fun notification(s: Settings): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_pulsr)
        .setContentTitle(getString(R.string.notify_title, s.bpm, s.beats))
        .setContentText(Tempo.marking(s.bpm))
        .setColor(PulsrAccent.toArgb())
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
                this, 1, Intent(this, PulseService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .build()

    // CPU wach, damit der Audio-Thread den Puffer rechtzeitig fuellt; nur waehrend des Spielens
    private fun acquireWake() {
        val w = wake ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pulsr:play")
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
        // Obergrenze fuer eine Uebungssitzung
        private const val MAX_WAKE_MS = 6 * 60 * 60 * 1000L
        const val ACTION_STOP = "solutions.xapient.pulsr.STOP"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, PulseService::class.java))
            } catch (_: RuntimeException) {
                // aus dem Hintergrund nicht erlaubt; spielt weiter, solange die App sichtbar ist
            }
        }
    }
}
