package solutions.xapient.dozr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.dozr.ui.DozrAccent

class MainActivity : ComponentActivity() {
    // Start-Wuensche aus der Kachel (Dienst-Start dort vom System abgelehnt)
    private val startRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        SleepService.channels(this)
        if (savedInstanceState == null) take(intent)
        setContent { TrialGated(DozrAccent) { DozrApp(startRequests.intValue) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent?) {
        if (intent?.action == ACTION_START) startRequests.intValue++
    }

    companion object {
        const val ACTION_START = "solutions.xapient.dozr.START"
    }
}
