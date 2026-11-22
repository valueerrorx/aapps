package solutions.xapient.hertzr

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Level
import solutions.xapient.hertzr.domain.Mode
import solutions.xapient.hertzr.domain.Output
import solutions.xapient.hertzr.domain.Params
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.domain.Sweep
import solutions.xapient.hertzr.domain.Wave
import solutions.xapient.hertzr.domain.params

// Audio-Ausgabe; im Test ersetzt
interface Player {
    fun start(p: Params): Boolean
    fun update(p: Params)
    fun stop()
    // laufende Frequenz (Sweep), null wenn nichts spielt
    fun liveHz(): Double?
    fun progress(): Float?
    // Sweep fertig oder Ausgabe weg; kommt vom Audio-Thread
    var onEnd: (() -> Unit)?
}

// Ein Generator pro Prozess, geteilt von Oberflaeche und Dienst; Einstellungen sofort gespeichert
class ToneStore(
    load: () -> Settings,
    private val save: (Settings) -> Unit,
    private val player: Player,
    val maxHz: Double = Freq.MAX,
) {
    // nach Neustart nie lauter als Level.RESTORE_MAX_DB
    private val _settings = MutableStateFlow(load().let { it.copy(levelDb = Level.restored(it.levelDb)) }.sane(maxHz))
    val settings: StateFlow<Settings> = _settings
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing
    // Kopfhoerer am Ausgang (AudioGuard)
    private val _headphones = MutableStateFlow(false)
    val headphones: StateFlow<Boolean> = _headphones

    init {
        player.onEnd = { _playing.value = false }
    }

    // false = nichts geaendert (z. B. am Rand des Bereichs)
    private fun update(f: (Settings) -> Settings): Boolean {
        val s = f(_settings.value).sane(maxHz)
        if (s == _settings.value) return false
        _settings.value = s
        save(s)
        if (_playing.value) player.update(s.params())
        return true
    }

    fun setHz(hz: Double): Boolean = update { it.copy(hz = hz) }
    fun nudge(dir: Int): Boolean = update { it.copy(hz = Freq.nudge(it.hz, dir, maxHz)) }
    fun octave(up: Boolean): Boolean = update { it.copy(hz = Freq.octave(it.hz, up, maxHz)) }
    fun setWave(w: Wave) { update { it.copy(wave = w) } }
    fun setLevel(db: Int): Boolean = update { it.copy(levelDb = db) }
    fun setSweepFrom(hz: Double) { update { it.copy(sweepFrom = hz) } }
    fun setSweepTo(hz: Double) { update { it.copy(sweepTo = hz) } }
    fun nextDuration() { update { it.copy(sweepSec = Sweep.nextDuration(it.sweepSec)) } }
    fun setOutput(o: Output) { update { it.copy(output = o) } }
    fun setKeepOn(on: Boolean) { update { it.copy(keepOn = on) } }

    // Kopfhoerer neu dran waehrend des Spielens: Pegel gleitet auf die Kopfhoerer-Grenze
    fun setHeadphones(on: Boolean) {
        val added = on && !_headphones.value
        _headphones.value = on
        if (added && _playing.value && _settings.value.levelDb > Level.HEADPHONE_DB) setLevel(Level.HEADPHONE_DB)
    }

    // Moduswechsel beendet das Spielen: Ton und Sweep sind verschiedene Ablaeufe
    fun setMode(m: Mode) {
        if (m == _settings.value.mode) return
        stop()
        update { it.copy(mode = m) }
    }

    fun start(): Boolean {
        if (_playing.value) return true
        if (!player.start(_settings.value.params())) return false
        _playing.value = true
        return true
    }

    fun stop() {
        if (!_playing.value) return
        player.stop()
        _playing.value = false
    }

    fun toggle() = if (_playing.value) stop() else start().let { }

    // was gerade klingt: Sweep live, sonst die eingestellte Frequenz
    fun shownHz(): Double {
        val s = _settings.value
        if (s.mode == Mode.TONE) return s.hz
        return (if (_playing.value) player.liveHz() else null) ?: s.sweepFrom
    }

    fun progress(): Float? = if (_playing.value) player.progress() else null

    companion object {
        @Volatile private var instance: ToneStore? = null

        // Schutz (Kopfhoerer, Telefonat) haengt am Store, nicht am Dienst: wirkt auch ohne Vordergrund
        fun get(context: Context): ToneStore = instance ?: synchronized(this) {
            instance ?: Prefs(context).let { p ->
                val save = Debounced(SAVE_DELAY_MS) { s: Settings -> p.settings = s }
                ToneStore({ p.settings }, save::post, AudioEngine(), Freq.maxFor(AudioEngine.nativeRate()))
            }.also {
                instance = it
                AudioGuard(context, it)
            }
        }

        // Ziehen am Band schreibt nicht je Bild
        private const val SAVE_DELAY_MS = 400L

        @VisibleForTesting
        fun install(store: ToneStore?) {
            instance = store
        }
    }
}
