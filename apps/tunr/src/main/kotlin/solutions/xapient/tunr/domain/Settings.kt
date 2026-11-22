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

// Gesperrt nur mit Beleg (Anfrage kam ohne Dialog sofort abgelehnt zurueck); Wegtippen des Dialogs zaehlt nicht
fun micAccess(granted: Boolean, deniedWithoutDialog: Boolean, showRationale: Boolean): MicAccess = when {
    granted -> MicAccess.GRANTED
    showRationale || !deniedWithoutDialog -> MicAccess.ASK
    else -> MicAccess.BLOCKED
}
