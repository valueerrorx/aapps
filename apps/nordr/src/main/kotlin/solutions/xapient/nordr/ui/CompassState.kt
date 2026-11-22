package solutions.xapient.nordr.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import solutions.xapient.nordr.domain.Accuracy
import solutions.xapient.nordr.domain.CompassEngine
import solutions.xapient.nordr.domain.CompassListener
import solutions.xapient.nordr.domain.CompassPhysics
import solutions.xapient.nordr.domain.CompassReading
import solutions.xapient.nordr.domain.Vec3

// Welcher Hinweis ueber dem Kompass liegt
enum class Hint { CALIBRATE, INTERFERENCE }

// Zustand des Kompasses fuer Compose; Sensor-Callbacks laufen auf dem Main-Thread
@Stable
class CompassState {
    val engine = CompassEngine()
    val physics = CompassPhysics()

    var reading by mutableStateOf(CompassReading())
        private set

    // Zaehler: Zeichnen ausloesen, Physik wecken, Ereignisse fuer Haptik
    var frame by mutableLongStateOf(0L)
        internal set
    var targetVersion by mutableIntStateOf(0)
        private set
    var courseSnaps by mutableIntStateOf(0)
        private set
    var cardinalTicks by mutableIntStateOf(0)
        private set

    var quarterTurns = 0

    // Hinweis: von Hand geoeffnet oder bei schlechter Genauigkeit; ausgeblendet bis zur naechsten Verschlechterung
    private var manualHint by mutableStateOf(false)
    private var dismissed by mutableStateOf(Accuracy.GOOD)
    val hint: Hint?
        get() = when {
            reading.accuracy == Accuracy.INTERFERENCE && dismissed < Accuracy.INTERFERENCE -> Hint.INTERFERENCE
            manualHint -> Hint.CALIBRATE
            reading.accuracy == Accuracy.LOW && dismissed < Accuracy.LOW -> Hint.CALIBRATE
            else -> null
        }
    val hintIsManual: Boolean get() = manualHint && reading.accuracy == Accuracy.GOOD

    val listener = object : CompassListener {
        override fun onRotation(r: FloatArray, timestampNanos: Long) {
            if (engine.onRotation(r, timestampNanos, quarterTurns)) show(engine.reading)
            // Neigung nur an die Physik, kein neuer Anzeigezustand
            if (physics.setTilt(engine.upX, engine.upY)) targetVersion++
            if (engine.justOnCourse) courseSnaps++
            if (engine.justCardinal) cardinalTicks++
        }

        override fun onMagnetic(m: Vec3, status: Int, timestampNanos: Long) {
            if (engine.onMagnetic(m, status, timestampNanos)) show(engine.reading)
        }
    }

    // Fester Wert, z. B. fuer Screenshots
    fun show(r: CompassReading) {
        reading = r
        // Verbesserung senkt die Schwelle, die naechste Verschlechterung zeigt den Hinweis wieder
        if (r.accuracy < dismissed) dismissed = r.accuracy
        physics.setTarget(r)
        targetVersion++
    }

    fun setDeclination(deg: Float) {
        engine.declination = deg
        show(engine.reading)
    }

    // Rueckgabe: neuer Kurs (magnetisch) oder null
    fun toggleCourse(): Float? {
        if (engine.courseMagnetic != null) engine.clearCourse() else engine.setCourse()
        show(engine.reading)
        return engine.courseMagnetic
    }

    fun showHint() {
        manualHint = true
        dismissed = Accuracy.GOOD
    }

    fun dismissHint() {
        manualHint = false
        dismissed = reading.accuracy
    }

    // Sensor-Pause: Zeitbasis und Filter neu
    fun onPause() = engine.reset()
}
