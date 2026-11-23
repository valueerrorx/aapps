package solutions.xapient.dozr

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.xapient.dozr.domain.Sleep

// Ein Timer pro Prozess, geteilt von Oberflaeche, Dienst und Kachel; jede Aenderung sofort gespeichert
class SleepStore(
    private val prefs: Prefs,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val onChange: (Sleep) -> Unit = {},
) {
    private val _state: MutableStateFlow<Sleep>
    val state: StateFlow<Sleep> get() = _state

    init {
        // wiederhergestellten Zustand gleich sichern, sonst wird er beim naechsten Laden neu gedeutet
        val loaded = prefs.sleep
        val s = Sleep.restored(loaded, prefs.sameBoot, clock())
        _state = MutableStateFlow(s)
        if (s != loaded) prefs.sleep = s
    }

    fun now(): Long = clock()

    private fun set(s: Sleep) {
        if (s == _state.value) return
        prefs.sleep = s
        _state.value = s
        onChange(s)
    }

    fun start() = set(_state.value.start(clock()))
    fun stop() = set(_state.value.stop())
    fun extend() = set(_state.value.extend(clock()))
    fun finish() = set(_state.value.finish())
    fun choose(minutes: Int) = set(_state.value.set(minutes))

    companion object {
        @Volatile private var instance: SleepStore? = null

        fun get(context: Context): SleepStore = instance ?: synchronized(this) {
            val app = context.applicationContext
            instance ?: SleepStore(Prefs(app), onChange = { DozrTile.refresh(app) }).also { instance = it }
        }

        // nur fuer Tests: Prozess-Tod nachstellen
        internal fun reset() {
            instance = null
        }
    }
}
