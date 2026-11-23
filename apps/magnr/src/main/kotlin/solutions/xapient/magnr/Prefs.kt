package solutions.xapient.magnr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.magnr.domain.Sensitivity

data class Settings(
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val drift: Boolean = true,
    val sound: Boolean = false,
    val haptics: Boolean = true,
    val keepOn: Boolean = true,
)

// Einstellungen und ob die Einfuehrung gesehen wurde; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("magnr", Context.MODE_PRIVATE)

    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                sensitivity = Sensitivity.entries.firstOrNull { it.name == sp.getString("sensitivity", null) } ?: d.sensitivity,
                drift = sp.getBoolean("drift", d.drift),
                sound = sp.getBoolean("sound", d.sound),
                haptics = sp.getBoolean("haptics", d.haptics),
                keepOn = sp.getBoolean("keepOn", d.keepOn),
            )
        }
        set(v) = sp.edit {
            putString("sensitivity", v.sensitivity.name)
            putBoolean("drift", v.drift)
            putBoolean("sound", v.sound)
            putBoolean("haptics", v.haptics)
            putBoolean("keepOn", v.keepOn)
        }

    var introSeen: Boolean
        get() = sp.getBoolean("introSeen", false)
        set(v) = sp.edit { putBoolean("introSeen", v) }
}
