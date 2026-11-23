package solutions.xapient.spinr.domain

import kotlin.math.abs

// Zahlen von..bis als Eintraege, auch absteigend
object NumberRange {
    const val MAX_DIGITS = 6

    // null, wenn weniger als 2 oder mehr als 50 Zahlen herauskommen
    fun entries(from: Int, to: Int): List<String>? {
        val count = abs(to.toLong() - from) + 1
        if (count < Wheel.MIN_ENTRIES || count > Wheel.MAX_ENTRIES) return null
        val step = if (to >= from) 1 else -1
        return IntProgression.fromClosedRange(from, to, step).map { it.toString() }
    }

    // Texteingaben aus dem Formular; ungueltig oder leer -> null
    fun parse(from: String, to: String): List<String>? {
        val a = from.trim().toIntOrNull() ?: return null
        val b = to.trim().toIntOrNull() ?: return null
        return entries(a, b)
    }

    // nur Ziffern und ein fuehrendes Minus, begrenzt
    fun cleanInput(text: String): String {
        val minus = text.trimStart().startsWith("-")
        val digits = text.filter { it.isDigit() }.take(MAX_DIGITS)
        return if (minus) "-$digits" else digits
    }
}
