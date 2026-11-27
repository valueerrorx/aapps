package solutions.xapient.pulsr

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.pulsr.ui.PulsrAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Lautstaerketasten regeln das Metronom
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent { TrialGated(PulsrAccent) { AndroidPulsrApp() } }
    }
}
