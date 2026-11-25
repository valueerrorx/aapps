package solutions.xapient.thinkr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.thinkr.ui.Motion
import solutions.xapient.thinkr.ui.ThinkrAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { TrialGated(ThinkrAccent) { ThinkrApp() } }
    }

    override fun onResume() {
        super.onResume()
        Motion.read(this)
    }
}
