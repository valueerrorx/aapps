package solutions.xapient.hudr.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

// Kritisch gedaempfte Feder fuer den Bogen (Kopie aus loudr): folgt weich, schwingt nie ueber
class Needle(private val omega: Float = 7f) {
    var value = Float.NaN
        private set
    private var velocity = 0f
    private var target = Float.NaN

    // ruht: kein weiteres Bild noetig
    val settled: Boolean
        get() = value.isNaN() || target.isNaN() || (abs(value - target) < EPS && abs(velocity) < EPS)

    fun snap(t: Float): Float {
        value = t
        target = t
        velocity = 0f
        return value
    }

    // geschlossene Loesung, stabil fuer jedes dt
    fun step(t: Float, dtSeconds: Float): Float {
        if (t.isNaN() || value.isNaN()) return snap(t)
        target = t
        val dt = dtSeconds.coerceIn(0f, 0.1f)
        val x = value - t
        val e = exp(-omega * dt)
        val k = velocity + omega * x
        value = t + (x + k * dt) * e
        velocity = (velocity - omega * k * dt) * e
        if (abs(value - t) < EPS && abs(velocity) < EPS) snap(t)
        return value
    }

    private companion object {
        // in Anzeige-Einheiten: ein Hundertstel km/h sieht niemand
        const val EPS = 1e-2f
    }
}

// Warnschwelle: 5er-Schritte, beim Einheitenwechsel umgerechnet
object Limit {
    const val MIN = 10
    const val MAX = 300
    const val STEP = 5

    fun clamp(v: Int): Int = ((v.toFloat() / STEP).roundToInt() * STEP).coerceIn(MIN, MAX)

    fun step(v: Int, dir: Int): Int = clamp(v + dir * STEP)

    fun convert(v: Int, from: SpeedUnit, to: SpeedUnit): Int = clamp(to.fromMps(from.toMps(v.toFloat())).roundToInt())

    fun default(unit: SpeedUnit): Int = when (unit) {
        SpeedUnit.KMH -> 50
        SpeedUnit.MPH -> 30
        SpeedUnit.KN -> 25
    }
}
