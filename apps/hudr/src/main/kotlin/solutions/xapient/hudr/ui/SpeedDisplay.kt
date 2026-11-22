package solutions.xapient.hudr.ui

import solutions.xapient.hudr.domain.Needle
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.domain.Ticks

// Stand je Bild, geteilt von Bogen, Werten und HUD; aus Bildschleife und Zeichenphase, je Bild nur einmal gerechnet
class SpeedDisplay(private val source: () -> Reading) {
    private val needle = Needle()
    private var lastFrame = 0L
    private var lastVersion = -1

    var reading: Reading? = null
        private set

    // Bogen als Anteil 0..1 (Feder); NaN ohne Wert
    var arc: Float = Float.NaN
        private set

    // Feder ruht: kein weiteres Bild noetig
    val settled: Boolean get() = needle.settled

    fun at(frameNanos: Long, version: Int, animate: Boolean) {
        if (version == lastVersion && frameNanos == lastFrame) return
        val fresh = version != lastVersion
        // nach Ruhe kein Sprung: hoechstens ein Bild Zeitschritt
        val dt = if (lastFrame != 0L && frameNanos > lastFrame) ((frameNanos - lastFrame) / 1e9f).coerceAtMost(MAX_DT) else 0f
        lastFrame = frameNanos
        lastVersion = version
        if (fresh || reading == null) reading = source()
        val r = reading!!
        val target = if (r.speed.isNaN()) Float.NaN else Ticks.fraction(r.speed, r.scale)
        arc = when {
            target.isNaN() -> needle.value.let { if (it.isNaN()) 0f else it }.also { needle.snap(it) }
            animate && !needle.value.isNaN() -> needle.step(target, dt)
            else -> needle.snap(target)
        }
    }

    private companion object {
        const val MAX_DT = 0.034f
    }
}
