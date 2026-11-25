package solutions.xapient.magnr

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import solutions.xapient.aapps.core.ui.TrialGated
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
        setContent { TrialGated(MagnrAccent) { MagnrApp() } }
    }
}
