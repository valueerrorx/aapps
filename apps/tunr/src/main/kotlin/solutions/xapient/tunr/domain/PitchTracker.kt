package solutions.xapient.tunr.domain

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

// Ein Analyse-Ergebnis vom Audio-Thread; hz <= 0 = kein Ton erkannt, dead = seit Sekunden nur Nullen
data class Frame(val hz: Float, val rmsDb: Float, val dead: Boolean = false)

// Was die Anzeige braucht; cents relativ zur Zielnote (Saite oder naechster Halbton)
data class Reading(
    val active: Boolean = false,
    val heard: Boolean = false,
    val hz: Float = 0f,
    val target: Int = -1,
    val cents: Float = 0f,
    val string: Int = -1,
    val inTune: Boolean = false,
    // zaehlt jedes Einrasten (Haptik, Glanz)
    val locks: Int = 0,
)

// Glaettet Rohwerte zu einer ruhigen Anzeige; laeuft auf dem Main-Thread
class PitchTracker(a4: Int = Notes.A4_DEFAULT, instrument: Instrument = Instrument.CHROMATIC) {
    var a4: Int = a4
        private set
    var instrument: Instrument = instrument
        private set
    var locked: Int = -1
        private set

    private val hist = FloatArray(MEDIAN)
    private var histN = 0
    private var histPos = 0
    private var smooth = 0f
    private var tracking = false
    private var pending = Float.NaN
    private var gate = false
    private var quiet = 0
    private var target = -1
    private var string = -1
    private var inTune = false
    private var heard = false
    private var active = false
    private var locks = 0

    var reading = Reading()
        private set

    // Kammerton/Instrument neu: alte Werte gelten nicht mehr
    fun configure(a4: Int, instrument: Instrument) {
        if (a4 == this.a4 && instrument == this.instrument) return
        this.a4 = a4
        if (instrument != this.instrument) locked = -1
        this.instrument = instrument
        stop()
        string = -1; target = -1; heard = false
        publish()
    }

    // festgehaltene Saite (-1 = automatisch)
    fun lock(index: Int) {
        locked = if (index in instrument.strings.indices) index else -1
        if (locked >= 0) {
            string = locked
            target = instrument.strings[locked]
        } else if (tracking) {
            string = -1
            chooseTarget()
        }
        publish()
    }

    fun push(f: Frame): Reading {
        gate = if (gate) f.rmsDb >= CLOSE_DB else f.rmsDb >= OPEN_DB
        if (!gate || f.hz <= 0f) {
            pending = Float.NaN
            if (++quiet >= QUIET_FRAMES && active) {
                active = false
                tracking = false
                inTune = false
            }
            return publish()
        }
        quiet = 0
        val c = Notes.centsFromA4(f.hz, a4.toFloat())
        when {
            // erster Wert nach Stille zaehlt erst mit einem passenden zweiten (Anschlag, Stoergeraeusch)
            !tracking -> if (!pending.isNaN() && abs(c - pending) <= JUMP) {
                begin(c)
            } else {
                pending = c
                return publish()
            }
            abs(c - smooth) > JUMP -> {
                // Sprung zaehlt erst, wenn der naechste Wert ihn bestaetigt
                if (!pending.isNaN() && abs(c - pending) <= JUMP) {
                    begin(c)
                } else {
                    pending = c
                    return publish()
                }
            }
            else -> {
                pending = Float.NaN
                add(c)
                smooth += ALPHA * (median() - smooth)
            }
        }
        active = true
        heard = true
        chooseTarget()
        val cents = centsToTarget()
        if (!inTune && abs(cents) <= IN_TUNE) {
            inTune = true
            locks++
        } else if (inTune && abs(cents) > OUT_OF_TUNE) {
            inTune = false
        }
        return publish()
    }

    // Mikrofon aus: kein Ton mehr, letzte Note bleibt fuer die gedimmte Anzeige
    fun stop() {
        active = false
        tracking = false
        inTune = false
        gate = false
        quiet = 0
        pending = Float.NaN
        publish()
    }

    private fun begin(c: Float) {
        hist[0] = c
        histN = 1
        histPos = 1
        smooth = c
        pending = Float.NaN
        tracking = true
        inTune = false
        if (locked < 0) string = -1
        target = if (locked >= 0) target else -1
    }

    private fun add(c: Float) {
        hist[histPos] = c
        histPos = (histPos + 1) % MEDIAN
        if (histN < MEDIAN) histN++
    }

    private fun median(): Float {
        val s = hist.copyOf(histN)
        s.sort()
        return if (histN % 2 == 1) s[histN / 2] else (s[histN / 2 - 1] + s[histN / 2]) / 2
    }

    private fun centsOf(midi: Int) = (midi - Notes.A4_MIDI) * 100f

    private fun centsToTarget() = smooth - centsOf(target)

    private fun chooseTarget() {
        val strings = instrument.strings
        if (strings.isEmpty()) {
            val nearest = (smooth / 100f).roundToInt() + Notes.A4_MIDI
            if (target < 0 || abs(smooth - centsOf(target)) > NOTE_HOLD) target = nearest
            string = -1
            return
        }
        if (locked >= 0) {
            string = locked
        } else {
            val n = instrument.nearest(smooth)
            string = when {
                string < 0 -> n
                n != string && abs(smooth - centsOf(strings[n])) < abs(smooth - centsOf(strings[string])) - STRING_HOLD -> n
                else -> string
            }
        }
        target = strings[string]
    }

    private fun publish(): Reading {
        val hz = if (heard) a4 * 2.0.pow(smooth / 1200.0).toFloat() else 0f
        reading = Reading(
            active = active,
            heard = heard || locked >= 0,
            hz = hz,
            target = target,
            cents = if (target >= 0 && heard) centsToTarget() else 0f,
            string = string,
            inTune = inTune && active,
            locks = locks,
        )
        return reading
    }

    companion object {
        const val OPEN_DB = -50f
        const val CLOSE_DB = -56f
        // ~150 ms bei 25 ms je Analyse
        const val QUIET_FRAMES = 6
        const val MEDIAN = 5
        const val ALPHA = 0.4f
        const val JUMP = 60f
        const val IN_TUNE = 5f
        const val OUT_OF_TUNE = 7f
        const val NOTE_HOLD = 55f
        const val STRING_HOLD = 30f
    }
}
