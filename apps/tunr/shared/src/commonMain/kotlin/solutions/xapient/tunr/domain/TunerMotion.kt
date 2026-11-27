package solutions.xapient.tunr.domain

import kotlin.math.abs
import kotlin.math.tanh

// Bewegung der Anzeige: Zeiger, Praesenz, Einrasten, Strobe-Drehung und Glanz; schlaeft ein, wenn alles ruht
class TunerMotion {
    val needle = SpringValue(0f, stiffness = 150f, dampingRatio = 0.72f)
    val presence = SpringValue(0f, stiffness = 60f, dampingRatio = 1f)
    val lock = SpringValue(0f, stiffness = 90f, dampingRatio = 0.85f)

    // Strobe-Winkel in Grad, 0..360
    var strobe = 0f
        private set
    private var strobeSpeed = 0f

    // Glanz nach dem Einrasten, 0..1 (>= 1 = vorbei)
    var shine = 1f
        private set
    private var seenLocks = -1

    fun setTarget(r: Reading) {
        if (r.active) needle.target = r.cents.coerceIn(-NEEDLE_LIMIT, NEEDLE_LIMIT)
        presence.target = if (r.active) 1f else 0f
        lock.target = if (r.inTune) 1f else 0f
        strobeSpeed = if (r.active) strobeSpeedOf(r.cents) else 0f
        if (seenLocks >= 0 && r.locks > seenLocks) shine = 0f
        seenLocks = r.locks
    }

    // Standbild, z. B. fuer Screenshots
    fun pose(r: Reading, strobeDeg: Float = 0f, shine: Float = 1f) {
        setTarget(r)
        needle.snapTo(needle.target)
        presence.snapTo(presence.target)
        lock.snapTo(lock.target)
        strobe = strobeDeg
        this.shine = shine
    }

    fun step(dt: Float) {
        needle.step(dt)
        presence.step(dt)
        lock.step(dt)
        if (strobeSpeed != 0f) strobe = ((strobe + strobeSpeed * dt.coerceAtMost(MAX_DT)) % 360f + 360f) % 360f
        if (shine < 1f) shine = (shine + dt / SHINE_S).coerceAtMost(1f)
    }

    // Strobe unter 0,3 Cent gilt als stillstehend
    val isSettled: Boolean
        get() = needle.isSettled(0.05f, 0.05f) && presence.isSettled() && lock.isSettled() &&
            abs(strobeSpeed) < 0.3f * DEG_PER_CENT_S && shine >= 1f

    companion object {
        const val NEEDLE_LIMIT = 55f
        const val DEG_PER_CENT_S = 6f
        const val SHINE_S = 0.9f
        // Strichzahl der Strobe-Ringe; innen dreht halb so schnell
        const val STROBE_OUTER = 64
        const val STROBE_INNER = 24
        const val INNER_RATIO = 0.5f
        // hoechstens 0,36 Strichabstand je Bild bei 60 Hz, sonst dreht der Ring scheinbar rueckwaerts
        const val MAX_STROBE_DEG_S = 120f
        // laengere Bilder (Ruckler, 30 Hz) drehen langsamer statt zu springen
        const val MAX_DT = 1f / 60f

        // nahe 0 linear (6 Grad/s je Cent), gross sanft gestaucht; Richtung bleibt
        fun strobeSpeedOf(cents: Float): Float = MAX_STROBE_DEG_S * tanh(cents * DEG_PER_CENT_S / MAX_STROBE_DEG_S)
    }
}
