package solutions.xapient.nordr

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import solutions.xapient.aapps.core.ui.SharedPrefsKeyValues
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.nordr.sensor.AndroidDeclination
import solutions.xapient.nordr.sensor.AndroidSensorSource
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            TrialGated(NordrAccent) {
                val context = LocalContext.current
                val prefs = remember { Prefs(SharedPrefsKeyValues(context, "nordr"), TimeZone.getDefault().id) }
                val sensor = remember {
                    AndroidSensorSource(context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
                }
                NordrApp(
                    prefs = prefs,
                    sensor = sensor,
                    declinationOf = { AndroidDeclination.declination(it.lat, it.lon, System.currentTimeMillis()) },
                    versionName = BuildConfig.VERSION_NAME,
                    diagnostics = if (BuildConfig.DEBUG) ({ DiagnosticsRow() }) else null,
                )
            }
        }
    }
}
