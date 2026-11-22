package com.valueerrorx.levlr.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.valueerrorx.levlr.domain.Calibration
import com.valueerrorx.levlr.domain.CalibrationSession
import com.valueerrorx.levlr.domain.LevelEngine
import com.valueerrorx.levlr.domain.LevelPhysics
import com.valueerrorx.levlr.domain.LevelReading
import com.valueerrorx.levlr.domain.Vec3

// Zustand der Waage fuer Compose; Sensor-Callbacks laufen auf dem Main-Thread
@Stable
class LevelState(calibration: Calibration = Calibration()) {
    val engine = LevelEngine(calibration)
    val physics = LevelPhysics()

    var reading by mutableStateOf(LevelReading.FLAT_LEVEL)
        private set
    var held by mutableStateOf(false)
        private set

    // Zaehler: Zeichnen ausloesen, Physik wecken, Einrasten melden
    var frame by mutableLongStateOf(0L)
        internal set
    var targetVersion by mutableIntStateOf(0)
        private set
    var snapCount by mutableIntStateOf(0)
        private set

    var calibration by mutableStateOf<CalibrationSession?>(null)
        private set
    var calibrationTick by mutableIntStateOf(0)
        private set
    var quarterTurns = 0

    private var lastNanos = 0L

    fun onSample(raw: Vec3, timestampNanos: Long) {
        val dt = if (lastNanos == 0L) 0f else ((timestampNanos - lastNanos) / 1e9f).coerceIn(0f, 0.5f)
        lastNanos = timestampNanos
        calibration?.let {
            it.offer(raw, dt, engine.mode)
            calibrationTick++
        }
        if (engine.onSample(raw, timestampNanos, quarterTurns) && !held) {
            show(engine.reading)
            if (engine.justLeveled) snapCount++
        }
    }

    // Fester Wert, z. B. fuer Screenshots und nach dem Loslassen
    fun show(r: LevelReading) {
        reading = r
        physics.setTarget(r)
        targetVersion++
    }

    fun toggleHold() {
        held = !held
        if (!held) show(engine.reading)
    }

    fun startCalibration() {
        if (held) toggleHold()
        calibration = CalibrationSession()
        calibrationTick++
    }

    fun measureCalibration() {
        calibration?.startMeasuring(engine.mode)
        calibrationTick++
    }

    fun endCalibration() {
        calibration = null
    }

    // Sensor-Pause: Zeitbasis neu, damit dt nicht springt
    fun onPause() {
        lastNanos = 0L
        engine.reset()
    }
}
