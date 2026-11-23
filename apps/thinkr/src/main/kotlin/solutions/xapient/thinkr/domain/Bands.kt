package solutions.xapient.thinkr.domain

import kotlin.math.min
import kotlin.math.pow

// Frist-Baender: Spielarten beginnen bei starts (erste = 1), je Stufe Frist mal step; die Stufe folgt der aktuellen Frist
class Bands(private val starts: List<Int>, private val bases: List<Double>, val maxLevel: Int, private val step: Double) {
    init {
        require(starts.first() == 1 && starts.size == bases.size && starts.zipWithNext().all { (a, b) -> a < b })
    }

    fun modeStart(level: Int): Int = starts.last { it <= level.coerceIn(1, maxLevel) }

    fun modeEnd(start: Int): Int {
        val i = starts.indexOf(start)
        return if (i + 1 < starts.size) starts[i + 1] - 1 else maxLevel
    }

    // Frist zu Beginn einer Stufe (Band-Obergrenze) innerhalb der Spielart ab start
    fun deadline(start: Int, level: Int): Double = bases[starts.indexOf(start)] * step.pow(level - start)

    // Stufe zur aktuellen Frist; hoechstens eine ueber das Ende bzw. eine unter den Beginn der Spielart
    fun levelOf(start: Int, ms: Double): Int {
        val top = min(modeEnd(start) + 1, maxLevel)
        var level = (start - 1).coerceAtLeast(1)
        for (x in start..top) if (deadline(start, x) >= ms - 1e-6) level = x
        return level
    }

    fun stairFor(start: Int, level: Int): Staircase {
        val top = min(modeEnd(start) + 1, maxLevel)
        return Staircase(deadline(start, level), min = deadline(start, top) * 0.9, max = deadline(start, start) * 1.3)
    }

    // Aufwaermen: zwei Stufen tiefer, aber nie unter den Beginn der Spielart
    fun warmUp(saved: Int): Int {
        val s = saved.coerceIn(1, maxLevel)
        return maxOf(modeStart(s), s - 2)
    }
}
