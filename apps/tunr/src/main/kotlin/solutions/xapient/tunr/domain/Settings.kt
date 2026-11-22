package solutions.xapient.tunr.domain

data class Settings(
    val a4: Int = Notes.A4_DEFAULT,
    val notation: Notation = Notation.SHARP,
    val instrument: Instrument = Instrument.CHROMATIC,
    val haptics: Boolean = true,
) {
    fun sane(): Settings = copy(a4 = a4.coerceIn(A4_MIN, A4_MAX))

    companion object {
        const val A4_MIN = 415
        const val A4_MAX = 466
    }
}

enum class MicAccess { GRANTED, ASK, BLOCKED }

// Nie gefragt oder Begruendung erlaubt: fragen; sonst dauerhaft abgelehnt (nur App-Info hilft)
fun micAccess(granted: Boolean, askedBefore: Boolean, showRationale: Boolean): MicAccess = when {
    granted -> MicAccess.GRANTED
    !askedBefore || showRationale -> MicAccess.ASK
    else -> MicAccess.BLOCKED
}
