package solutions.xapient.dozr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import solutions.xapient.dozr.domain.Fader
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.domain.Phase
import solutions.xapient.dozr.domain.ShakeDetector
import solutions.xapient.dozr.domain.Sleep
import solutions.xapient.dozr.ui.DozrAccent
import androidx.compose.ui.graphics.toArgb
import java.util.Date
import kotlin.math.min

// Laeuft, solange der Timer laeuft: blendet aus, stoppt fremde Wiedergabe, stellt die Lautstaerke zurueck
class SleepService : Service(), SensorEventListener {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = MainScope()
    private lateinit var store: SleepStore
    private lateinit var prefs: Prefs
    private lateinit var silencer: Silencer
    private lateinit var fader: Fader
    private var wake: PowerManager.WakeLock? = null
    private var sensors: SensorManager? = null
    private var shake: ShakeDetector? = null
    private var foreground = false
    private var collecting = false
    private var shown: Any? = null
    // Ende laeuft: Fokus gehalten, warten bis die Player pausiert haben
    private var ending = false
    private var endStarted = 0L
    private var keyed = false
    private val tick = Runnable { onTick() }
    private val endCheck = Runnable { onEndCheck() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        alive = true
        store = SleepStore.get(this)
        prefs = Prefs(this)
        silencer = Silencer(this)
        fader = Fader(MusicVolume(silencer.am), prefs.memo)
        channels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Pflicht nach startForegroundService: sofort in den Vordergrund; nicht erlaubt -> aufhoeren, die App startet neu
        if (!goForeground(store.state.value)) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_EXTEND -> store.extend()
            ACTION_STOP -> store.stop()
        }
        if (!collecting) {
            collecting = true
            scope.launch { store.state.collect { sync(it) } }
        }
        sync(store.state.value)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        sensorsOff()
        releaseWake()
        if (ending) {
            silencer.releaseFocus()
            ending = false
        }
        alive = false
        super.onDestroy()
    }

    private fun sync(s: Sleep) {
        handler.removeCallbacks(tick)
        when (s.mode) {
            Mode.RUNNING -> {
                if (ending) cancelEnd()
                // Prozess war zu lange tot: nicht mehr pausieren
                if (s.overdue(store.now())) {
                    store.finish()
                    return
                }
                acquireWake(s)
                if (prefs.shake) sensorsOn() else sensorsOff()
                onTick()
            }
            // vom Nutzer gestoppt: Lautstaerke sofort zurueck
            Mode.IDLE -> if (!ending) {
                restore()
                stopAll()
            }
            // zu Ende, aber nicht von diesem Dienst: nur zurueckstellen, wenn nichts mehr spielt
            Mode.DONE -> if (!ending) {
                if (!silencer.playing) restore()
                stopAll()
            }
        }
    }

    private fun onTick() {
        val s = store.state.value
        if (s.mode != Mode.RUNNING || ending) return
        val now = store.now()
        val fadeMs = prefs.fade.ms
        when (s.phase(now, fadeMs)) {
            Phase.RUNNING -> {
                // verlaengert waehrend des Ausblendens
                if (fader.faded) restore()
                goForeground(s)
                handler.postDelayed(tick, (s.fadeStart(fadeMs) - now).coerceIn(1L, IDLE_TICK_MS))
            }
            Phase.FADING -> {
                if (!silencer.fixedVolume && fader.step(s.fadeProgress(now, fadeMs))) prefs.memo = fader.memo
                goForeground(s)
                handler.postDelayed(tick, min(FADE_TICK_MS, (s.endAt - now).coerceAtLeast(1L)))
            }
            Phase.ENDED -> beginEnd()
            else -> Unit
        }
    }

    // Stille erreicht: Fokus holen, Player pausieren lassen
    private fun beginEnd() {
        ending = true
        if (!silencer.fixedVolume && fader.step(1.0)) prefs.memo = fader.memo
        sensorsOff()
        store.finish()
        silencer.grabFocus()
        endStarted = store.now()
        keyed = false
        handler.postDelayed(endCheck, KEY_AFTER_MS)
    }

    private fun onEndCheck() {
        if (silencer.playing) {
            // Rueckfall fuer Player ohne Fokus-Beachtung
            if (!keyed) {
                silencer.pauseKey()
                keyed = true
            }
            if (store.now() - endStarted < GIVE_UP_MS) {
                handler.postDelayed(endCheck, POLL_MS)
                return
            }
            // spielt weiter: lieber still lassen als laut; beim naechsten Oeffnen zurueck
            silencer.releaseFocus()
            if (fader.faded) notifyMuted()
        } else {
            silencer.releaseFocus()
            restore()
        }
        ending = false
        stopAll()
    }

    private fun cancelEnd() {
        handler.removeCallbacks(endCheck)
        silencer.releaseFocus()
        restore()
        ending = false
    }

    private fun restore() {
        if (fader.restore()) prefs.memo = fader.memo
    }

    private fun stopAll() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(endCheck)
        sensorsOff()
        releaseWake()
        if (foreground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foreground = false
        }
        shown = null
        stopSelf()
    }

    // false = Vordergrund nicht erlaubt
    private fun goForeground(s: Sleep): Boolean {
        val fading = s.mode == Mode.RUNNING && s.phase(store.now(), prefs.fade.ms) == Phase.FADING
        val key = s.endAt to fading
        if (foreground && key == shown) return true
        val n = notification(s, fading)
        if (!foreground) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(ID_RUN, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(ID_RUN, n)
                }
            } catch (_: RuntimeException) {
                return false
            }
            foreground = true
        } else {
            getSystemService(NotificationManager::class.java).notify(ID_RUN, n)
        }
        shown = key
        return true
    }

    private fun wall(elapsedAt: Long): String =
        DateFormat.getTimeFormat(this).format(Date(System.currentTimeMillis() + (elapsedAt - store.now())))

    private fun notification(s: Sleep, fading: Boolean): Notification {
        val b = NotificationCompat.Builder(this, CHANNEL_RUN)
            .setSmallIcon(R.drawable.ic_stat_dozr)
            .setColor(DozrAccent.toArgb())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(this))
        if (s.mode != Mode.RUNNING) return b.setContentTitle(getString(R.string.app_name)).build()
        val now = store.now()
        b.setContentTitle(getString(R.string.notif_stops_at, wall(s.endAt)))
            .setContentText(
                if (fading) getString(R.string.notif_fading) else getString(R.string.notif_fade_from, wall(s.fadeStart(prefs.fade.ms))),
            )
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            // Chronometer schneidet ab; +999 ms rundet auf wie die App
            .setWhen(System.currentTimeMillis() + s.remaining(now) + 999)
            .addAction(0, getString(R.string.extend), action(ACTION_EXTEND, 1))
            .addAction(0, getString(R.string.stop), action(ACTION_STOP, 2))
        return b.build()
    }

    private fun notifyMuted() {
        val n = NotificationCompat.Builder(this, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_stat_dozr)
            .setColor(DozrAccent.toArgb())
            .setContentTitle(getString(R.string.notif_muted_title))
            .setContentText(getString(R.string.notif_muted_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.notif_muted_text)))
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openApp(this))
            .build()
        try {
            getSystemService(NotificationManager::class.java).notify(ID_MUTED, n)
        } catch (_: SecurityException) {
            // ohne Benachrichtigungsrecht: App stellt beim Oeffnen trotzdem zurueck
        }
    }

    private fun action(a: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code, Intent(this, SleepService::class.java).setAction(a),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // CPU wach halten: sonst bleiben Handler-Zeiten bei Offload-Wiedergabe stehen
    private fun acquireWake(s: Sleep) {
        val w = wake ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dozr:timer")
            .apply { setReferenceCounted(false) }
            .also { wake = it }
        w.acquire(s.remaining(store.now()) + WAKE_SLACK_MS)
    }

    private fun releaseWake() {
        wake?.let { if (it.isHeld) it.release() }
    }

    // Beschleunigungssensor nur, solange der Timer laeuft
    private fun sensorsOn() {
        if (sensors != null) return
        val sm = getSystemService(SensorManager::class.java) ?: return
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        shake = ShakeDetector()
        sensors = sm
        sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME, handler)
    }

    private fun sensorsOff() {
        sensors?.unregisterListener(this)
        sensors = null
        shake = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val d = shake ?: return
        val v = event.values
        if (d.onSample(v[0], v[1], v[2], event.timestamp / 1_000_000L)) {
            store.extend()
            buzz()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // Bestaetigung ohne Bildschirm: zwei kurze Pulse
    private fun buzz() {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        if (v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 35, 90, 35), -1))
    }

    companion object {
        const val CHANNEL_RUN = "run"
        const val CHANNEL_INFO = "info"
        const val ID_RUN = 1
        const val ID_MUTED = 2
        const val IDLE_TICK_MS = 30_000L
        const val FADE_TICK_MS = 250L
        const val KEY_AFTER_MS = 1_000L
        const val POLL_MS = 500L
        const val GIVE_UP_MS = 10_000L
        const val WAKE_SLACK_MS = 60_000L
        const val ACTION_EXTEND = "solutions.xapient.dozr.EXTEND"
        const val ACTION_STOP = "solutions.xapient.dozr.STOP"

        // Prozess-lokal: Dienst laeuft gerade (dann gehoert ihm der Lautstaerke-Merker)
        @Volatile var alive = false
            internal set

        fun channels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_RUN, context.getString(R.string.channel_run), NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) },
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_INFO, context.getString(R.string.channel_info), NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) },
            )
        }

        fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // nur aus dem Vordergrund aufrufen (App, Kachel-Tipp); false = System lehnt ab
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, SleepService::class.java))
            true
        } catch (_: IllegalStateException) {
            false
        } catch (_: SecurityException) {
            false
        }

        // Lautstaerke nach einem Ende ohne Dienst (Prozess-Tod, Player spielte weiter) zurueck
        fun restorePending(context: Context) {
            // laufender Timer: der Dienst macht mit dem Merker weiter
            if (alive || SleepStore.get(context).state.value.mode == Mode.RUNNING) return
            val prefs = Prefs(context)
            val memo = prefs.memo ?: return
            val f = Fader(MusicVolume(context.getSystemService(android.media.AudioManager::class.java)), memo)
            f.restore()
            prefs.memo = null
            context.getSystemService(NotificationManager::class.java).cancel(ID_MUTED)
        }
    }
}
