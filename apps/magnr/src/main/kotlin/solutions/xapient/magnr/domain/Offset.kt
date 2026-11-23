package solutions.xapient.magnr.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

// Drehmatrix Handy -> Welt aus einem Einheitsquaternion (w, x, y, z), wie getRotationMatrixFromVector
fun rotationMatrix(w: Float, x: Float, y: Float, z: Float): DoubleArray {
    val qw = w.toDouble()
    val qx = x.toDouble()
    val qy = y.toDouble()
    val qz = z.toDouble()
    return doubleArrayOf(
        1 - 2 * (qy * qy + qz * qz), 2 * (qx * qy - qz * qw), 2 * (qx * qz + qy * qw),
        2 * (qx * qy + qz * qw), 1 - 2 * (qx * qx + qz * qz), 2 * (qy * qz - qx * qw),
        2 * (qx * qz - qy * qw), 2 * (qy * qz + qx * qw), 1 - 2 * (qx * qx + qy * qy),
    )
}

// Lage zum Zeitpunkt eines Magnetometer-Werts aus den letzten zwei Rotationswerten (linear, auch kurz voraus)
class RotationTrack(private val staleNanos: Long = 100_000_000L) {
    private val q0 = FloatArray(4)
    private val q1 = FloatArray(4)
    private var t0 = -1L
    private var t1 = -1L

    fun reset() {
        t0 = -1L
        t1 = -1L
    }

    fun add(w: Float, x: Float, y: Float, z: Float, timestampNanos: Long) {
        if (t1 >= 0L && timestampNanos <= t1) return
        q1.copyInto(q0)
        t0 = t1
        // gleiche Halbkugel wie der Vorgaenger: q und -q sind dieselbe Lage
        val s = if (t1 >= 0L && w * q0[0] + x * q0[1] + y * q0[2] + z * q0[3] < 0f) -1f else 1f
        q1[0] = s * w; q1[1] = s * x; q1[2] = s * y; q1[3] = s * z
        t1 = timestampNanos
    }

    // null: keine frische Lage
    fun at(timestampNanos: Long): DoubleArray? {
        if (t1 < 0L || abs(timestampNanos - t1) > staleNanos) return null
        if (t0 < 0L || t1 - t0 > staleNanos) return rotationMatrix(q1[0], q1[1], q1[2], q1[3])
        val f = ((timestampNanos - t0).toDouble() / (t1 - t0)).coerceIn(-1.0, 3.0)
        val q = DoubleArray(4) { q0[it] + f * (q1[it] - q0[it]) }
        val n = sqrt(q.sumOf { it * it })
        return rotationMatrix((q[0] / n).toFloat(), (q[1] / n).toFloat(), (q[2] / n).toFloat(), (q[3] / n).toFloat())
    }
}

/**
 * Rest-Versatz o des kalibrierten Magnetometers aus Drehungen (Lage aus Gyro, ohne Magnetometer).
 * Ohne Metall dreht sich B - o starr mit dem Handy: (I - dR) o = B_t - dR B_k mit dR = R_t^T R_k.
 * Reines Verschieben (dR = I) liefert keine Gleichung, Metall beim Ueberstreichen stoert die Schaetzung also nicht.
 * Kleinste Quadrate mit Vergessen; Ridge haelt nicht beobachtete Richtungen bei 0.
 */
class OffsetEstimator(
    stepDegrees: Double = 15.0,
    private val windowNanos: Long = 1_000_000_000L,
    private val decay: Double = 0.98,
    private val ridge: Double = 0.05,
    private val maxResidual: Double = 15.0,
    private val maxOffset: Double = 100.0,
) {
    private val minCos = cos(Math.toRadians(stepDegrees))
    private val m = DoubleArray(9)
    private val v = DoubleArray(3)
    private val keyB = DoubleArray(3)
    private var keyR: DoubleArray? = null
    private var keyNanos = 0L

    // geschaetzter Versatz in µT (Handy-Koordinaten)
    val offset = FloatArray(3)

    // naechste Gleichung startet neu (z. B. nach Pause)
    fun restart() {
        keyR = null
    }

    // true: Versatz hat sich geaendert
    fun add(x: Float, y: Float, z: Float, r: DoubleArray, timestampNanos: Long): Boolean {
        val k = keyR
        if (k == null || timestampNanos - keyNanos > windowNanos) {
            setKey(x, y, z, r, timestampNanos)
            return false
        }
        val d = DoubleArray(9)
        for (i in 0..2) for (j in 0..2) d[i * 3 + j] = r[i] * k[j] + r[3 + i] * k[3 + j] + r[6 + i] * k[6 + j]
        if ((d[0] + d[4] + d[8] - 1) / 2 > minCos) return false
        val a = DoubleArray(9) { (if (it % 4 == 0) 1.0 else 0.0) - d[it] }
        val bt = doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble())
        val b = DoubleArray(3) { bt[it] - (d[it * 3] * keyB[0] + d[it * 3 + 1] * keyB[1] + d[it * 3 + 2] * keyB[2]) }
        setKey(x, y, z, r, timestampNanos)
        // passt nicht zum Modell (Metall im Gefaelle, Sprung der Systemkalibrierung): auslassen
        var res = 0.0
        for (i in 0..2) {
            val e = b[i] - (a[i * 3] * offset[0] + a[i * 3 + 1] * offset[1] + a[i * 3 + 2] * offset[2])
            res += e * e
        }
        if (res > maxResidual * maxResidual) return false
        for (i in 0..8) m[i] = m[i] * decay + ata(a, i / 3, i % 3)
        for (i in 0..2) v[i] = v[i] * decay + a[i] * b[0] + a[3 + i] * b[1] + a[6 + i] * b[2]
        return solve()
    }

    private fun ata(a: DoubleArray, i: Int, j: Int) = a[i] * a[j] + a[3 + i] * a[3 + j] + a[6 + i] * a[6 + j]

    private fun setKey(x: Float, y: Float, z: Float, r: DoubleArray, timestampNanos: Long) {
        keyB[0] = x.toDouble(); keyB[1] = y.toDouble(); keyB[2] = z.toDouble()
        keyR = r
        keyNanos = timestampNanos
    }

    // (M + ridge I) o = v nach Cramer
    private fun solve(): Boolean {
        val n = DoubleArray(9) { m[it] + if (it % 4 == 0) ridge else 0.0 }
        val det = det(n)
        if (abs(det) < 1e-12) return false
        val o = DoubleArray(3) { c -> det(n.copyOf().also { for (r in 0..2) it[r * 3 + c] = v[r] }) / det }
        if (sqrt(o.sumOf { it * it }) > maxOffset) return false
        for (i in 0..2) offset[i] = o[i].toFloat()
        return true
    }

    private fun det(n: DoubleArray) =
        n[0] * (n[4] * n[8] - n[5] * n[7]) - n[1] * (n[3] * n[8] - n[5] * n[6]) + n[2] * (n[3] * n[7] - n[4] * n[6])

    // Gleichungssystem ueberlebt Prozess-Tod
    fun save(): FloatArray = FloatArray(SIZE) { if (it < 9) m[it].toFloat() else v[it - 9].toFloat() }

    fun restore(s: FloatArray) {
        if (s.size < SIZE || s.any { it.isNaN() }) return
        for (i in 0..8) m[i] = s[i].toDouble()
        for (i in 0..2) v[i] = s[9 + i].toDouble()
        if (!solve()) offset.fill(0f)
    }

    companion object {
        const val SIZE = 12
    }
}
