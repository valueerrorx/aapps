package solutions.xapient.spinr.domain

import java.text.BreakIterator

// Ein gespeichertes Rad; entries darf leere Zeilen enthalten (Bearbeiten), labels nicht
data class Wheel(val name: String, val entries: List<String>) {
    val labels: List<String> get() = entries.map { it.trim() }.filter { it.isNotEmpty() }
    val canSpin: Boolean get() = labels.size >= MIN_ENTRIES

    companion object {
        const val MIN_ENTRIES = 2
        const val MAX_ENTRIES = 50
        const val MAX_LENGTH = 40

        // Steuerzeichen raus (auch die Trenner des Speicherformats), Zeilen zu Leerzeichen, Laenge begrenzt
        fun clean(text: String): String {
            val s = text.map { if (it == '\n' || it == '\r' || it == '\t') ' ' else it }
                .filter { !it.isISOControl() }
                .joinToString("")
            if (s.length <= MAX_LENGTH) return s
            // nur an Zeichengrenzen kuerzen: kein halbes Emoji, kein loser Akzent
            val bi = BreakIterator.getCharacterInstance().apply { setText(s) }
            return s.substring(0, if (bi.isBoundary(MAX_LENGTH)) MAX_LENGTH else bi.preceding(MAX_LENGTH))
        }
    }
}

// Speicherformat: Raeder mit RS getrennt, Name und Eintraege mit US
object WheelCodec {
    private const val RS = '\u001E'
    private const val US = '\u001F'

    fun encode(wheels: List<Wheel>): String =
        wheels.joinToString(RS.toString()) { w ->
            (listOf(w.name) + w.entries).joinToString(US.toString()) { Wheel.clean(it) }
        }

    // Tolerant: kaputte oder leere Teile werden uebersprungen
    fun decode(text: String?): List<Wheel> {
        if (text.isNullOrEmpty()) return emptyList()
        return text.split(RS).mapNotNull { part ->
            val fields = part.split(US)
            if (fields.size < 2) return@mapNotNull null
            Wheel(Wheel.clean(fields[0]), fields.drop(1).map(Wheel::clean).take(Wheel.MAX_ENTRIES))
        }
    }
}

// Eine Runde: welche Eintraege noch auf dem Rad sind (Indizes in labels)
data class Round(val size: Int, val removed: Set<Int> = emptySet()) {
    val remaining: List<Int> get() = (0 until size).filter { it !in removed }
    val canSpin: Boolean get() = remaining.size >= Wheel.MIN_ENTRIES
    val canRemove: Boolean get() = remaining.size > Wheel.MIN_ENTRIES

    fun remove(label: Int): Round = if (canRemove && label in remaining) copy(removed = removed + label) else this

    // Segment auf dem Rad (Index in remaining) herausnehmen
    fun removeShown(segment: Int): Round = remaining.getOrNull(segment)?.let { remove(it) } ?: this
    fun restore(): Round = copy(removed = emptySet())
}
