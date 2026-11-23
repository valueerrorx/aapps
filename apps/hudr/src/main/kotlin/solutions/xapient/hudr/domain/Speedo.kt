package solutions.xapient.hudr.domain

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
    // Vorhersage in 3 s aus der Beschleunigung; NaN ohne Fahrt
    val trend: Float = Float.NaN,
) {
    companion object {
        val SEARCHING = Reading(GpsPhase.SEARCHING, Float.NaN, false, Float.NaN, 0, 0, Float.NaN, Float.NaN, 0.0, Alert.NONE)
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

    // true = Schwelle gerade ueberschritten (Ton)
    fun onFix(fix: Fix, unit: SpeedUnit, limit: Int?): Boolean {
        val prev = last
        if (prev != null && fix.nanos <= prev.nanos) return false
        val v = filter.update(fix)
        trip.add(fix, v, filter.moving, filter.reliable)
        last = fix
        return if (v.isNaN()) false else warning.update(unit.fromMps(v), limit)
    }

    // true nur bei geaenderter Zahl: sonst kein neues Bild
    fun onSatellites(used: Int, visible: Int): Boolean {
        val u = used.coerceAtLeast(0)
        val v = visible.coerceAtLeast(u)
        if (u == this.used && v == this.visible) return false
        this.used = u
        this.visible = v
        return true
    }

    fun phase(now: Long): GpsPhase {
        val f = last ?: return GpsPhase.SEARCHING
        return if (now - f.nanos > STALE_NS) GpsPhase.LOST else GpsPhase.FIX
    }

    fun reading(now: Long, unit: SpeedUnit, limit: Int?): Reading {
        val phase = phase(now)
        val live = phase == GpsPhase.FIX
        val v = filter.speed
        val maxV = if (trip.maxMps > 0f || trip.movingSeconds > 0) unit.fromMps(trip.maxMps) else Float.NaN
        val moving = live && filter.moving
        return Reading(
            phase = phase,
            speed = if (live && !v.isNaN()) unit.fromMps(v) else Float.NaN,
            moving = moving,
            accuracy = last?.accuracy ?: Float.NaN,
            used = used,
            visible = visible,
            max = maxV,
            avg = trip.avgMps.let { if (it.isNaN()) it else unit.fromMps(it) },
            distance = unit.distance(trip.meters),
            alert = if (live && limit != null) warning.state else Alert.NONE,
            trend = if (moving) unit.fromMps((v + TREND_S * filter.accel).coerceAtLeast(0f)) else Float.NaN,
        )
    }

    // Fahrtwerte neu; GPS-Stand bleibt
    fun reset() {
        trip.reset()
        warning.reset()
    }

    // GPS neu gestartet (zurueck in den Vordergrund): alte Glaettung verwerfen
    fun restart() {
        filter.reset()
        last = null
        used = 0
        visible = 0
    }

    fun save(): DoubleArray = trip.save()

    fun restore(s: DoubleArray) = trip.restore(s)

    companion object {
        const val STALE_NS = 5_000_000_000L
        const val TREND_S = 3f
    }
}
