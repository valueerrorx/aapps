package solutions.xapient.pulsr

import android.content.Context
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.xapient.pulsr.domain.Pulse
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.TapTempo

// Audio-Ausgabe; im Test ersetzt
interface Player {
    fun start(s: Settings): Boolean
    fun update(s: Settings)
    fun stop()
    // Puls am gerade hoerbaren Frame (System.nanoTime-Basis)
    fun pulse(nowNanos: Long): Pulse?
    // Ausgabe abgebrochen (z. B. Audio-Geraet weg), kommt vom Audio-Thread
    var onFailure: (() -> Unit)?
}

// Ein Metronom pro Prozess, geteilt von Oberflaeche und Dienst; Einstellungen sofort gespeichert
class PulseStore(
    load: () -> Settings,
    private val save: (Settings) -> Unit,
    private val player: Player,
    private val clockMs: () -> Long = SystemClock::uptimeMillis,
) {
    private val _settings = MutableStateFlow(load().sane())
    val settings: StateFlow<Settings> = _settings
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing
    private val tapper = TapTempo()

    init {
        player.onFailure = { _playing.value = false }
    }

    // false = nichts geaendert (z. B. am Rand des Bereichs)
    private fun update(f: (Settings) -> Settings): Boolean {
        val s = f(_settings.value).sane()
        if (s == _settings.value) return false
        _settings.value = s
        save(s)
        if (_playing.value) player.update(s)
        return true
    }

    fun setBpm(bpm: Int) { update { it.copy(bpm = bpm) } }
    fun nudge(by: Int): Boolean = update { it.copy(bpm = it.bpm + by) }
    fun setBeats(n: Int) { update { it.copy(beats = n) } }
    fun set(s: Settings) { update { s } }

    // true, sobald ein Tempo feststeht; atMs = Zeit des Aufsetzens (uptimeMillis), nicht der Auswertung
    fun tap(atMs: Long = clockMs()): Boolean {
        val bpm = tapper.tap(atMs) ?: return false
        setBpm(bpm)
        return true
    }

    fun start(): Boolean {
        if (_playing.value) return true
        if (!player.start(_settings.value)) return false
        _playing.value = true
        return true
    }

    fun stop() {
        if (!_playing.value) return
        player.stop()
        _playing.value = false
    }

    fun toggle() = if (_playing.value) stop() else start().let { }

    fun pulse(nowNanos: Long): Pulse? = if (_playing.value) player.pulse(nowNanos) else null

    companion object {
        @Volatile private var instance: PulseStore? = null

        fun get(context: Context): PulseStore = instance ?: synchronized(this) {
            instance ?: Prefs(context).let { p -> PulseStore({ p.settings }, { p.settings = it }, AudioEngine()) }.also { instance = it }
        }

        @VisibleForTesting
        fun install(store: PulseStore?) {
            instance = store
        }
    }
}
