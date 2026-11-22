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
import solutions.xapient.roundr.domain.CueTracker
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
    private var cues: CueTracker? = null
    private var phase = -1
    private var shown: Any? = null
    private var foreground = false
    // Fertig-Signal ausklingen lassen, dann erst beenden
    private var lingering = false
    private var collecting = false
    private val tick = Runnable { onTick() }
    private val stop = Runnable { finish() }

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Pflicht nach startForegroundService: sofort in den Vordergrund; nicht erlaubt (Neustart aus dem Hintergrund) -> aufhoeren
        if (!goForeground(store.state.value)) return START_NOT_STICKY
        if (!collecting) {
            collecting = true
            scope.launch { store.state.collect { sync(it) } }
        }
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
        handler.removeCallbacks(stop)
        scope.cancel()
        releaseWake()
        signals.release()
        super.onDestroy()
    }

    private fun sync(r: Run?) {
        handler.removeCallbacks(tick)
        if (r == null) {
            // Schliessen waehrend des Fertig-Signals: ausklingen lassen
            if (!lingering) finish()
            return
        }
        val now = store.now()
        val e = r.session.elapsed(now)
        if (r.id != trackedId) {
            // neuer Lauf (auch „Nochmal" waehrend das Fertig-Signal ausklingt)
            trackedId = r.id
            cues = CueTracker(Cues.points(r.timeline), e)
            phase = r.timeline.at(e)?.index ?: -1
            handler.removeCallbacks(stop)
            lingering = false
        }
        if (lingering) return
        val c = cues ?: return
        c.onSync(e, r.session.running, r.session.offsetMs)
        when {
            // ans Ende gesprungen: Fertig-Signal noch spielen
            r.finished(now) -> if (c.last < r.timeline.totalMs) onTick() else finish()
            r.session.running -> {
                acquireWake(r, e)
                onTick()
            }
            else -> {
                releaseWake()
                goForeground(r)
            }
        }
    }

    private fun onTick() {
        val r = store.state.value ?: return
        val c = cues ?: return
        val now = store.now()
        val e = r.session.elapsed(now)
        c.onTick(e)?.let { signals.play(it, prefs.sound, prefs.vibrate) }
        if (e >= r.timeline.totalMs) {
            // Wakelock bleibt, bis das Signal ausgeklungen ist
            lingering = true
            handler.postDelayed(stop, DONE_LINGER_MS)
            return
        }
        val index = r.timeline.at(e)?.index ?: -1
        if (index != phase) {
            phase = index
            // Fortschritt sichern; nach Geraete-Neustart geht hoechstens der laufende Abschnitt verloren
            if (r.session.running) store.checkpoint()
        }
        goForeground(r)
        if (r.session.running) {
            val next = c.nextAt(e) ?: r.timeline.totalMs
            handler.postDelayed(tick, (next - e).coerceIn(1, 30_000))
        }
    }

    private fun finish() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(stop)
        lingering = false
        releaseWake()
        if (foreground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foreground = false
        }
        shown = null
        stopSelf()
    }

    // false = Vordergrund nicht erlaubt; dann beendet sich der Dienst, die Oberflaeche startet ihn beim Oeffnen neu
    private fun goForeground(r: Run?): Boolean {
        val now = store.now()
        val pos = r?.timeline?.at(r.session.elapsed(now))
        // nur bei Abschnitts- oder Sitzungswechsel (Anhalten, Weiter, Springen) neu melden
        val key = pos?.index to r?.session
        if (foreground && key == shown) return true
        val n = notification(r)
        if (!foreground) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(ID, n)
                }
            } catch (_: RuntimeException) {
                finish()
                return false
            }
            foreground = true
        } else {
            getSystemService(NotificationManager::class.java).notify(ID, n)
        }
        shown = key
        return true
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
                // Chronometer schneidet ab; +999 ms rundet auf wie die App
                .setWhen(System.currentTimeMillis() + pos.remainingMs + 999)
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
        // laenger als das Fertig-Signal (0,8 s Ton, 1 s Vibration)
        const val DONE_LINGER_MS = 1_500L
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
            try {
                ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
            } catch (_: RuntimeException) {
                // aus dem Hintergrund nicht erlaubt; beim naechsten Oeffnen erneut
            }
        }
    }
}
