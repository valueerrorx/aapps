package solutions.xapient.focusr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import solutions.xapient.focusr.domain.Mode

// Alarm am Ende: Ton, Vibration, Benachrichtigung; veraltete Alarme werden ignoriert
class EndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != EndAlarm.ACTION) return
        val prefs = Prefs(context)
        val t = prefs.timer
        if (t.mode != Mode.RUNNING || t.endAt - System.currentTimeMillis() > EARLY_MS) return
        Notifier.cancel(context, Notifier.ID_RUNNING)
        if (!Visibility.visible) Notifier.showDone(context, t.setMs)
        if (prefs.vibrate) Alert.vibrate(context)
        if (prefs.sound) {
            val pending = goAsync()
            Alert.chime(context) { pending.finish() }
        }
    }

    companion object {
        // Toleranz fuer minimal fruehes Ausloesen
        const val EARLY_MS = 2000L
    }
}

// Ob die App gerade sichtbar ist; dann zeigt sie das Ende selbst
object Visibility {
    @Volatile var visible = false
}
