package solutions.xapient.thinkr.domain.switches

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tan

// Punkt im Netz-Raster (1 = ein Rasterfeld)
data class Pt(val x: Double, val y: Double) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(k: Double) = Pt(x * k, y * k)
    val length: Double get() = hypot(x, y)
    fun unit(): Pt = if (length == 0.0) this else this * (1.0 / length)
}

// Schiene: Polylinie mit abgerundeten Ecken, Position nach Weglaenge
class Track(val points: List<Pt>, corner: Double = CORNER) {
    private sealed interface Piece {
        val length: Double
        fun at(s: Double): Pt
    }

    private class Line(val a: Pt, val b: Pt) : Piece {
        override val length = (b - a).length
        override fun at(s: Double): Pt = if (length == 0.0) a else a + (b - a) * (s / length)
    }

    private class Arc(val c: Pt, val r: Double, val a0: Double, val sweep: Double) : Piece {
        override val length = r * abs(sweep)
        override fun at(s: Double): Pt {
            val a = a0 + sweep * (s / length)
            return Pt(c.x + r * cos(a), c.y + r * sin(a))
        }
    }

    private val pieces: List<Piece>
    private val starts: DoubleArray
    val length: Double

    init {
        require(points.size >= 2)
        val out = mutableListOf<Piece>()
        var cur = points.first()
        for (i in 1 until points.lastIndex) {
            val a = points[i - 1]
            val b = points[i]
            val c = points[i + 1]
            val u = (b - a).unit()
            val v = (c - b).unit()
            val turn = acos((u.x * v.x + u.y * v.y).coerceIn(-1.0, 1.0))
            if (turn < 1e-6) continue
            // Bogen nie laenger als die halbe Nachbarstrecke
            val d = min(corner * tan(turn / 2), min((b - a).length, (c - b).length) / 2)
            val r = d / tan(turn / 2)
            val t1 = b - u * d
            val cross = u.x * v.y - u.y * v.x
            val n = if (cross > 0) Pt(-u.y, u.x) else Pt(u.y, -u.x)
            val center = t1 + n * r
            if ((t1 - cur).length > 1e-9) out += Line(cur, t1)
            out += Arc(center, r, atan2(t1.y - center.y, t1.x - center.x), sign(cross) * turn)
            cur = b + v * d
        }
        out += Line(cur, points.last())
        pieces = out
        starts = DoubleArray(out.size)
        var acc = 0.0
        out.forEachIndexed { i, p -> starts[i] = acc; acc += p.length }
        length = acc
    }

    // Punkt nach Weglaenge s (geklemmt auf die Schiene)
    fun at(s: Double): Pt {
        val x = s.coerceIn(0.0, length)
        var i = pieces.lastIndex
        while (i > 0 && starts[i] > x) i--
        return pieces[i].at(x - starts[i])
    }

    // Punkte im Abstand step fuer das Zeichnen
    fun sample(step: Double): List<Pt> {
        val n = (length / step).toInt().coerceAtLeast(1)
        return List(n + 1) { at(length * it / n) }
    }

    // Abgangsrichtung am Anfang (fuer den Weichenpfeil)
    val heading: Pt get() = (points[1] - points[0]).unit()

    companion object {
        const val CORNER = 1.0
    }
}
