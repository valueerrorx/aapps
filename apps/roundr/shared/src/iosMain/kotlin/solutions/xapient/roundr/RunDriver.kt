package solutions.xapient.roundr

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import solutions.xapient.roundr.domain.Cue
import solutions.xapient.roundr.domain.CueTracker
import solutions.xapient.roundr.domain.Cues
import solutions.xapient.roundr.domain.Phase
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.res.Res
import solutions.xapient.roundr.res.done
import solutions.xapient.roundr.res.phase_prep
import solutions.xapient.roundr.res.phase_rest
import solutions.xapient.roundr.res.phase_set_rest
import solutions.xapient.roundr.res.phase_work
import solutions.xapient.roundr.res.round_of
import solutions.xapient.roundr.res.round_set_of

/**
 * Ansagen auf iOS: im Vordergrund spielt roundr alle Signale selbst (wie der Android-Dienst);
 * fuer den Hintergrund ist jeder Abschnittswechsel als lokale Mitteilung mit Ton vorgemerkt
 * (iOS zeigt sie nicht, solange die App vorne ist).
 */
class RunDriver(private val store: RunStore, private val prefs: IosPrefs) {
    private val scope = MainScope()
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val signals = IosSignals()
    private var trackedId = Long.MIN_VALUE
    private var cues: CueTracker? = null
    private var tick: Job? = null
    private var plan: Job? = null

    /** Vom Oberflaechen-Lebenszyklus gesetzt */
    var active = false
        set(v) {
            field = v
            // zurueck im Vordergrund: Verpasstes nicht nachspielen, das haben die Mitteilungen erledigt
            if (v) {
                center.removeAllDeliveredNotifications()
                resync()
            }
        }

    init {
        scope.launch { store.state.collect { sync(it) } }
    }

    private fun sync(r: Run?) {
        tick?.cancel()
        schedule(r)
        if (r == null) {
            trackedId = Long.MIN_VALUE
            cues = null
            return
        }
        val now = store.now()
        val e = r.session.elapsed(now)
        if (r.id != trackedId) {
            trackedId = r.id
            cues = CueTracker(Cues.points(r.timeline), e)
        }
        val c = cues ?: return
        c.onSync(e, r.session.running, r.session.offsetMs)
        // ans Ende gesprungen: Fertig-Signal noch spielen
        if (r.session.running || (r.finished(now) && c.last < r.timeline.totalMs)) onTick()
    }

    private fun resync() {
        val r = store.state.value ?: return
        val e = r.session.elapsed(store.now())
        cues = CueTracker(Cues.points(r.timeline), e).also { it.onTick(e) }
        tick?.cancel()
        if (r.session.running) onTick()
    }

    private fun onTick() {
        val r = store.state.value ?: return
        val c = cues ?: return
        val e = r.session.elapsed(store.now())
        c.onTick(e)?.let { if (active) signals.play(it, prefs.sound, prefs.vibrate) }
        if (e >= r.timeline.totalMs || !r.session.running) return
        val next = c.nextAt(e) ?: r.timeline.totalMs
        tick = scope.launch {
            delay((next - e).coerceIn(1, 30_000))
            onTick()
        }
    }

    // Abschnittswechsel als Mitteilungen; iOS erlaubt hoechstens 64 vorgemerkte je App
    private fun schedule(r: Run?) {
        plan?.cancel()
        center.removeAllPendingNotificationRequests()
        if (r == null || !r.session.running) return
        val sound = prefs.sound
        plan = scope.launch {
            val e = r.session.elapsed(store.now())
            val t = r.timeline
            Cues.points(t).filter { it.cue !is Cue.Count && it.atMs > e }.take(MAX).forEachIndexed { i, p ->
                val content = UNMutableNotificationContent()
                when (val cue = p.cue) {
                    is Cue.Start -> {
                        val s = t.at(p.atMs)?.segment ?: return@forEachIndexed
                        content.setTitle(getString(label(cue.phase)))
                        content.setBody(
                            if (t.sets > 1) getString(Res.string.round_set_of, s.round, t.rounds, s.set, t.sets)
                            else getString(Res.string.round_of, s.round, t.rounds),
                        )
                    }
                    else -> content.setTitle(getString(Res.string.done))
                }
                if (sound) content.setSound(UNNotificationSound.soundNamed(IosSignals.fileName(IosSignals.key(p.cue))))
                // Abstand ab jetzt; Wartezeit fuer die Texte abziehen
                val left = (p.atMs - r.session.elapsed(store.now())) / 1000.0
                if (left <= 0.0) return@forEachIndexed
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(left, false)
                center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier("roundr.$i", content, trigger), null)
            }
        }
    }

    private fun label(p: Phase) = when (p) {
        Phase.PREP -> Res.string.phase_prep
        Phase.WORK -> Res.string.phase_work
        Phase.REST -> Res.string.phase_rest
        Phase.SET_REST -> Res.string.phase_set_rest
    }

    private companion object {
        const val MAX = 60
    }
}
