package com.valueerrorx.nordr.domain

import kotlin.math.abs

// Momentaufnahme fuer die Anzeige; Winkel in Grad, heading/course im gewaehlten Nordbezug
data class CompassReading(
    val valid: Boolean = false,
    val heading: Float = 0f,
    val magnetic: Float = 0f,
    val course: Float? = null,
    val deviation: Float = 0f, // positiv = Kurs liegt rechts
    val onCourse: Boolean = false,
    val accuracy: Accuracy = Accuracy.GOOD,
    val tiltX: Float = 0f, // Welt-oben auf dem Bildschirm, -1..1
    val tiltY: Float = 0f,
) {
    val whole: Int get() = Angles.whole(heading)
    val point: Int get() = Angles.point16(heading)
}

// Orientierung -> Richtung -> Glaettung -> Totband -> Deklination, Kurs, Ticks, Genauigkeit
class CompassEngine {
    // Ost positiv; 0 = magnetisch
    var declination: Float = 0f
        set(value) {
            field = value
            cardinal.reset()
            refresh()
        }

    private val smoother = HeadingSmoother()
    private val dead = AngleDeadband(DEADBAND)
    private val tracker = CourseTracker()
    private val cardinal = CardinalDetector()
    private val accuracyMonitor = AccuracyMonitor()
    private var tiltX = 0f
    private var tiltY = 0f
    private var lastNanos = 0L

    var reading = CompassReading()
        private set

    // Ereignisse des letzten Messwerts, fuer Haptik
    var justOnCourse = false
        private set
    var justCardinal = false
        private set

    val courseMagnetic: Float? get() = tracker.course

    fun reset() {
        smoother.reset(); dead.reset(); cardinal.reset(); accuracyMonitor.reset()
        lastNanos = 0L
    }

    // Rotationsmatrix (siehe Orientation); Rueckgabe: ob sich die Anzeige geaendert hat
    fun onRotation(r: FloatArray, timestampNanos: Long, quarterTurns: Int): Boolean {
        val dt = if (lastNanos == 0L) 0f else (timestampNanos - lastNanos) / 1e9f
        lastNanos = timestampNanos
        val raw = Orientation.heading(r, quarterTurns)
        val smooth = smoother.update(raw, dt)
        if (smooth.isNaN()) return false
        dead.update(smooth)
        val up = Orientation.upOnScreen(r, quarterTurns)
        if (abs(up.x - tiltX) >= TILT_DEADBAND) tiltX = up.x
        if (abs(up.y - tiltY) >= TILT_DEADBAND) tiltY = up.y
        return refresh(events = true)
    }

    // Magnetometer: nur Genauigkeit und Stoerung
    fun onMagnetic(m: Vec3, status: Int, timestampNanos: Long): Boolean {
        accuracyMonitor.update(m, status, timestampNanos)
        return refresh()
    }

    fun setCourse() {
        if (!reading.valid) return
        tracker.set(reading.magnetic)
        refresh()
    }

    fun restoreCourse(magnetic: Float?) {
        if (magnetic == null || magnetic.isNaN()) tracker.clear() else tracker.set(magnetic)
        refresh()
    }

    fun clearCourse() {
        tracker.clear()
        refresh()
    }

    private fun refresh(events: Boolean = false): Boolean {
        justOnCourse = false
        justCardinal = false
        val mag = dead.value
        if (mag.isNaN()) {
            val next = reading.copy(accuracy = accuracyMonitor.state)
            val changed = next != reading
            reading = next
            return changed
        }
        val shown = Angles.normalize(mag + declination)
        // Kurs und Ticks nur bei neuer Richtung, sonst verschluckt das Magnetometer die Ereignisse
        if (events) {
            justOnCourse = tracker.update(mag)
            justCardinal = cardinal.update(shown)
        }
        val next = CompassReading(
            valid = true,
            heading = shown,
            magnetic = mag,
            course = tracker.course?.let { Angles.normalize(it + declination) },
            deviation = tracker.deviation(mag),
            onCourse = tracker.onCourse,
            accuracy = accuracyMonitor.state,
            tiltX = tiltX,
            tiltY = tiltY,
        )
        val changed = next != reading
        reading = next
        return changed
    }

    private companion object {
        const val DEADBAND = 0.4f
        const val TILT_DEADBAND = 0.02f
    }
}
