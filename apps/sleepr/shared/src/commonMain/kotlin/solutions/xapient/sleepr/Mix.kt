package solutions.xapient.sleepr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import solutions.xapient.aapps.core.ui.KeyValues
import kotlin.math.pow

// id ist stabil, der Name nur Anzeige (Texte in Texts.kt)
class Preset(val id: String, val levels: Map<Int, Int>, val tone: Int, val waves: Map<Int, Int> = emptyMap())

/** Mischung wie Mixer auf Android: gemeinsamer Zustand von Oberflaeche und Audio, in den UserDefaults gespeichert */
class Mix(private val kv: KeyValues? = null) {
    // Compose-Zustand: die Oberflaeche zeichnet bei jeder Aenderung neu, der Audio-Thread liest blockweise
    val levels = mutableStateListOf(0, 20, 60, 0, 0)
    val waves = mutableStateListOf(false, false, false, false, false) // Wellen-Modulation je Farbe
    val waveSec = mutableStateListOf(8, 8, 8, 8, 8)                   // mittlere Wellenlaenge in s
    var master by mutableIntStateOf(70)
    var tone by mutableIntStateOf(100)
    var timer by mutableIntStateOf(0)

    init { load() }

    /** Nach jeder Aenderung aus der Oberflaeche: speichern */
    fun changed() = save()

    fun toneHz(v: Int = tone) = toneHzOf(v)

    /** true, solange die aktuelle Einstellung genau dieser Mischung entspricht */
    fun matches(p: Preset): Boolean = tone == p.tone && levels.indices.all { i ->
        levels[i] == (p.levels[i] ?: 0) && waves[i] == p.waves.containsKey(i) && (p.waves[i] ?: waveSec[i]) == waveSec[i]
    }

    fun applyPreset(p: Preset) {
        for (i in levels.indices) {
            levels[i] = p.levels[i] ?: 0
            waves[i] = p.waves.containsKey(i)
            p.waves[i]?.let { waveSec[i] = it }
        }
        tone = p.tone
        changed()
    }

    private fun load() {
        val p = kv ?: return
        for (i in levels.indices) {
            levels[i] = p.int("l$i", levels[i])
            waves[i] = p.bool("w$i", waves[i])
            waveSec[i] = p.int("ws$i", waveSec[i])
        }
        master = p.int("master", master); tone = p.int("tone", tone); timer = p.int("timer", timer)
    }

    private fun save() {
        val p = kv ?: return
        for (i in levels.indices) {
            p.putInt("l$i", levels[i]); p.putBool("w$i", waves[i]); p.putInt("ws$i", waveSec[i])
        }
        p.putInt("master", master); p.putInt("tone", tone); p.putInt("timer", timer)
    }

    companion object {
        val PRESETS = listOf(
            Preset("ocean", mapOf(2 to 70, 1 to 35), 70, waves = mapOf(2 to 9, 1 to 13)),
            Preset("focus", mapOf(1 to 55, 2 to 35), 100),
            Preset("sleep", mapOf(2 to 70), 55),
            Preset("mask", mapOf(3 to 50, 1 to 40), 100),
            Preset("nature", mapOf(4 to 60, 2 to 25), 85),
            Preset("white", mapOf(0 to 45), 80),
        )
        val TIMERS = intArrayOf(0, 15, 30, 45, 60, 90)
        const val WAVE_MIN = 3
        const val WAVE_MAX = 20

        fun gain(v: Int) = (v / 100.0).pow(2) // wahrnehmungsnahe Kurve
        fun gain(v: Double) = (v / 100.0).pow(2)
        fun toneHzOf(v: Int) = 400.0 * 50.0.pow(v / 100.0) // 400 Hz … 20 kHz log
    }
}

/** Anzeige-Einstellungen; aendern nur, was sichtbar ist, nie den Klang */
class Display(private val kv: KeyValues? = null) {
    var showWaves by mutableStateOf(kv?.bool("showWaves", true) ?: true)
        private set
    var showMixes by mutableStateOf(kv?.bool("showMixes", true) ?: true)
        private set

    fun setWaves(v: Boolean) { showWaves = v; kv?.putBool("showWaves", v) }
    fun setMixes(v: Boolean) { showMixes = v; kv?.putBool("showMixes", v) }

    companion object {
        // Wellenlaengen-Regler nur bei aktiven Wellen und eingeblendeten Wellen-Schaltern
        fun waveBoxVisible(waveOn: Boolean, showWaves: Boolean) = waveOn && showWaves
    }
}
