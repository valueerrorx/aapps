package solutions.xapient.pulsr.domain

// Unterteilung eines Schlags
enum class Subdivision(val count: Int) { QUARTER(1), EIGHTH(2), TRIPLET(3), SIXTEENTH(4) }

enum class Sound { CLICK, WOOD, BEEP }

// Art eines Klicks im Takt
enum class Kind { ACCENT, BEAT, SUB }

object Tempo {
    const val MIN = 20
    const val MAX = 300
    const val DEFAULT = 120

    fun clamp(bpm: Int): Int = bpm.coerceIn(MIN, MAX)

    // Italienische Tempobezeichnung, Grenzen nach gaengiger Metronom-Skala
    fun marking(bpm: Int): String = when {
        bpm < 40 -> "Grave"
        bpm < 60 -> "Largo"
        bpm < 66 -> "Larghetto"
        bpm < 76 -> "Adagio"
        bpm < 108 -> "Andante"
        bpm < 120 -> "Moderato"
        bpm < 156 -> "Allegro"
        bpm < 176 -> "Vivace"
        bpm < 200 -> "Presto"
        else -> "Prestissimo"
    }
}

object Beats {
    const val MIN = 1
    const val MAX = 12
    fun clamp(n: Int): Int = n.coerceIn(MIN, MAX)
}

data class Settings(
    val bpm: Int = Tempo.DEFAULT,
    val beats: Int = 4,
    val sub: Subdivision = Subdivision.QUARTER,
    val sound: Sound = Sound.CLICK,
    val accent: Boolean = true,
    val keepOn: Boolean = true,
) {
    // gespeicherte Werte koennen kaputt sein: immer in den gueltigen Bereich
    fun sane(): Settings = copy(bpm = Tempo.clamp(bpm), beats = Beats.clamp(beats))
}
