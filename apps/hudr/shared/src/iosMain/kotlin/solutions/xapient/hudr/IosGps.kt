package solutions.xapient.hudr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLActivityTypeAutomotiveNavigation
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.Foundation.NSProcessInfo
import platform.Foundation.timeIntervalSinceNow
import platform.darwin.NSObject
import solutions.xapient.hudr.domain.Fix
import solutions.xapient.hudr.ui.LocState

// Monotone Zeit in ns seit Geraetestart, Bezug fuer Fix.nanos (wie Android elapsedRealtimeNanos)
fun uptimeNanos(): Long = (NSProcessInfo.processInfo.systemUptime * 1e9).toLong()

/**
 * GPS ueber CoreLocation, nur "Beim Verwenden der App" und nur zwischen Start und Stopp. iOS nennt keine
 * Satellitenzahl, die Anzeige zeigt dann nur die Genauigkeit.
 */
@OptIn(ExperimentalForeignApi::class)
class IosGps : GpsSource {
    override var onFix: ((Fix) -> Unit)? = null
    override var onSatellites: ((used: Int, visible: Int) -> Unit)? = null
    override var onEnabled: ((Boolean) -> Unit)? = null

    // Recht oder Genauigkeit geaendert
    var onAuthorization: (() -> Unit)? = null

    private val manager = CLLocationManager()
    private var running = false

    // stark gehalten: CLLocationManager.delegate ist schwach
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            onFix?.invoke(loc.toFix())
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            onAuthorization?.invoke()
        }
    }

    init {
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        manager.distanceFilter = kCLDistanceFilterNone
        manager.activityType = CLActivityTypeAutomotiveNavigation
        manager.pausesLocationUpdatesAutomatically = false
    }

    private val status: CLAuthorizationStatus get() = manager.authorizationStatus
    private val authorized: Boolean
        get() = status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
    private val precise: Boolean
        get() = manager.accuracyAuthorization == CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy

    val notDetermined: Boolean get() = status == kCLAuthorizationStatusNotDetermined

    // Ortungsdienste aus = GPS_OFF; sonst wie Android: fragen, ungefaehr, gesperrt, bereit
    fun locState(): LocState = when {
        !enabled() -> LocState.GPS_OFF
        notDetermined -> LocState.ASK
        !authorized -> LocState.BLOCKED
        !precise -> LocState.COARSE
        else -> LocState.READY
    }

    fun requestWhenInUse() = manager.requestWhenInUseAuthorization()

    // genauer Standort fuer diese Sitzung; Zweck-Text in Info.plist (NSLocationTemporaryUsageDescriptionDictionary)
    fun requestPrecise() = manager.requestTemporaryFullAccuracyAuthorizationWithPurposeKey(PURPOSE_KEY)

    override fun enabled(): Boolean = CLLocationManager.locationServicesEnabled()

    override fun start(): Boolean {
        if (running) return true
        if (!authorized) return false
        manager.startUpdatingLocation()
        running = true
        return true
    }

    override fun stop() {
        if (!running) return
        running = false
        manager.stopUpdatingLocation()
    }

    // Zeitstempel ist Uhrzeit: ueber das Alter auf die monotone Uhr umrechnen
    private fun CLLocation.toFix(): Fix {
        val ageNanos = (-timestamp.timeIntervalSinceNow * 1e9).toLong().coerceAtLeast(0L)
        val (lat, lon) = coordinate.useContents { latitude to longitude }
        return Fix(
            nanos = uptimeNanos() - ageNanos,
            lat = lat,
            lon = lon,
            accuracy = if (horizontalAccuracy >= 0.0) horizontalAccuracy.toFloat() else 50f,
            speed = if (speed >= 0.0) speed.toFloat() else Float.NaN,
            speedAccuracy = if (speedAccuracy >= 0.0) speedAccuracy.toFloat() else Float.NaN,
        )
    }

    companion object {
        const val PURPOSE_KEY = "speed"
    }
}
