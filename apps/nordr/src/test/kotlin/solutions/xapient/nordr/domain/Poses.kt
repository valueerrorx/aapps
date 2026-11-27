package solutions.xapient.nordr.domain

import kotlin.math.cos
import kotlin.math.sin

// Testhilfen (Kopie von shared/commonTest fuer den Android-Sensortest): Geraetelagen als Rotationsmatrix (Spalten = Geraeteachsen in Welt Ost/Nord/Oben)
object Poses {
    private fun rad(deg: Float) = Math.toRadians(deg.toDouble()).toFloat()

    fun fromAxes(x: Vec3, y: Vec3, z: Vec3): FloatArray = floatArrayOf(
        x.x, y.x, z.x,
        x.y, y.y, z.y,
        x.z, y.z, z.z,
    )

    private fun forward(h: Float) = Vec3(sin(rad(h)), cos(rad(h)), 0f)
    private fun right(h: Float) = Vec3(cos(rad(h)), -sin(rad(h)), 0f)
    private val UP = Vec3(0f, 0f, 1f)

    // Flach, Oberkante zeigt nach h, um pitch Grad aufgerichtet (90 = aufrecht), dann um roll Grad
    // um die waagrechte Blickachse gekippt (positiv = rechte Kante nach unten)
    fun pose(heading: Float, pitch: Float = 0f, roll: Float = 0f): FloatArray {
        val p = rad(pitch)
        val f = forward(heading)
        val y = f * cos(p) + UP * sin(p)
        val z = f * -sin(p) + UP * cos(p)
        val rl = rad(roll)
        // Rodrigues um f
        fun turn(v: Vec3) = v * cos(rl) + (f cross v) * sin(rl) + f * ((f dot v) * (1f - cos(rl)))
        return fromAxes(turn(right(heading)), turn(y), turn(z))
    }

    // Welt -> Geraet: d = R^T w
    fun toDevice(r: FloatArray, w: Vec3) = Vec3(
        r[0] * w.x + r[3] * w.y + r[6] * w.z,
        r[1] * w.x + r[4] * w.y + r[7] * w.z,
        r[2] * w.x + r[5] * w.y + r[8] * w.z,
    )

    // Erdfeld wie in Mitteleuropa: 20 µT nach Norden, 44 µT nach unten
    val EARTH = Vec3(0f, 20f, -44f)

    fun gravity(r: FloatArray) = toDevice(r, Vec3(0f, 0f, 9.81f))
    fun magnetic(r: FloatArray) = toDevice(r, EARTH)
}
