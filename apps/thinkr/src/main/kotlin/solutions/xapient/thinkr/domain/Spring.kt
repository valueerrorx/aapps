package solutions.xapient.thinkr.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

// Feder als reine Zeitfunktion: haengt nur an der Spielzeit, steht also in der Pause mit
object Spring {
    // Sprung 0 -> 1 mit leichtem Ueberschwinger (Daempfung 0,5, ca. 2,2 Hz)
    fun step(ms: Double, hz: Double = 2.2, damping: Double = 0.5): Double {
        if (ms <= 0.0) return 0.0
        val w = 2 * PI * hz
        val t = ms / 1000.0
        val wd = w * sqrt(1 - damping * damping)
        val x = 1 - exp(-damping * w * t) * (cos(wd * t) + damping * w / wd * sin(wd * t))
        return if (t > 6.0 / (damping * w)) 1.0 else x
    }

    // Stoss, der um 0 ausschwingt (Wackeln, Plopp); Startausschlag 0, Spitze nahe 1
    fun kick(ms: Double, hz: Double = 6.0, decayMs: Double = 160.0): Double {
        if (ms <= 0.0 || ms > decayMs * 6) return 0.0
        return sin(2 * PI * hz * ms / 1000.0) * exp(-ms / decayMs)
    }
}
