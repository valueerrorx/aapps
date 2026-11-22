package com.valueerrorx.levlr.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.valueerrorx.aapps.core.ui.SettingsAction
import com.valueerrorx.levlr.R
import kotlinx.coroutines.delay
import java.io.File

// Nur im Debug-Build: 10 s Rohdaten als CSV, Teilen ueber FileProvider (src/debug)
@Composable
fun DiagnosticsRow() {
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(0) }
    SettingsAction(
        if (recording) stringResource(R.string.recording, left) else stringResource(R.string.record_sensor),
        stringResource(R.string.record_sub),
        enabled = !recording,
    ) { recording = true }

    if (recording) {
        LaunchedEffect(Unit) {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val csv = StringBuilder("t_ns,sensor,x,y,z\n")
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    val name = if (e.sensor.type == Sensor.TYPE_GRAVITY) "gravity" else "accel"
                    csv.append(e.timestamp).append(',').append(name)
                    for (i in 0 until 3) csv.append(',').append(e.values[i])
                    csv.append('\n')
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            try {
                listOf(Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER).forEach { type ->
                    manager?.getDefaultSensor(type)?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
                }
                for (s in SECONDS downTo 1) {
                    left = s
                    delay(1000)
                }
            } finally {
                manager?.unregisterListener(listener)
                recording = false
            }
            share(context, csv.toString())
        }
    }
}

private const val SECONDS = 10

private fun share(context: Context, csv: String) {
    val dir = File(context.cacheDir, "diag").apply { mkdirs() }
    val file = File(dir, "levlr-sensor-${System.currentTimeMillis()}.csv")
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, context.packageName + ".diag", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(send, null))
    } catch (_: ActivityNotFoundException) {
        // keine App zum Teilen vorhanden
    }
}
