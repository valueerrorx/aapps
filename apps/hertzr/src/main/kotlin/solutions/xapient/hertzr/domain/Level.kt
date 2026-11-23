package solutions.xapient.hertzr.domain

import kotlin.math.pow

enum class Warning { NONE, HEADPHONES, LOUD }

// Pegel in dBFS (Spitze des Sinus) und Schutzregeln
object Level {
    const val MIN_DB = -60
    const val MAX_DB = 0
    const val DEFAULT_DB = -24
    // nach App-Neustart hoechstens so laut
    const val RESTORE_MAX_DB = -12
    // ab hier Bernstein und Hinweis
    const val LOUD_DB = -10
    // mit Kopfhoerer ab hier Hinweis
    const val HEADPHONE_DB = -30

    fun amp(db: Int): Float = 10.0.pow(db / 20.0).toFloat()

    fun restored(db: Int): Int = db.coerceIn(MIN_DB, RESTORE_MAX_DB)

    fun warning(db: Int, headphones: Boolean): Warning = when {
        db > LOUD_DB -> Warning.LOUD
        headphones && db > HEADPHONE_DB -> Warning.HEADPHONES
        else -> Warning.NONE
    }

    // Lage auf dem Schieber 0..1
    fun toPos(db: Int): Float = (db - MIN_DB).toFloat() / (MAX_DB - MIN_DB)

    fun fromPos(pos: Float): Int = (MIN_DB + pos.coerceIn(0f, 1f) * (MAX_DB - MIN_DB)).let { kotlin.math.round(it).toInt() }
}
