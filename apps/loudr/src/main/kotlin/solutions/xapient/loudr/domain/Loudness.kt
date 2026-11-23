package solutions.xapient.loudr.domain

import kotlin.math.abs
import kotlin.math.roundToInt

// Farbzonen nach Pegel
enum class Zone(val from: Float) {
    QUIET(Float.NEGATIVE_INFINITY), MODERATE(55f), LOUD(70f), VERY_LOUD(85f);

    companion object {
        fun of(db: Float): Zone = entries.last { db >= it.from }
    }
}

// Alltagsvergleich, grob je 10 dB(A)
enum class Example(val from: Float) {
    SILENCE(Float.NEGATIVE_INFINITY),
    WHISPER(25f),
    QUIET_ROOM(35f),
    OFFICE(45f),
    CONVERSATION(55f),
    VACUUM(65f),
    TRAFFIC(75f),
    MOWER(85f),
    MOTORBIKE(95f),
    CONCERT(105f),
    SIREN(115f);

    companion object {
        fun of(db: Float): Example? = if (db.isNaN()) null else entries.last { db >= it.from }
    }
}

// Skala von Bogen und Verlauf; der Verlauf zeigt den Alltagsbereich enger
object Scale {
    const val MIN = 20f
    const val MAX = 120f
    const val HISTORY_MIN = 30f
    const val HISTORY_MAX = 110f
    fun fraction(db: Float, lo: Float = MIN, hi: Float = MAX): Float = if (db.isNaN()) 0f else ((db - lo) / (hi - lo)).coerceIn(0f, 1f)
}

// Versatz gegen ein Referenzgeraet
object Calibration {
    const val MIN = -20f
    const val MAX = 20f
    const val STEP = 0.5f

    fun clamp(offset: Float): Float =
        if (offset.isNaN()) 0f else ((offset / STEP).roundToInt() * STEP).coerceIn(MIN, MAX)

    fun step(offset: Float, dir: Int): Float = clamp(clamp(offset) + dir * STEP)

    fun apply(db: Float, offset: Float): Float = if (db.isNaN()) db else db + offset

    // "+2.5", "0", "−1" (ohne Einheit); Dezimalzeichen je Sprache
    fun label(offset: Float, decimal: Char = '.'): String {
        val o = clamp(offset)
        if (o == 0f) return "0"
        val sign = if (o > 0) "+" else "−"
        val a = abs(o)
        return sign + a.toInt() + if (a % 1f == 0f) "" else "${decimal}5"
    }
}

// Ganzzahlige Anzeige, "—" ohne Wert
fun formatDb(db: Float): String = if (db.isNaN()) "—" else db.roundToInt().coerceAtLeast(0).toString()
