package solutions.xapient.thinkr.domain.stars

import kotlin.math.hypot
import kotlin.random.Random

// Raster side × side, Zellen zeilenweise 0 … side²-1
object Sky {
    // n verschiedene Zellen, aufsteigend sortiert
    fun pattern(rng: Random, side: Int, n: Int): List<Int> {
        require(n in 1..side * side)
        return (0 until side * side).shuffled(rng).take(n).sorted()
    }

    // Sternbild-Linien: kuerzester Baum ueber die Sterne (Prim), deterministisch; in Wachstums-Reihenfolge
    fun links(cells: List<Int>, side: Int): List<Pair<Int, Int>> {
        if (cells.size < 2) return emptyList()
        val inTree = mutableListOf(cells.first())
        val rest = cells.drop(1).toMutableList()
        val out = ArrayList<Pair<Int, Int>>(cells.size - 1)
        while (rest.isNotEmpty()) {
            var best: Pair<Int, Int>? = null
            var bestD = Double.MAX_VALUE
            for (a in inTree) for (b in rest) {
                val d = dist(a, b, side)
                if (d < bestD - 1e-9) { bestD = d; best = a to b }
            }
            val e = best!!
            out += e
            inTree += e.second
            rest -= e.second
        }
        return out
    }

    fun dist(a: Int, b: Int, side: Int): Double = hypot((a % side - b % side).toDouble(), (a / side - b / side).toDouble())
}
