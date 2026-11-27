package solutions.xapient.magnr

import android.content.Context
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import solutions.xapient.aapps.core.ui.SharedPrefsKeyValues
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.magnr.domain.Beeper
import solutions.xapient.magnr.domain.FieldSource
import solutions.xapient.magnr.sensor.AndroidFieldSource
import solutions.xapient.magnr.sensor.AudioBeeper
import solutions.xapient.magnr.ui.MagnrAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Lautstaerketasten regeln den Piepton
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent { TrialGated(MagnrAccent) { MagnrRoot() } }
    }
}

// Einstellungen in den SharedPreferences "magnr" (Backup ist aus)
fun Prefs(context: Context) = Prefs(SharedPrefsKeyValues(context.applicationContext, "magnr"))

private fun androidSource(context: Context): FieldSource =
    AndroidFieldSource(context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)

// Android-Einstieg; Tests setzen eigene Quelle und eigenen Ton
@Composable
fun MagnrRoot(
    newSource: (Context) -> FieldSource = ::androidSource,
    newBeeper: () -> Beeper = ::AudioBeeper,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    MagnrApp(
        prefs = prefs,
        newSource = { newSource(context) },
        newBeeper = newBeeper,
        elapsedMillis = SystemClock::elapsedRealtime,
        versionName = BuildConfig.VERSION_NAME,
    )
}
