package solutions.xapient.roundr

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Session
import solutions.xapient.roundr.domain.Workout

// Ein Lauf pro Prozess, geteilt von Oberflaeche und Dienst; jede Aenderung sofort gespeichert
class RunStore(
    private val load: () -> Run?,
    private val save: (Run?) -> Unit,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _state = MutableStateFlow(load()?.let { it.copy(session = it.session.restored(clock())) })
    val state: StateFlow<Run?> = _state

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
        set(Run(w, Session.start(now), now))
    }

    fun pause() = update { r, now -> r.session.pause(now) }
    fun resume() = update { r, now -> r.session.resume(now) }
    fun toggle() = update { r, now -> if (r.session.running) r.session.pause(now) else r.session.resume(now) }
    fun skip() = update { r, now -> r.session.seek(now, r.timeline.skipTarget(r.session.elapsed(now))) }
    fun back() = update { r, now -> r.session.seek(now, r.timeline.backTarget(r.session.elapsed(now))) }
    fun clear() = set(null)

    companion object {
        @Volatile private var instance: RunStore? = null

        fun get(context: Context): RunStore = instance ?: synchronized(this) {
            instance ?: Prefs(context).let { p -> RunStore({ p.run }, { p.run = it }) }.also { instance = it }
        }
    }
}
