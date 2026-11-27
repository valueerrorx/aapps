package solutions.xapient.loudr.domain

import kotlin.math.exp

// Kritisch gedaempfte Feder fuer den Pegelbogen: folgt flott, schwingt nie ueber
class Needle(private val omega: Float = 20f) {
    var value = Float.NaN
        private set
    private var velocity = 0f

    fun snap(target: Float): Float {
        value = target
        velocity = 0f
        return value
    }

    // geschlossene Loesung, stabil fuer jedes dt
    fun step(target: Float, dtSeconds: Float): Float {
        if (target.isNaN() || value.isNaN()) return snap(target)
        val t = dtSeconds.coerceIn(0f, 0.1f)
        val x = value - target
        val e = exp(-omega * t)
        val k = velocity + omega * x
        value = target + (x + k * t) * e
        velocity = (velocity - omega * k * t) * e
        return value
    }
}

// Haelt einen Wert fest, damit Ziffern lesbar bleiben
class Hold<T>(private val holdNanos: Long) {
    private var at = 0L
    private var has = false
    var value: T? = null
        private set

    fun update(nowNanos: Long, v: T, force: Boolean = false): T {
        if (force || !has || nowNanos - at >= holdNanos) {
            value = v
            at = nowNanos
            has = true
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}
