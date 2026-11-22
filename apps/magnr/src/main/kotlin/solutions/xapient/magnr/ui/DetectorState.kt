package solutions.xapient.magnr.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import solutions.xapient.magnr.domain.FieldListener
import solutions.xapient.magnr.domain.FieldMeter
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.domain.Reading

// Welche Karte ueber dem Radar liegt
enum class Hint { INTRO, CALIBRATE }

/**
 * Zustand fuer Compose; Sensor-Callbacks laufen auf dem Main-Thread.
 * tick wechselt je Messwert und wird nur in der Zeichenphase gelesen.
 */
@Stable
class DetectorState(val meter: FieldMeter, private val maxRange: () -> Float = { 0f }) {
    var tick by mutableLongStateOf(0L)
        private set

    // fester Wert statt Messung, z. B. fuer Screenshots
    var fixed: Reading? = null

    // neuester Stand (Bogen, Verlauf) und gehaltener Stand fuer die Ziffern
    val reading: Reading get() = fixed ?: meter.reading
    var shown: Reading = Reading.EMPTY
        get() = fixed ?: field
        private set
    private var shownAt = 0L

    // Zeit des letzten Messwerts in Sekunden, fuer die Wellen
    var seconds = 0f
        private set

    var onRise: ((Level) -> Unit)? = null
    var onStrength: ((Float) -> Unit)? = null

    // Karten: Einfuehrung, von Hand geoeffnete Kalibrierhilfe, sonst bei niedriger Genauigkeit
    var introVisible by mutableStateOf(false)
    private var manualCalibrate by mutableStateOf(false)
    var accuracyLow by mutableStateOf(false)
        private set
    private var calibrateDismissed by mutableStateOf(false)

    val hint: Hint?
        get() = when {
            introVisible -> Hint.INTRO
            manualCalibrate -> Hint.CALIBRATE
            accuracyLow && !calibrateDismissed -> Hint.CALIBRATE
            else -> null
        }

    val listener = FieldListener { x, y, z, status, t ->
        val r = meter.onSample(x, y, z, maxRange(), status, t)
        seconds = (t % 1_000_000_000_000L) / 1e9f
        if (meter.rose || r.level < shown.level || t - shownAt >= HOLD_NS || shownAt == 0L) {
            shown = r
            shownAt = t
        }
        if (meter.rose) onRise?.invoke(r.level)
        onStrength?.invoke(if (r.saturated) 1f else r.strength)
        setAccuracy(r.accuracyLow)
        tick++
    }

    private fun setAccuracy(low: Boolean) {
        if (low == accuracyLow) return
        accuracyLow = low
        // besser geworden: naechste Verschlechterung zeigt die Karte wieder
        if (!low) calibrateDismissed = false
    }

    fun tare() {
        meter.tare()
        shown = meter.reading
        onStrength?.invoke(0f)
        tick++
    }

    // Sensor wieder an; nowMillis = elapsedRealtime, lange weg = neu einschwingen
    fun resume(nowMillis: Long) {
        meter.resume(nowMillis / 1000f)
        shown = meter.reading
        shownAt = 0L
        tick++
    }

    // Sensor aus; nowMillis = elapsedRealtime
    fun pause(nowMillis: Long) {
        meter.pause(nowMillis / 1000f)
        shown = meter.reading
        shownAt = 0L
        onStrength?.invoke(0f)
        setAccuracy(false)
        tick++
    }

    fun showCalibration() {
        introVisible = false
        manualCalibrate = true
    }

    fun dismissHint() {
        when (hint) {
            Hint.INTRO -> introVisible = false
            Hint.CALIBRATE -> {
                manualCalibrate = false
                if (accuracyLow) calibrateDismissed = true
            }
            null -> Unit
        }
    }

    private companion object {
        // Ziffern hoechstens viermal pro Sekunde
        const val HOLD_NS = 250_000_000L
    }
}
