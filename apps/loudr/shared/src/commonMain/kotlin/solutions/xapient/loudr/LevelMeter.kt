package solutions.xapient.loudr

import solutions.xapient.loudr.domain.Reading

// Pegelquelle fuer die Oberflaeche; im Test ersetzt
interface LevelMeter {
    fun start(): Boolean
    fun stop()
    fun reset()
    fun reading(): Reading
    fun save(): DoubleArray
    fun restore(state: DoubleArray)
    // Aufnahme abgebrochen, kommt vom Audio-Thread
    var onFailure: (() -> Unit)?
    // System liefert nur Stille (Datenschutzschalter, andere App hat Vorrang), kommt vom Audio-Thread
    var onSilenced: ((Boolean) -> Unit)?
}
