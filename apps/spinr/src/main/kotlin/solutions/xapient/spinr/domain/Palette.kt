package solutions.xapient.spinr.domain

// Farbindex je Segment: reihum, aber Nachbarn nie gleich, auch ueber den Kreis
object Palette {
    fun colorIndex(i: Int, n: Int, k: Int): Int {
        require(k >= 3 && i in 0 until n)
        val c = i % k
        // letztes Segment liegt neben dem ersten: bei Gleichheit eine Farbe ausweichen
        return if (n > 1 && i == n - 1 && c == 0) 1 else c
    }

    // Farben der verbliebenen Eintraege (ids im vollen Rad): jeder behaelt seine Farbe, nur gleiche Nachbarn weichen aus
    fun colors(ids: List<Int>, total: Int, k: Int): List<Int> {
        val base = ids.map { colorIndex(it, total, k) }
        val out = base.toMutableList()
        val m = out.size
        for (j in 1 until m) {
            val next = if (j == m - 1) out[0] else base[j + 1]
            if (out[j] == out[j - 1] || (j == m - 1 && out[j] == out[0])) {
                out[j] = (0 until k).first { it != out[j - 1] && it != next }
            }
        }
        return out
    }
}
