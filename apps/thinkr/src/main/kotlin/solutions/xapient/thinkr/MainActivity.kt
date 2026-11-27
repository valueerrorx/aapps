package solutions.xapient.thinkr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import solutions.xapient.aapps.core.ui.SharedPrefsKeyValues
import solutions.xapient.aapps.core.ui.TrialGated
import solutions.xapient.thinkr.ui.Motion
import solutions.xapient.thinkr.ui.ThinkrAccent
import solutions.xapient.thinkr.ui.read

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            TrialGated(ThinkrAccent) {
                val context = LocalContext.current
                val prefs = remember { Prefs(SharedPrefsKeyValues(context, "thinkr")) }
                ThinkrApp(prefs, { AndroidTonePlayer(context) }, BuildConfig.VERSION_NAME)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Motion.read(this)
    }
}
