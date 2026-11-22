package com.valueerrorx.nordr.domain

import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    val length: Float get() = sqrt(x * x + y * y + z * z)
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(f: Float) = Vec3(x * f, y * f, z * f)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

// Winkel in Grad auf dem Kreis
object Angles {
    // 0 <= Ergebnis < 360
    fun normalize(deg: Float): Float {
        val r = deg % 360f
        val n = if (r < 0f) r + 360f else r
        // + 0f macht aus -0 eine 0
        return if (n >= 360f) 0f else n + 0f
    }

    // Kuerzeste Drehung von a nach b, -180 < Ergebnis <= 180
    fun delta(from: Float, to: Float): Float {
        var d = (to - from) % 360f
        if (d > 180f) d -= 360f
        if (d <= -180f) d += 360f
        return d
    }

    // Fortlaufender Winkel ohne Sprung bei 0/360, fuer Feder und Zeichnen
    fun unwrap(previous: Float, deg: Float): Float = previous + delta(previous, deg)

    // Anzeige in ganzen Grad, 360 wird zu 0
    fun whole(deg: Float): Int = normalize(deg).roundToInt() % 360

    // Index der 16-teiligen Windrose (0 = N, 4 = O, 8 = S, 12 = W)
    fun point16(deg: Float): Int = (normalize(deg) / 22.5f).roundToInt() % 16
}
