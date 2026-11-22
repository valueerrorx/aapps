package solutions.xapient.levlr.domain

import kotlin.math.abs

// Momentaufnahme fuer die Anzeige; Winkel in Grad
data class LevelReading(
    val mode: LevelMode,
    val edgeAngle: Float = 0f, // Kante: rechts tiefer = positiv
    val quadrant: Int = 0, // Kante: welche Bildschirmkante unten liegt
    val flatX: Float = 0f, // Flach: rechts hoeher = positiv
    val flatY: Float = 0f, // Flach: oben hoeher = positiv
    val isLevel: Boolean = false,
) {
    // Hauptwert der grossen Anzeige, immer >= 0
    val primary: Float
        get() = if (isLevel) 0f else if (mode == LevelMode.EDGE) abs(edgeAngle) else flatTotal

    val flatTotal: Float get() = kotlin.math.hypot(flatX, flatY)

    companion object {
        val FLAT_LEVEL = LevelReading(LevelMode.FLAT, isLevel = true)
    }
}

// Rohwert -> Kalibrierung -> Glaettung -> Bildschirmkoordinaten -> Winkel, Modus, Einrasten
class LevelEngine(calibration: Calibration = Calibration()) {
    var calibration: Calibration = calibration
        set(value) {
            field = value
            reset()
        }

    private val smoother = VectorSmoother()
    private val modeDetector = ModeDetector()
    private val snap = SnapDetector()
    private val deadA = Deadband(DEADBAND)
    private val deadX = Deadband(DEADBAND)
    private val deadY = Deadband(DEADBAND)
    private var lastNanos = 0L
    private var quadrant: Int? = null

    var mode: LevelMode = LevelMode.FLAT
        private set
    var reading: LevelReading = LevelReading.FLAT_LEVEL
        private set

    // Liefert true beim Einrasten (fuer Haptik)
    var justLeveled = false
        private set

    fun reset() {
        smoother.reset(); snap.reset(); deadA.reset(); deadX.reset(); deadY.reset()
        lastNanos = 0L
        quadrant = null
    }

    // Verarbeitet einen Messwert; Rueckgabe: ob sich die Anzeige geaendert hat
    fun onSample(raw: Vec3, timestampNanos: Long, quarterTurns: Int): Boolean {
        val dt = if (lastNanos == 0L) 0f else (timestampNanos - lastNanos) / 1e9f
        lastNanos = timestampNanos
        val rawMode = modeDetector.update(TiltMath.inclination(raw))
        if (rawMode != mode) {
            mode = rawMode
            snap.reset()
        }
        val corrected = raw - calibration.biasFor(mode).asVec()
        val smooth = smoother.update(corrected, dt)
        val up = TiltMath.toScreen(smooth, quarterTurns)

        val next = when (mode) {
            LevelMode.EDGE -> {
                val raw360 = TiltMath.edgeRaw(up)
                val q = quadrant?.let { TiltMath.edgeQuadrant(raw360, it) } ?: TiltMath.edgeQuadrant(raw360)
                if (q != quadrant) deadA.reset()
                quadrant = q
                val a = deadA.update(TiltMath.edgeAngle(raw360, q))
                justLeveled = snap.update(abs(a))
                LevelReading(LevelMode.EDGE, edgeAngle = a, quadrant = q, isLevel = snap.isLevel)
            }
            LevelMode.FLAT -> {
                val x = deadX.update(TiltMath.flatX(up))
                val y = deadY.update(TiltMath.flatY(up))
                justLeveled = snap.update(kotlin.math.hypot(x, y))
                LevelReading(LevelMode.FLAT, flatX = x, flatY = y, isLevel = snap.isLevel)
            }
        }
        val changed = next != reading
        reading = next
        return changed
    }

    private companion object {
        const val DEADBAND = 0.03f
    }
}
