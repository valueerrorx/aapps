package solutions.xapient.roundr

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.roundr.ui.RoundrAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Lautstaerketasten regeln die Signaltoene
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            TrialGated(RoundrAccent) {
                val context = LocalContext.current
                val prefs = remember { Prefs(context) }
                val store = remember { RunStore.get(context) }
                RoundrApp(
                    prefs = prefs,
                    store = store,
                    versionName = BuildConfig.VERSION_NAME,
                    start = rememberStart(prefs, store),
                    onRun = { TimerService.start(context) },
                )
            }
        }
    }
}
