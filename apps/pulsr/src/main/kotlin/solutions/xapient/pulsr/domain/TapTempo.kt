package solutions.xapient.pulsr.domain

import kotlin.math.abs
import kotlin.math.roundToInt

// Tempo aus Tipp-Abstaenden: Mittelwert der letzten Abstaende ohne Ausreisser, nach einer Pause neu
class TapTempo(
    private val resetMs: Long = RESET_MS,
    private val maxIntervals: Int = 6,
    private val tolerance: Double = 0.35,
) {
    private val taps = ArrayDeque<Long>()

    // Zeit in ms (monoton); liefert das Tempo ab dem zweiten Tipp
    fun tap(ms: Long): Int? {
        val last = taps.lastOrNull()
        if (last != null && (ms - last > resetMs || ms <= last)) taps.clear()
        taps.addLast(ms)
        while (taps.size > maxIntervals + 1) taps.removeFirst()
        if (taps.size < 2) return null
        val intervals = taps.zipWithNext { a, b -> (b - a).toDouble() }
        val sorted = intervals.sorted()
        val mid = sorted.size / 2
        val median = if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
        val kept = intervals.filter { abs(it - median) <= median * tolerance }
        val avg = if (kept.isEmpty()) {
            // Tempo deutlich gewechselt: neu ab dem letzten Abstand
            while (taps.size > 2) taps.removeFirst()
            intervals.last()
        } else {
            kept.average()
        }
        return Tempo.clamp((60_000.0 / avg).roundToInt())
    }

    fun reset() = taps.clear()

    companion object {
        // laenger als ein Schlag bei 20 BPM, sonst waere das Minimum nicht tappbar
        const val RESET_MS = 3_200L
    }
}
