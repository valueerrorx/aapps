package solutions.xapient.hudr.domain

import kotlin.math.max
import kotlin.math.roundToInt

enum class GpsPhase { SEARCHING, FIX, LOST }

// Ein Stand fuer die Anzeige, alles schon in der gewaehlten Einheit
data class Reading(
    val phase: GpsPhase,
    val speed: Float,
    val moving: Boolean,
    val accuracy: Float,
    val used: Int,
    val visible: Int,
    val max: Float,
    val avg: Float,
    val distance: Double,
    val alert: Alert,
    val scale: Int,
) {
    companion object {
        fun searching(unit: SpeedUnit, scale: Int = unit.scales.first()) =
            Reading(GpsPhase.SEARCHING, Float.NaN, false, Float.NaN, 0, 0, Float.NaN, Float.NaN, 0.0, Alert.NONE, scale)
    }
}

/**
 * Tacho ohne Android: Fixes rein, Stand raus.
 * Uhr kommt als Parameter (elapsedRealtimeNanos), damit Veralten testbar ist.
 */
class Speedo {
    private val filter = SpeedFilter()
    private val trip = Trip()
    private val warning = Warning()
    private var last: Fix? = null
    private var used = 0
    private var visible = 0
    // Skala waechst nur; Index in SpeedUnit.scales, gleich fuer alle Einheiten
    private var scaleIndex = 0

    // true = Schwelle gerade ueberschritten (Ton)
    fun onFix(fix: Fix, unit: SpeedUnit, limit: Int?): Boolean {
        val prev = last
        if (prev != null && fix.nanos <= prev.nanos) return false
        val v = filter.update(fix)
        trip.add(fix, v, filter.moving)
        last = fix
        grow(unit, limit)
        return if (v.isNaN()) false else warning.update(unit.fromMps(v), limit)
    }

    fun onSatellites(used: Int, visible: Int) {
        this.used = used.coerceAtLeast(0)
        this.visible = visible.coerceAtLeast(this.used)
    }

    fun phase(now: Long): GpsPhase {
        val f = last ?: return GpsPhase.SEARCHING
        return if (now - f.nanos > STALE_NS) GpsPhase.LOST else GpsPhase.FIX
    }

    fun reading(now: Long, unit: SpeedUnit, limit: Int?): Reading {
        grow(unit, limit)
        val phase = phase(now)
        val live = phase == GpsPhase.FIX
        val v = filter.speed
        val maxV = if (trip.maxMps > 0f || trip.movingSeconds > 0) unit.fromMps(trip.maxMps) else Float.NaN
        return Reading(
            phase = phase,
            speed = if (live && !v.isNaN()) unit.fromMps(v) else Float.NaN,
            moving = live && filter.moving,
            accuracy = last?.accuracy ?: Float.NaN,
            used = used,
            visible = visible,
            max = maxV,
            avg = trip.avgMps.let { if (it.isNaN()) it else unit.fromMps(it) },
            distance = unit.distance(trip.meters),
            alert = if (live && limit != null) warning.state else Alert.NONE,
            scale = unit.scales[scaleIndex],
        )
    }

    private fun grow(unit: SpeedUnit, limit: Int?) {
        val need = max(unit.fromMps(trip.maxMps), max(unit.fromMps(filter.speed.takeUnless { it.isNaN() } ?: 0f), (limit ?: 0).toFloat()))
        scaleIndex = max(scaleIndex, fit(unit, need))
    }

    // Fahrtwerte und Skala neu; GPS-Stand bleibt
    fun reset() {
        trip.reset()
        warning.reset()
        scaleIndex = 0
    }

    // GPS neu gestartet (zurueck in den Vordergrund): alte Glaettung verwerfen
    fun restart() {
        filter.reset()
        last = null
        used = 0
        visible = 0
    }

    fun save(): DoubleArray = trip.save() + scaleIndex.toDouble()

    fun restore(s: DoubleArray) {
        if (s.size < 4) return
        trip.restore(s.copyOfRange(0, 3))
        scaleIndex = s[3].roundToInt().coerceIn(0, SpeedUnit.KMH.scales.lastIndex)
    }

    companion object {
        const val STALE_NS = 5_000_000_000L

        // kleinste Skala mit etwas Luft ueber dem Bedarf
        fun fit(unit: SpeedUnit, need: Float): Int {
            val i = unit.scales.indexOfFirst { it >= need * 1.05f }
            return if (i < 0) unit.scales.lastIndex else i
        }
    }
}
