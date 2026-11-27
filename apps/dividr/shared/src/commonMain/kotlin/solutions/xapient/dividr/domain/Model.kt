package solutions.xapient.dividr.domain

// IDs: oberes Wort = Geraete-ID (Zufall je Installation), unteres = laufende Nummer dieses Geraets
object Ids {
    fun of(device: Int, seq: Int): Long = (device.toLong() shl 32) or (seq.toLong() and 0xFFFFFFFFL)
    fun device(id: Long): Int = (id ushr 32).toInt()
    fun seq(id: Long): Int = id.toInt()
}

// Versionsstempel: v steigt bei jeder Aenderung, by = Geraet der letzten Aenderung (nur Tie-Break)
data class Stamp(val v: Int, val by: Int)

// Gruppenkopf als ein LWW-Datensatz
data class Meta(val name: String, val currency: String, val stamp: Stamp)

data class Member(
    val id: Long,
    val name: String,
    val stamp: Stamp,
    val alias: Long? = null, // nach „zusammenlegen": Ziel-Person
    val removed: Boolean = false,
    val fromStart: Boolean = true, // false = zaehlt nur bei danach erstellten „alle"-Ausgaben
    val known: Map<Int, Int> = emptyMap(), // Wissensstand beim Aufnehmen: hoechste Nummer je Geraet
)

enum class Kind { EXPENSE, PAYMENT }

data class Entry(
    val id: Long,
    val stamp: Stamp,
    val kind: Kind = Kind.EXPENSE,
    val title: String = "",
    val amount: Long = 0, // Minor Units der Eintragswaehrung
    val currency: String = "",
    val rateMicros: Long = 0, // 1 Einheit Eintragswaehrung = rateMicros/1e6 Gruppenwaehrung
    val payer: Long = 0,
    val forAll: Boolean = true,
    val forIds: Set<Long> = emptySet(), // nur ohne forAll; bei Zahlung genau der Empfaenger
    val day: Int = 0, // Tage seit 1970, nur Anzeige
    val deleted: Boolean = false,
) {
    // Grabstein ohne Inhalt: auf allen Geraeten gleich
    fun tombstone(stamp: Stamp) = Entry(id, stamp, deleted = true)
}

data class Group(
    val ids: Set<Long>, // nach Zusammenlegen mehrere
    val meta: Meta,
    val members: Map<Long, Member> = emptyMap(),
    val entries: Map<Long, Entry> = emptyMap(),
) {
    val name get() = meta.name
    val currency get() = meta.currency

    fun sameGroup(other: Group) = ids.any { it in other.ids }

    companion object {
        const val MAX_NAME = 24
        const val MAX_TITLE = 40
        const val MAX_AMOUNT = 10_000_000_000L

        fun cleanName(s: String) = s.replace(Regex("\\s+"), " ").trimStart().take(MAX_NAME)
        fun cleanTitle(s: String) = s.replace(Regex("\\s+"), " ").trimStart().take(MAX_TITLE)

        // Vergleich von Namen: Gross/Klein und Leerzeichen egal
        fun nameKey(s: String) = s.trim().replace(Regex("\\s+"), " ").lowercase()
    }
}
