package solutions.xapient.levlr.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan

data class Vec3(val x: Float, val y: Float, val z: Float) {
    val length: Float get() = sqrt(x * x + y * y + z * z)
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(f: Float) = Vec3(x * f, y * f, z * f)

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

enum class LevelMode { FLAT, EDGE }

enum class AngleUnit { DEGREE, PERCENT, MM_PER_M }

private const val RAD_TO_DEG = (180.0 / PI).toFloat()

// Neigungsrechnung; "oben" ist der Messwert des Sensors (zeigt im Ruhezustand nach oben)
object TiltMath {

    // Geraete- auf Bildschirmkoordinaten drehen (quarterTurns = Display-Rotation 0..3)
    fun toScreen(v: Vec3, quarterTurns: Int): Vec3 = when (quarterTurns.mod(4)) {
        1 -> Vec3(-v.y, v.x, v.z)
        2 -> Vec3(-v.x, -v.y, v.z)
        3 -> Vec3(v.y, -v.x, v.z)
        else -> v
    }

    // Kante: Drehung in der Bildschirmebene, Grad im Uhrzeigersinn (rechts tiefer = positiv)
    fun edgeRaw(up: Vec3): Float = atan2(-up.x, up.y) * RAD_TO_DEG

    // Welche Bildschirmkante unten liegt (0 = untere, 1 = rechte, 2 = obere, 3 = linke)
    fun edgeQuadrant(raw: Float): Int = (raw / 90f).roundToInt().mod(4)

    // Wie oben, aber die bisherige Kante bleibt bis 45° + Hysterese
    fun edgeQuadrant(raw: Float, previous: Int, hysteresis: Float = 5f): Int =
        if (abs(edgeAngle(raw, previous)) <= 45f + hysteresis) previous else edgeQuadrant(raw)

    // Abweichung von der naechsten Kante, -45..45
    fun edgeAngle(raw: Float): Float = raw - 90f * (raw / 90f).roundToInt()

    // Abweichung von einer bestimmten Kante, -180..180
    fun edgeAngle(raw: Float, quadrant: Int): Float {
        var d = (raw - quadrant * 90f) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    // Flach: Neigung um die Y-Achse (rechts hoeher = positiv) und um die X-Achse (oben hoeher = positiv)
    fun flatX(up: Vec3): Float = atan2(up.x, sqrt(up.y * up.y + up.z * up.z)) * RAD_TO_DEG
    fun flatY(up: Vec3): Float = atan2(up.y, sqrt(up.x * up.x + up.z * up.z)) * RAD_TO_DEG

    // Gesamtneigung gegen die Waagrechte, 0..180
    fun inclination(up: Vec3): Float = atan2(sqrt(up.x * up.x + up.y * up.y), up.z) * RAD_TO_DEG

    fun convert(angleDeg: Float, unit: AngleUnit): Float = when (unit) {
        AngleUnit.DEGREE -> angleDeg
        AngleUnit.PERCENT -> tan(angleDeg / RAD_TO_DEG) * 100f
        AngleUnit.MM_PER_M -> tan(angleDeg / RAD_TO_DEG) * 1000f
    }
}
