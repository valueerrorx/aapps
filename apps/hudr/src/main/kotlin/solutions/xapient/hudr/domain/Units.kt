package solutions.xapient.hudr.domain

import kotlin.math.roundToInt

// Geschwindigkeits-Einheit mit passender Strecken-Einheit
enum class SpeedUnit(val perMps: Float, val metersPerDistance: Float) {
    KMH(3.6f, 1000f),
    MPH(2.236936f, 1609.344f),
    KN(1.943844f, 1852f);

    fun fromMps(mps: Float): Float = mps * perMps
    fun toMps(v: Float): Float = v / perMps
    fun distance(meters: Double): Double = meters / metersPerDistance

    companion object {
        // Laender mit Meilen auf Verkehrsschildern
        private val MILES = setOf("US", "GB", "LR", "MM")

        fun defaultFor(country: String?): SpeedUnit = if (country?.uppercase() in MILES) MPH else KMH
    }
}

// Geschwindigkeitsband: sichtbarer Ausschnitt um den Wert, Striche je 5, Zahlen je 10 oder 20
object Tape {
    // sichtbare Einheiten ueber die ganze Laenge
    const val SPAN = 60f
    const val TICK = 5

    // Zahlen je 10, wenn sie sich nicht draengen, sonst je 20
    fun labelStep(pxPerUnit: Float, labelPx: Float): Int = if (10 * pxPerUnit >= labelPx * 1.6f) 10 else 20

    // Lage eines Werts entlang des Bands, 0 = Zeiger in der Mitte
    fun offset(v: Float, center: Float, pxPerUnit: Float): Float = (v - center) * pxPerUnit
}

// Ganze Zahl wie angezeigt; ohne Wert "–"
fun formatSpeed(v: Float): String = if (v.isNaN()) "–" else v.roundToInt().coerceAtLeast(0).toString()

// Ansage nur bei Wechsel von Warnzustand oder Zehnerstelle
fun announceKey(r: Reading): String =
    if (r.phase != GpsPhase.FIX || r.speed.isNaN()) r.phase.name else "${r.alert}:${(r.speed.roundToInt() / 10)}"

// Strecke: unter 100 mit einer Nachkommastelle, sonst ganz
fun formatDistance(v: Double, decimal: Char = '.'): String {
    if (v.isNaN()) return "–"
    if (v >= 99.95) return v.roundToInt().toString()
    val tenths = (v * 10).roundToInt()
    return "${tenths / 10}$decimal${tenths % 10}"
}
