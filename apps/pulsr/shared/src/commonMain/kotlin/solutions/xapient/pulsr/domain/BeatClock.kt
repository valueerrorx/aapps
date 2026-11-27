package solutions.xapient.pulsr.domain

import kotlin.math.floor

// Zustand des Pulses an einem hoerbaren Frame; daraus zeichnet die Oberflaeche
data class Pulse(
    val beat: Int,
    val beats: Int,
    // Fortschritt im Schlag 0..1
    val phase: Float,
    val sub: Int,
    val subIndex: Int,
    val accent: Boolean,
    val sinceBeatMs: Float,
    val sinceSubMs: Float,
    val beatMs: Float,
) {
    // Fortschritt im Takt 0..1
    val barPhase: Float get() = (beat + phase) / beats
}

// Merkt sich pro Block den Scheduler-Stand (Audio-Thread schreibt, Oberflaeche liest).
// Die Oberflaeche fragt mit dem gerade hoerbaren Frame, nicht mit der Wanduhr: Bild und Ton bleiben synchron.
class BeatClock(val rate: Int, private val capacity: Int = 256) {
    private val frame = LongArray(capacity)
    private val pos = DoubleArray(capacity)
    private val beat = IntArray(capacity)
    private val beats = IntArray(capacity)
    private val sub = IntArray(capacity)
    private val step = DoubleArray(capacity)
    private val accent = BooleanArray(capacity)
    private var count = 0
    private var head = 0
    private val lock = Lock()

    fun record(f: Long, p: Double, b: Int, n: Int, s: Int, d: Double, a: Boolean) = lock.locked { put(f, p, b, n, s, d, a) }

    private fun put(f: Long, p: Double, b: Int, n: Int, s: Int, d: Double, a: Boolean) {
        frame[head] = f; pos[head] = p; beat[head] = b; beats[head] = n; sub[head] = s; step[head] = d; accent[head] = a
        head = (head + 1) % capacity
        if (count < capacity) count++
    }

    fun clear() = lock.locked {
        count = 0
        head = 0
    }

    // null = noch nichts hoerbar
    fun at(f: Long): Pulse? = lock.locked { find(f) }

    private fun find(f: Long): Pulse? {
        if (count == 0 || f < 0) return null
        // juengsten Stand suchen, der nicht nach f liegt; aelter als der Puffer = aeltesten nehmen
        var i = (head - 1 + capacity) % capacity
        var left = count
        while (left > 1 && frame[i] > f) {
            i = (i - 1 + capacity) % capacity
            left--
        }
        if (frame[i] > f) return null
        val p = pos[i] + (f - frame[i]) * step[i]
        val whole = floor(p)
        val n = beats[i]
        val b = ((beat[i] + whole.toLong()) % n).toInt()
        val phase = p - whole
        val framesPerBeat = 1.0 / step[i]
        val msPerFrame = 1000.0 / rate
        val subPos = phase * sub[i]
        val si = floor(subPos)
        return Pulse(
            beat = b,
            beats = n,
            phase = phase.toFloat(),
            sub = sub[i],
            subIndex = si.toInt(),
            accent = accent[i] && b == 0,
            sinceBeatMs = (phase * framesPerBeat * msPerFrame).toFloat(),
            sinceSubMs = ((subPos - si) / sub[i] * framesPerBeat * msPerFrame).toFloat(),
            beatMs = (framesPerBeat * msPerFrame).toFloat(),
        )
    }

    companion object {
        // hoerbarer Frame jetzt: letzter Zeitstempel der Ausgabe plus verstrichene Zeit
        fun presented(stampFrame: Long, stampNanos: Long, nowNanos: Long, rate: Int): Long =
            stampFrame + (nowNanos - stampNanos) * rate / 1_000_000_000L
    }
}
