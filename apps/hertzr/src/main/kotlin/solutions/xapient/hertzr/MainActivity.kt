package solutions.xapient.hertzr

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.hertzr.ui.HertzrAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Lautstaerketasten regeln den Ton
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent { TrialGated(HertzrAccent) { HertzrApp() } }
    }
}
