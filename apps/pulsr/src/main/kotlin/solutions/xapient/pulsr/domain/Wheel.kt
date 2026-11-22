package solutions.xapient.pulsr.domain

import kotlin.math.atan2
import kotlin.math.truncate

// Drehen am Rad: Winkel in Bildschirm-Koordinaten (y nach unten), im Uhrzeigersinn positiv
object Wheel {
    const val DEG_PER_BPM = 6f

    fun angle(x: Float, y: Float, cx: Float, cy: Float): Float =
        Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat()

    // kuerzester Weg, -180..180
    fun delta(from: Float, to: Float): Float {
        var d = (to - from) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }
}

// Sammelt Drehwinkel und gibt ganze Schritte heraus; Rest bleibt fuer die naechste Bewegung
class WheelSteps(private val degPerStep: Float = Wheel.DEG_PER_BPM) {
    private var acc = 0f

    fun add(deg: Float): Int {
        acc += deg
        val steps = truncate(acc / degPerStep).toInt()
        acc -= steps * degPerStep
        return steps
    }

    fun reset() {
        acc = 0f
    }
}
