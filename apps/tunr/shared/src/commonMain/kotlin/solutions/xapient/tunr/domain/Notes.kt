package solutions.xapient.tunr.domain

import kotlin.math.log2
import kotlin.math.pow

enum class Notation { SHARP, FLAT, GERMAN }

enum class Accidental(val symbol: String) { NONE(""), SHARP("♯"), FLAT("♭") }

// Notenname zerlegt, damit die Anzeige Vorzeichen und Oktave getrennt setzen kann
data class NoteName(val letter: String, val accidental: Accidental, val octave: Int) {
    val text: String get() = letter + accidental.symbol
    override fun toString(): String = "$text$octave"
}

// MIDI-Nummern (A4 = 69), Cent relativ zum Kammerton
object Notes {
    const val A4_MIDI = 69
    const val A4_DEFAULT = 440

    private val sharp = listOf("C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B")
    private val flat = listOf("C", "D", "D", "E", "E", "F", "G", "G", "A", "A", "B", "B")
    private val black = setOf(1, 3, 6, 8, 10)

    // Cent ueber A4 (beliebige Frequenz)
    fun centsFromA4(hz: Float, a4: Float): Float = (1200.0 * log2(hz.toDouble() / a4)).toFloat()

    fun hz(midi: Int, a4: Float): Float = (a4 * 2.0.pow((midi - A4_MIDI) / 12.0)).toFloat()

    fun name(midi: Int, notation: Notation): NoteName {
        val pc = (midi).mod(12)
        val octave = (midi).floorDiv(12) - 1
        return when {
            notation == Notation.GERMAN && pc == 10 -> NoteName("B", Accidental.NONE, octave)
            notation == Notation.GERMAN && pc == 11 -> NoteName("H", Accidental.NONE, octave)
            pc !in black -> NoteName(sharp[pc], Accidental.NONE, octave)
            notation == Notation.FLAT -> NoteName(flat[pc], Accidental.FLAT, octave)
            else -> NoteName(sharp[pc], Accidental.SHARP, octave)
        }
    }
}
