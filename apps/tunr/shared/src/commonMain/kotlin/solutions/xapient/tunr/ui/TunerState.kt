package solutions.xapient.tunr.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import solutions.xapient.tunr.domain.Frame
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Notes
import solutions.xapient.tunr.domain.PitchTracker
import solutions.xapient.tunr.domain.Pluck
import solutions.xapient.tunr.domain.Reading
import solutions.xapient.tunr.domain.TunerMotion
import kotlin.math.roundToInt

// Was die Komposition braucht; aendert sich selten (Note, Saite, Einrasten)
data class Coarse(
    val active: Boolean = false,
    val heard: Boolean = false,
    val target: Int = -1,
    val string: Int = -1,
    val inTune: Boolean = false,
    val locks: Int = 0,
    // -1 zu tief, 0 gestimmt/still, 1 zu hoch (Ansage)
    val dir: Int = 0,
    // Abweichung in 5-Cent-Schritten (Ansage, Zustandszeile); 0 ohne Ton
    val cents5: Int = 0,
)

// Referenzton-Saite (-1 = keiner); pose >= 0 = festes Leuchten statt Abklingen
data class ToneMark(val string: Int = -1, val id: Int = 0, val pose: Float = -1f)

// Anzeige-Zustand auf dem Main-Thread; Feinwerte nur in der Zeichenphase lesen (frame)
@Stable
class TunerState(a4: Int = Notes.A4_DEFAULT, instrument: Instrument = Instrument.CHROMATIC) {
    private val tracker = PitchTracker(a4, instrument)
    val motion = TunerMotion()

    // Feinwerte: kein Snapshot-State, gelesen nach frame
    var reading: Reading = tracker.reading
        private set

    var frame by mutableLongStateOf(0L)
        internal set
    // weckt die Bildschleife
    var version by mutableIntStateOf(0)
        private set
    var coarse by mutableStateOf(Coarse())
        private set

    // Mikrofon liefert nur Nullen (systemweit gesperrt oder von anderer App belegt)
    var micSilent by mutableStateOf(false)
        private set

    // Referenzton: welche Saite gerade klingt; id zaehlt jeden Anschlag (Leuchten neu starten)
    var tone by mutableStateOf(ToneMark())
        private set

    // eigener Ton klingt: Anzeige aus, Mikrofon zaehlt erst nach dem Ausklingen wieder
    fun reference(string: Int) {
        tracker.hush(Pluck.HUSH_FRAMES)
        tone = ToneMark(string, tone.id + 1)
        update(tracker.reading)
    }

    // festes Leuchten ohne Bewegung, z. B. fuer Screenshots
    fun showTone(string: Int, level: Float) {
        tone = ToneMark(string, tone.id + 1, level)
    }

    fun push(f: Frame) {
        if (f.dead != micSilent) micSilent = f.dead
        update(tracker.push(f))
    }

    fun stop() {
        micSilent = false
        tracker.stop()
        update(tracker.reading)
    }

    fun configure(a4: Int, instrument: Instrument) {
        tracker.configure(a4, instrument)
        update(tracker.reading)
    }

    fun lock(index: Int) {
        if (index == tracker.locked) return
        tracker.lock(index)
        update(tracker.reading)
    }

    // fester Wert ohne Bewegung, z. B. fuer Screenshots
    fun show(r: Reading, strobeDeg: Float = 0f, shine: Float = 1f) {
        reading = r
        motion.pose(r, strobeDeg, shine)
        coarse = coarseOf(r)
        frame++
    }

    private fun update(r: Reading) {
        if (r == reading) return
        reading = r
        motion.setTarget(r)
        val c = coarseOf(r)
        if (c != coarse) coarse = c
        version++
        // auch bei ruhender Feder neu zeichnen (Zahlen)
        frame++
    }

    private fun coarseOf(r: Reading) = Coarse(
        r.active, r.heard, r.target, r.string, r.inTune, r.locks,
        dir = if (!r.active || r.inTune) 0 else if (r.cents > 0) 1 else -1,
        cents5 = if (r.active) (r.cents / 5f).roundToInt() * 5 else 0,
    )
}
