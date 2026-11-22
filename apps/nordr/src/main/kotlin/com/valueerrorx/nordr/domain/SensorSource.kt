package com.valueerrorx.nordr.domain

// Callbacks der Sensorquelle; Rotationsmatrix siehe Orientation
interface CompassListener {
    fun onRotation(r: FloatArray, timestampNanos: Long)
    fun onMagnetic(m: Vec3, status: Int, timestampNanos: Long)
}

interface SensorSource {
    val isAvailable: Boolean
    fun start(listener: CompassListener)
    fun stop()
}
