package solutions.xapient.roundr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import solutions.xapient.roundr.domain.CuePoint
import solutions.xapient.roundr.domain.Cues
import solutions.xapient.roundr.domain.Phase
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.TimeFormat
import solutions.xapient.roundr.ui.phaseColor

// Haelt den Lauf bei gesperrtem Bildschirm am Leben: Ansagen, Wakelock, Benachrichtigung mit Steuerung
class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = MainScope()
    private lateinit var store: RunStore
    private lateinit var prefs: Prefs
    private lateinit var signals: Signals
    private var wake: PowerManager.WakeLock? = null
    private var trackedId = Long.MIN_VALUE
    private var points: List<CuePoint> = emptyList()
    // verstrichene Zeit beim letzten Tick; Ansagen kommen aus dem Fenster (last, jetzt]
    private var last = 0L
    private var shown: Any? = null
    private var foreground = false
    private val tick = Runnable { onTick() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = RunStore.get(this)
        prefs = Prefs(this)
        signals = Signals(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.notify_channel), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
        // Pflicht nach startForegroundService: sofort in den Vordergrund, auch wenn gleich Schluss ist
        goForeground(store.state.value)
        scope.launch { store.state.collect { sync(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> store.toggle()
            ACTION_SKIP -> store.skip()
            ACTION_STOP -> store.clear()
        }
        sync(store.state.value)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        scope.cancel()
        releaseWake()
        signals.release()
        super.onDestroy()
    }

    private fun sync(r: Run?) {
        handler.removeCallbacks(tick)
        val now = store.now()
        if (r == null || r.finished(now)) {
            finish()
            return
        }
        val e = r.session.elapsed(now)
        if (r.id != trackedId) {
            trackedId = r.id
            points = Cues.points(r.timeline)
            // frischer Start: Startsignal mitnehmen; nach Prozess-Neustart nichts nachholen
            last = if (e < 1000) -1 else e
        }
        // zurueckgesprungen: Startsignal des Ziel-Abschnitts spielen
        if (e < last) last = e - 1
        if (r.session.running) {
            acquireWake(r, e)
            onTick()
        } else {
            releaseWake()
            last = e
            goForeground(r)
        }
    }

    private fun onTick() {
        val r = store.state.value ?: return
        val now = store.now()
        val e = r.session.elapsed(now)
        Cues.due(points, last, e)?.let { signals.play(it, prefs.sound, prefs.vibrate) }
        last = e
        if (e >= r.timeline.totalMs) {
            finish()
            return
        }
        goForeground(r)
        if (r.session.running) {
            val next = Cues.nextAt(points, e) ?: r.timeline.totalMs
            handler.postDelayed(tick, (next - e).coerceIn(1, 30_000))
        }
    }

    private fun finish() {
        handler.removeCallbacks(tick)
        releaseWake()
        if (foreground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foreground = false
        }
        shown = null
        stopSelf()
    }

    private fun goForeground(r: Run?) {
        val now = store.now()
        val pos = r?.timeline?.at(r.session.elapsed(now))
        // nur bei Abschnitts- oder Zustandswechsel neu melden
        val key = pos?.index to r?.session?.running
        if (foreground && key == shown) return
        val n = notification(r)
        if (!foreground) {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(ID, n)
            }
            foreground = true
        } else {
            getSystemService(NotificationManager::class.java).notify(ID, n)
        }
        shown = key
    }

    private fun notification(r: Run?): Notification {
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_roundr)
            .setContentTitle(getString(R.string.app_name))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        val now = store.now()
        val pos = r?.timeline?.at(r.session.elapsed(now)) ?: return b.build()
        val s = pos.segment
        val round = if (r.timeline.sets > 1) {
            getString(R.string.round_set_of, s.round, r.timeline.rounds, s.set, r.timeline.sets)
        } else {
            getString(R.string.round_of, s.round, r.timeline.rounds)
        }
        b.setContentTitle("${getString(phaseRes(s.phase))} · $round")
            .setColor(phaseColor(s.phase).toArgb())
            .setColorized(true)
        if (r.session.running) {
            b.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + pos.remainingMs)
            r.timeline.next(pos.index)?.let {
                b.setContentText(getString(R.string.next_is, getString(phaseRes(it.phase)), TimeFormat.clock((it.durationMs / 1000).toInt())))
            }
        } else {
            b.setShowWhen(false).setContentText(getString(R.string.paused_at, TimeFormat.clock(TimeFormat.ceilSec(pos.remainingMs))))
        }
        b.addAction(
            0,
            getString(if (r.session.running) R.string.pause else R.string.resume),
            action(ACTION_TOGGLE, 1),
        )
        b.addAction(0, getString(R.string.skip), action(ACTION_SKIP, 2))
        b.addAction(0, getString(R.string.stop), action(ACTION_STOP, 3))
        return b.build()
    }

    private fun action(a: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code, Intent(this, TimerService::class.java).setAction(a),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // CPU wach halten, solange der Lauf laeuft; Zeitlimit = Restdauer plus Reserve
    private fun acquireWake(r: Run, e: Long) {
        val w = wake ?: getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "roundr:run")
            .apply { setReferenceCounted(false) }
            .also { wake = it }
        w.acquire((r.timeline.totalMs - e).coerceAtLeast(0) + 60_000)
    }

    private fun releaseWake() {
        wake?.let { if (it.isHeld) it.release() }
    }

    companion object {
        private const val CHANNEL = "run"
        private const val ID = 1
        const val ACTION_TOGGLE = "solutions.xapient.roundr.TOGGLE"
        const val ACTION_SKIP = "solutions.xapient.roundr.SKIP"
        const val ACTION_STOP = "solutions.xapient.roundr.STOP"

        fun phaseRes(p: Phase): Int = when (p) {
            Phase.PREP -> R.string.phase_prep
            Phase.WORK -> R.string.phase_work
            Phase.REST -> R.string.phase_rest
            Phase.SET_REST -> R.string.phase_set_rest
        }

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        }
    }
}
