package solutions.xapient.nordr.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sqrt

private const val RAD_TO_DEG = (180.0 / PI).toFloat()

// Rotationsmatrix wie bei Android (3x3, zeilenweise): Zeilen = Ost, Nord, Oben in Geraetekoordinaten
object Orientation {

    // Wie SensorManager.getRotationMatrixFromVector; v = x, y, z[, w]
    fun fromRotationVector(v: FloatArray): FloatArray {
        val q1 = v[0]; val q2 = v[1]; val q3 = v[2]
        val q0 = if (v.size >= 4) v[3] else sqrt((1f - q1 * q1 - q2 * q2 - q3 * q3).coerceAtLeast(0f))
        val sq1 = 2f * q1 * q1; val sq2 = 2f * q2 * q2; val sq3 = 2f * q3 * q3
        val q12 = 2f * q1 * q2; val q30 = 2f * q3 * q0; val q13 = 2f * q1 * q3
        val q20 = 2f * q2 * q0; val q23 = 2f * q2 * q3; val q10 = 2f * q1 * q0
        return floatArrayOf(
            1f - sq2 - sq3, q12 - q30, q13 + q20,
            q12 + q30, 1f - sq1 - sq3, q23 - q10,
            q13 - q20, q23 + q10, 1f - sq1 - sq2,
        )
    }

    // Wie SensorManager.getRotationMatrix; null, wenn Magnetfeld und Schwerkraft fast parallel sind
    fun fromGravityMagnetic(gravity: Vec3, magnetic: Vec3): FloatArray? {
        val h = magnetic cross gravity
        val normH = h.length
        val normA = gravity.length
        if (normH < 0.1f || normA < 0.1f) return null
        val e = h * (1f / normH)
        val a = gravity * (1f / normA)
        val n = a cross e
        return floatArrayOf(e.x, e.y, e.z, n.x, n.y, n.z, a.x, a.y, a.z)
    }

    // Bildschirm-oben in Geraetekoordinaten je Display-Rotation (0..3)
    fun screenUp(quarterTurns: Int): Vec3 = when (Math.floorMod(quarterTurns, 4)) {
        1 -> Vec3(1f, 0f, 0f)
        2 -> Vec3(0f, -1f, 0f)
        3 -> Vec3(-1f, 0f, 0f)
        else -> Vec3(0f, 1f, 0f)
    }

    // Geraete- in Bildschirmkoordinaten (x rechts, y oben, z aus dem Bildschirm)
    fun toScreen(v: Vec3, quarterTurns: Int): Vec3 = when (Math.floorMod(quarterTurns, 4)) {
        1 -> Vec3(-v.y, v.x, v.z)
        2 -> Vec3(-v.x, -v.y, v.z)
        3 -> Vec3(v.y, -v.x, v.z)
        else -> v
    }

    // Bildschirm-rechts in Geraetekoordinaten je Display-Rotation (0..3)
    fun screenRight(quarterTurns: Int): Vec3 = screenUp(quarterTurns).let { Vec3(it.y, -it.x, 0f) }

    // Blickrichtung = Bildschirm-rechts waagrecht um 90° nach links gedreht: flach die Oberkante,
    // aufrecht die Rueckseite; Nicken und seitliches Kippen aendern sie nicht. Grad 0..360, NaN wenn rechts senkrecht steht
    fun heading(r: FloatArray, quarterTurns: Int): Float {
        val s = screenRight(quarterTurns)
        val e = r[0] * s.x + r[1] * s.y + r[2] * s.z
        val n = r[3] * s.x + r[4] * s.y + r[5] * s.z
        if (hypot(e, n) < MIN_HORIZONTAL) return Float.NaN
        // vorne = (-n, e), wenn rechts = (e, n) in Ost/Nord
        return Angles.normalize(atan2(-n, e) * RAD_TO_DEG)
    }

    // unter ~84° seitlicher Kippung ist rechts zu steil fuer eine ruhige Richtung
    private const val MIN_HORIZONTAL = 0.1f

    // Welt-oben in Bildschirmkoordinaten (Einheitsvektor), fuer die Glas-Spiegelung
    fun upOnScreen(r: FloatArray, quarterTurns: Int): Vec3 = toScreen(Vec3(r[6], r[7], r[8]), quarterTurns)
}

// Fallback ohne Rotation-Vector: Schwerkraft aus dem Beschleunigungssensor tiefpassen, dann mit dem Magnetfeld kombinieren
class GravityMagFusion(private val tau: Float = 0.15f) {
    private var gravity: Vec3? = null
    private var lastNanos = 0L

    fun onAccelerometer(a: Vec3, timestampNanos: Long) {
        val prev = gravity
        val dt = if (lastNanos == 0L) 0f else ((timestampNanos - lastNanos) / 1e9f).coerceIn(0f, 0.5f)
        lastNanos = timestampNanos
        gravity = if (prev == null) a else prev + (a - prev) * (1f - exp(-dt / tau))
    }

    fun onMagnetic(m: Vec3): FloatArray? = gravity?.let { Orientation.fromGravityMagnetic(it, m) }

    fun reset() {
        gravity = null
        lastNanos = 0L
    }
}
