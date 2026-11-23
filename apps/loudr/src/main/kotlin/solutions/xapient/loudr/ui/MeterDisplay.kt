package solutions.xapient.loudr.ui

import solutions.xapient.loudr.domain.Hold
import solutions.xapient.loudr.domain.Needle
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.domain.TimeWeighting

// Stand je Bild, geteilt von Bogen, Werten und Verlauf; nur aus der Zeichenphase aufgerufen
class MeterDisplay(private val source: () -> Reading) {
    private val needle = Needle()
    private val hold = Hold<Reading>(HOLD_NS)
    private var lastFrame = 0L
    private var lastVersion = -1
    private var lastLive = false
    private var lastWeighting: TimeWeighting? = null

    // neuester Stand (Verlauf), gehaltener Stand (Ziffern), Bogen (Feder, ohne Kalibrierung)
    var raw: Reading = Reading.EMPTY
        private set
    var shown: Reading = Reading.EMPTY
        private set
    var arc: Float = Float.NaN
        private set

    fun at(frameNanos: Long, version: Int, w: TimeWeighting, live: Boolean, animate: Boolean) {
        val same = version == lastVersion && live == lastLive && w == lastWeighting
        if (same && frameNanos == lastFrame) return
        val dt = if (same && lastFrame != 0L) (frameNanos - lastFrame) / 1e9f else 0f
        lastFrame = frameNanos
        lastVersion = version
        lastLive = live
        lastWeighting = w
        raw = source()
        val target = raw.level(w)
        val smooth = live && animate && same
        arc = if (smooth) needle.step(target, dt) else needle.snap(target)
        shown = hold.update(frameNanos, raw, force = !smooth)
    }

    private companion object {
        const val HOLD_NS = 250_000_000L
    }
}
