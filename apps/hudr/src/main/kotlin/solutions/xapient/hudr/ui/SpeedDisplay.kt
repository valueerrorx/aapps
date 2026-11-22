package solutions.xapient.hudr.ui

import solutions.xapient.hudr.domain.Needle
import solutions.xapient.hudr.domain.Reading

// Stand je Bild, geteilt von Ziffern, Band und Werten; aus Bildschleife und Zeichenphase, je Bild nur einmal gerechnet
class SpeedDisplay(private val source: () -> Reading) {
    private val needle = Needle()
    private var lastFrame = 0L
    private var lastVersion = -1

    var reading: Reading? = null
        private set

    // Mitte des Bands in Anzeige-Einheiten (Feder); ohne Wert gleitet es auf 0
    var tape: Float = 0f
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
        val target = if (r.speed.isNaN()) 0f else r.speed
        tape = if (animate && !needle.value.isNaN()) needle.step(target, dt) else needle.snap(target)
    }

    private companion object {
        const val MAX_DT = 0.034f
    }
}
