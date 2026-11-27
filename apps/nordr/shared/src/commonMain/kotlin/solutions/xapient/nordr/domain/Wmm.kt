package solutions.xapient.nordr.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Deklination aus dem Weltmagnetfeld-Modell WMM2025 (NOAA/BGS, gemeinfrei), gueltig 2025-2030; fuer iOS ohne Standort
object Wmm : DeclinationModel {
    private const val EPOCH = 2025.0
    private const val N = 12

    // Gauss-Koeffizienten (nT) und jaehrliche Aenderung, Reihenfolge n = 1..12, m = 0..n
    private val G = doubleArrayOf(
        -29351.8, -1410.8, -2556.6, 2951.1, 1649.3, 1361.0, -2404.1, 1243.8,
        453.6, 895.0, 799.5, 55.7, -281.1, 12.1, -233.2, 368.9,
        187.2, -138.7, -142.0, 20.9, 64.4, 63.8, 76.9, -115.7,
        -40.9, 14.9, -60.7, 79.5, -77.0, -8.8, 59.3, 15.8,
        2.5, -11.1, 14.2, 23.2, 10.8, -17.5, 2.0, -21.7,
        16.9, 15.0, -16.8, 0.9, 4.6, 7.8, 3.0, -0.2,
        -2.5, -13.1, 2.4, 8.6, -8.7, -12.9, -1.3, -6.4,
        0.2, 2.0, -1.0, -0.6, -0.9, 1.5, 0.9, -2.7,
        -3.9, 2.9, -1.5, -2.5, 2.4, -0.6, -0.1, -0.6,
        -0.1, 1.1, -1.0, -0.2, 2.6, -2.0, -0.2, 0.3,
        1.2, -1.3, 0.6, 0.6, 0.5, -0.1, -0.4, -0.2,
        -1.3, -0.7,
    )
    private val H = doubleArrayOf(
        0.0, 4545.4, 0.0, -3133.6, -815.1, 0.0, -56.6, 237.5,
        -549.5, 0.0, 278.6, -133.9, 212.0, -375.6, 0.0, 45.4,
        220.2, -122.9, 43.0, 106.1, 0.0, -18.4, 16.8, 48.8,
        -59.8, 10.9, 72.7, 0.0, -48.9, -14.4, -1.0, 23.4,
        -7.4, -25.1, -2.3, 0.0, 7.1, -12.6, 11.4, -9.7,
        12.7, 0.7, -5.2, 3.9, 0.0, -24.8, 12.2, 8.3,
        -3.3, -5.2, 7.2, -0.6, 0.8, 10.0, 0.0, 3.3,
        0.0, 2.4, 5.3, -9.1, 0.4, -4.2, -3.8, 0.9,
        -9.1, 0.0, 0.0, 2.9, -0.6, 0.2, 0.5, -0.3,
        -1.2, -1.7, -2.9, -1.8, -2.3, 0.0, -1.3, 0.7,
        1.0, -1.4, -0.0, 0.6, -0.1, 0.8, 0.1, -1.0,
        0.1, 0.2,
    )
    private val GDOT = doubleArrayOf(
        12.0, 9.7, -11.6, -5.2, -8.0, -1.3, -4.2, 0.4,
        -15.6, -1.6, -2.4, -6.0, 5.6, -7.0, 0.6, 1.4,
        0.0, 0.6, 2.2, 0.9, -0.2, -0.4, 0.9, 1.2,
        -0.9, 0.3, 0.9, -0.0, -0.1, -0.1, 0.5, -0.1,
        -0.8, -0.8, 0.8, -0.1, 0.2, 0.0, 0.5, -0.1,
        0.3, 0.2, -0.0, 0.2, -0.0, -0.1, 0.1, 0.3,
        -0.3, 0.0, 0.3, -0.1, 0.1, -0.1, 0.1, 0.0,
        0.1, 0.1, -0.0, -0.3, 0.0, -0.1, -0.1, -0.0,
        -0.0, 0.0, -0.0, 0.0, 0.0, 0.0, -0.1, 0.0,
        -0.0, -0.1, -0.1, -0.1, -0.1, 0.0, 0.0, -0.0,
        -0.0, -0.0, -0.0, 0.1, -0.0, 0.0, 0.0, -0.1,
        -0.0, -0.1,
    )
    private val HDOT = doubleArrayOf(
        0.0, -21.5, 0.0, -27.7, -12.1, 0.0, 4.0, -0.3,
        -4.1, 0.0, -1.1, 4.1, 1.6, -4.4, 0.0, -0.5,
        2.2, 0.4, 1.7, 1.9, 0.0, 0.3, -1.6, -0.4,
        0.9, 0.7, 0.9, 0.0, 0.6, 0.5, -0.8, 0.0,
        -1.0, 0.6, -0.2, 0.0, -0.2, 0.5, -0.4, 0.4,
        -0.5, -0.6, 0.3, 0.2, 0.0, -0.3, 0.3, -0.3,
        0.3, 0.2, -0.1, -0.2, 0.4, 0.1, 0.0, 0.0,
        -0.0, -0.2, 0.1, -0.1, 0.1, 0.0, -0.1, 0.2,
        -0.0, 0.0, -0.0, 0.1, -0.0, 0.1, -0.0, -0.0,
        0.1, -0.0, 0.0, 0.0, 0.0, 0.0, -0.0, 0.0,
        -0.1, 0.1, -0.0, -0.0, -0.0, 0.0, -0.0, -0.0,
        0.0, -0.1,
    )

    // Schmidt-Normierung je (n, m)
    private val schmidt = Array(N + 1) { DoubleArray(N + 1) }.also { s ->
        s[0][0] = 1.0
        for (n in 1..N) {
            s[n][0] = s[n - 1][0] * (2 * n - 1) / n
            for (m in 1..n) s[n][m] = s[n][m - 1] * sqrt((n - m + 1).toDouble() * (if (m == 1) 2 else 1) / (n + m))
        }
    }

    private const val MS_PER_YEAR = 365.25 * 24 * 3600 * 1000
    private const val YEAR_1970 = 1970.0

    override fun declination(lat: Float, lon: Float, timeMillis: Long): Float =
        declination(lat.toDouble(), lon.toDouble(), 0.0, YEAR_1970 + timeMillis / MS_PER_YEAR).toFloat()

    /** Grad, Ost positiv; Hoehe in km ueber dem Ellipsoid, Jahr dezimal */
    fun declination(latDeg: Double, lonDeg: Double, altKm: Double, year: Double): Double {
        // geodaetisch (WGS84) -> geozentrisch
        val a = 6378.137
        val f = 1 / 298.257223563
        val e2 = f * (2 - f)
        val phi = latDeg * PI / 180
        val lam = lonDeg * PI / 180
        val rc = a / sqrt(1 - e2 * sin(phi) * sin(phi))
        val p = (rc + altKm) * cos(phi)
        val z = (rc * (1 - e2) + altKm) * sin(phi)
        val r = hypot(p, z)
        val phiC = asin(z / r)
        val ct = sin(phiC)
        val st = cos(phiC).coerceAtLeast(1e-9)

        // Legendre-Funktionen (Gauss-normiert) und Ableitung nach der Kobreite
        val pp = Array(N + 1) { DoubleArray(N + 1) }
        val dp = Array(N + 1) { DoubleArray(N + 1) }
        pp[0][0] = 1.0
        for (n in 1..N) {
            for (m in 0..n) {
                if (m == n) {
                    pp[n][m] = st * pp[n - 1][m - 1]
                    dp[n][m] = st * dp[n - 1][m - 1] + ct * pp[n - 1][m - 1]
                } else {
                    val k = if (n >= 2) ((n - 1) * (n - 1) - m * m).toDouble() / ((2 * n - 1) * (2 * n - 3)) else 0.0
                    val p2 = if (n >= 2) pp[n - 2][m] else 0.0
                    val d2 = if (n >= 2) dp[n - 2][m] else 0.0
                    pp[n][m] = ct * pp[n - 1][m] - k * p2
                    dp[n][m] = ct * dp[n - 1][m] - st * pp[n - 1][m] - k * d2
                }
            }
        }

        val dt = year - EPOCH
        var x = 0.0
        var y = 0.0
        var zz = 0.0
        var i = 0
        for (n in 1..N) {
            val rr = (6371.2 / r).pow(n + 2)
            for (m in 0..n) {
                val g = G[i] + dt * GDOT[i]
                val h = H[i] + dt * HDOT[i]
                i++
                val c = cos(m * lam)
                val s = sin(m * lam)
                val sp = pp[n][m] * schmidt[n][m]
                x += rr * (g * c + h * s) * dp[n][m] * schmidt[n][m]
                y += rr * m * (g * s - h * c) * sp / st
                zz -= (n + 1) * rr * (g * c + h * s) * sp
            }
        }
        // zurueck ins geodaetische System; Ost-Komponente bleibt
        val d = phi - phiC
        val north = x * cos(d) + zz * sin(d)
        return atan2(y, north) * 180 / PI
    }
}
