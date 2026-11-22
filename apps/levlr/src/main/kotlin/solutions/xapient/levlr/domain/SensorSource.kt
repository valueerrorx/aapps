package solutions.xapient.levlr.domain

// Quelle fuer Schwerkraft-Messwerte in Geraetekoordinaten (m/s²)
interface SensorSource {
    val isAvailable: Boolean
    fun start(listener: (Vec3, Long) -> Unit)
    fun stop()
}
