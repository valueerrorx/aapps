package solutions.xapient.focusr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.focusr.domain.Mode
import solutions.xapient.focusr.domain.Scale
import solutions.xapient.focusr.domain.Timer

// Timer und Einstellungen; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("focusr", Context.MODE_PRIVATE)

    // synchron schreiben: Receiver und Prozess-Tod muessen den Stand sehen
    var timer: Timer
        get() = Timer(
            setMs = sp.getLong("set", Timer.DEFAULT_MS),
            mode = Mode.entries.firstOrNull { it.name == sp.getString("mode", null) } ?: Mode.IDLE,
            endAt = sp.getLong("end", 0L),
            leftMs = sp.getLong("left", 0L),
        ).sanitized(System.currentTimeMillis())
        set(v) = sp.edit(commit = true) {
            putLong("set", v.setMs)
            putString("mode", v.mode.name)
            putLong("end", v.endAt)
            putLong("left", v.leftMs)
        }

    var scale: Scale
        get() = Scale.fromMinutes(sp.getInt("scale", Scale.DEFAULT.minutes))
        set(v) = sp.edit { putInt("scale", v.minutes) }

    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit { putBoolean("sound", v) }

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit { putBoolean("vibrate", v) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var keepOn: Boolean
        get() = sp.getBoolean("keepOn", true)
        set(v) = sp.edit { putBoolean("keepOn", v) }

    // Benachrichtigungs-Abfrage nur einmal von selbst
    var askedNotifications: Boolean
        get() = sp.getBoolean("askedNotif", false)
        set(v) = sp.edit { putBoolean("askedNotif", v) }
}
