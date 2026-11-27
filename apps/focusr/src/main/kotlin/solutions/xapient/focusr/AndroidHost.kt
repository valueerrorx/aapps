package solutions.xapient.focusr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import solutions.xapient.focusr.domain.Timer
import java.util.Date

// Ende per exaktem Alarm (EndAlarm/EndReceiver), Countdown-Benachrichtigung im Hintergrund (MainActivity.onStop)
class AndroidHost(private val context: Context) : FocusrHost {
    override fun sync(timer: Timer) = EndAlarm.sync(context, timer)

    override fun endText(inMs: Long): String = DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + inMs))

    override val countdownNotification = true
}

// Recht kann in den Systemeinstellungen geaendert werden; einmal von selbst fragen, danach nur ueber die Einstellungen
@Composable
fun rememberNotificationAccess(prefs: Prefs): NotificationAccess {
    val context = LocalContext.current
    var ok by remember { mutableStateOf(Notifier.allowed(context)) }
    LifecycleResumeEffect(Unit) {
        ok = Notifier.allowed(context)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        ok = Notifier.allowed(context)
    }
    return NotificationAccess(ok) { fromSettings ->
        if (!ok) {
            if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotifications) {
                prefs.askedNotifications = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (fromSettings) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                } catch (_: ActivityNotFoundException) {
                    // keine Einstellungsseite: nichts tun
                }
            }
        }
    }
}
