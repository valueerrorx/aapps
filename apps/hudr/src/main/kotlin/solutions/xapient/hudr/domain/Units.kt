package solutions.xapient.hudr.domain

import kotlin.math.roundToInt

// Geschwindigkeits-Einheit mit passender Strecken-Einheit
enum class SpeedUnit(val perMps: Float, val metersPerDistance: Float, val scales: List<Int>) {
    KMH(3.6f, 1000f, listOf(40, 80, 160, 240)),
    MPH(2.236936f, 1609.344f, listOf(30, 60, 120, 180)),
    KN(1.943844f, 1852f, listOf(20, 40, 80, 120));

    fun fromMps(mps: Float): Float = mps * perMps
    fun toMps(v: Float): Float = v / perMps
    fun distance(meters: Double): Double = meters / metersPerDistance

    companion object {
        // Laender mit Meilen auf Verkehrsschildern
        private val MILES = setOf("US", "GB", "LR", "MM")

        fun defaultFor(country: String?): SpeedUnit = if (country?.uppercase() in MILES) MPH else KMH
    }
}

// Beschriftung der Skala: hoechstens 6 Abschnitte, runde Schritte; 5 kleine Striche je Abschnitt
object Ticks {
    private val STEPS = listOf(5, 10, 20, 30, 40, 60)

    fun labelStep(scale: Int): Int = STEPS.firstOrNull { scale % it == 0 && scale / it <= 6 } ?: (scale / 4).coerceAtLeast(1)

    fun fraction(v: Float, scale: Int): Float = if (v.isNaN()) 0f else (v / scale).coerceIn(0f, 1f)
}

// Ganze Zahl wie angezeigt; ohne Wert "–"
fun formatSpeed(v: Float): String = if (v.isNaN()) "–" else v.roundToInt().coerceAtLeast(0).toString()

// Strecke: unter 100 mit einer Nachkommastelle, sonst ganz
fun formatDistance(v: Double, decimal: Char = '.'): String {
    if (v.isNaN()) return "–"
    if (v >= 99.95) return v.roundToInt().toString()
    val tenths = (v * 10).roundToInt()
    return "${tenths / 10}$decimal${tenths % 10}"
}
