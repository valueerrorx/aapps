package solutions.xapient.magnr.domain

// Magnetometer-Werte in µT; status 0 (unzuverlaessig) .. 3 (hoch)
fun interface FieldListener {
    fun onField(x: Float, y: Float, z: Float, status: Int, timestampNanos: Long)
}

interface FieldSource {
    val isAvailable: Boolean
    // Messbereich je Achse in µT, 0 = unbekannt
    val maxRange: Float
    fun start(listener: FieldListener)
    fun stop()
}

// Piepton-Ausgabe; strength wird laufend nachgefuehrt
interface Beeper {
    var strength: Float
    fun start()
    fun stop()
}
