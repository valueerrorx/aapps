package com.valueerrorx.spinr.domain

// Farbindex je Segment: reihum, aber Nachbarn nie gleich, auch ueber den Kreis
object Palette {
    fun colorIndex(i: Int, n: Int, k: Int): Int {
        require(k >= 3 && i in 0 until n)
        val c = i % k
        // letztes Segment liegt neben dem ersten: bei Gleichheit eine Farbe ausweichen
        return if (n > 1 && i == n - 1 && c == 0) 1 else c
    }
}
