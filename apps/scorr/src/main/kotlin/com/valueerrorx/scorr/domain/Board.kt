package com.valueerrorx.scorr.domain

import kotlin.math.abs

// Raster der Kacheln: Spalten x Zeilen
data class Grid(val cols: Int, val rows: Int)

object Board {
    fun grid(n: Int, landscape: Boolean): Grid {
        val k = n.coerceIn(1, Game.MAX_PLAYERS)
        return if (landscape) {
            if (k <= 4) Grid(k, 1) else Grid((k + 1) / 2, 2)
        } else {
            if (k <= 3) Grid(1, k) else Grid(2, (k + 1) / 2)
        }
    }

    // Ziehen: ganze Schritte aus dem Weg, nach oben (negatives dy) ist plus
    fun steps(dy: Float, stepPx: Float): Int = if (stepPx <= 0f) 0 else (-dy / stepPx).toInt()

    // Punkte mit echtem Minuszeichen
    fun format(v: Int): String = if (v < 0) "−${abs(v.toLong())}" else "$v"

    // Aenderung immer mit Vorzeichen
    fun formatDelta(v: Int): String = when {
        v > 0 -> "+$v"
        v < 0 -> "−${abs(v.toLong())}"
        else -> "0"
    }

    // Eingabe aus dem Zahlenfeld: nur Ziffern, Betrag begrenzt
    fun parseAmount(text: String): Int? {
        val digits = text.filter { it.isDigit() }.trimStart('0')
        if (digits.isEmpty()) return if (text.any { it.isDigit() }) 0 else null
        return if (digits.length > 6) Game.MAX else digits.toInt().coerceAtMost(Game.MAX)
    }
}
