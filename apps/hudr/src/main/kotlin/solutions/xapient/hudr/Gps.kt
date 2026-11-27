package solutions.xapient.hudr

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import solutions.xapient.hudr.domain.Fix

class AndroidGps(context: Context) : GpsSource {
    private val lm = context.applicationContext.getSystemService(LocationManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    override var onFix: ((Fix) -> Unit)? = null
    override var onSatellites: ((Int, Int) -> Unit)? = null
    override var onEnabled: ((Boolean) -> Unit)? = null

    // alle Methoden ausdruecklich: vor Android 11 sind sie abstrakt
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onFix?.invoke(location.toFix())
        }
        override fun onProviderEnabled(provider: String) {
            onEnabled?.invoke(true)
        }
        override fun onProviderDisabled(provider: String) {
            onEnabled?.invoke(false)
        }
        @Deprecated("nur fuer Android < 10")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    private val gnss = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) used++
            onSatellites?.invoke(used, status.satelliteCount)
        }
    }

    override fun enabled(): Boolean = lm != null && LocationManagerCompat.isLocationEnabled(lm) &&
        runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    override fun start(): Boolean {
        if (running) return true
        val m = lm ?: return false
        return try {
            m.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_MS, 0f, listener, Looper.getMainLooper())
            runCatching { m.registerGnssStatusCallback(gnss, main) }
            running = true
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            // Geraet ohne GPS-Anbieter
            false
        }
    }

    override fun stop() {
        if (!running) return
        running = false
        lm?.removeUpdates(listener)
        runCatching { lm?.unregisterGnssStatusCallback(gnss) }
    }

    private companion object {
        const val INTERVAL_MS = 500L
    }
}

fun Location.toFix(): Fix = Fix(
    nanos = elapsedRealtimeNanos,
    lat = latitude,
    lon = longitude,
    accuracy = if (hasAccuracy()) accuracy else 50f,
    speed = if (hasSpeed()) speed else Float.NaN,
    speedAccuracy = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else Float.NaN,
)
