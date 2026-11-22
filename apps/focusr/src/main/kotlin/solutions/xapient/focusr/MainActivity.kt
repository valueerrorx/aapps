package solutions.xapient.focusr

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dunkle Basis: helle Symbole auf transparenten Leisten
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Alarm fuer einen laufenden Timer sicherheitshalber neu setzen (z. B. nach Update)
        EndAlarm.sync(this, Prefs(this).timer)
        setContent { FocusrApp() }
    }

    override fun onStart() {
        super.onStart()
        Visibility.visible = true
        Notifier.cancel(this, Notifier.ID_RUNNING)
        Notifier.cancel(this, Notifier.ID_DONE)
    }

    // Im Hintergrund zeigt eine stille Benachrichtigung den Countdown
    override fun onStop() {
        super.onStop()
        Visibility.visible = false
        val now = SystemClock.elapsedRealtime()
        Prefs(this).timer.alarmAt(now)?.let { Notifier.showRunning(this, it - now) }
    }
}
