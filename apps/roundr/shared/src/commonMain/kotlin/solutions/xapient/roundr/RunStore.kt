package solutions.xapient.roundr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Session
import solutions.xapient.roundr.domain.Workout

// Ein Lauf pro Prozess, geteilt von Oberflaeche und Dienst; jede Aenderung sofort gespeichert
class RunStore(
    load: () -> Run?,
    private val save: (Run?) -> Unit,
    private val clock: () -> Long = ::runClock,
    private val boot: Int = -1,
) {
    private val _state = MutableStateFlow<Run?>(null)
    val state: StateFlow<Run?> = _state

    init {
        // wiederhergestellten Zustand gleich sichern, sonst wird er beim naechsten Laden neu (falsch) gedeutet
        val loaded = load()
        val r = loaded?.restored(clock(), boot)
        _state.value = r
        if (r != loaded) save(r)
    }

    fun now(): Long = clock()

    private fun set(r: Run?) {
        _state.value = r
        save(r)
    }

    private fun update(f: (Run, Long) -> Session) {
        val r = _state.value ?: return
        val now = clock()
        if (r.finished(now)) return
        set(r.copy(session = f(r, now)))
    }

    fun start(w: Workout) {
        val now = clock()
        set(Run(w, Session.start(now), now, boot))
    }

    fun pause() = update { r, now -> r.session.pause(now) }
    fun resume() = update { r, now -> r.session.resume(now) }
    fun toggle() = update { r, now -> if (r.session.running) r.session.pause(now) else r.session.resume(now) }
    fun skip() = update { r, now -> r.session.seek(now, r.timeline.skipTarget(r.session.elapsed(now))) }
    fun back() = update { r, now -> r.session.seek(now, r.timeline.backTarget(r.session.elapsed(now))) }
    fun clear() = set(null)

    // Fortschritt sichern (bei Phasenwechsel); nur wenn der Lauf laeuft
    fun checkpoint() {
        if (_state.value?.session?.running == true) update { r, now -> r.session.rebased(now) }
    }

    // Android: RunStore.get(context) im App-Modul, iOS: einmal im MainViewController
    companion object
}

/** Uhr des Laufs in ms: Android Laufzeituhr (elapsedRealtime), iOS Wanduhr (laeuft auch im Ruhezustand weiter) */
expect fun runClock(): Long
