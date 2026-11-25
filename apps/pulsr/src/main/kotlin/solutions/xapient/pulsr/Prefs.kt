package solutions.xapient.pulsr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.pulsr.domain.Settings

// Tempo, Takt und Einstellungen; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("pulsr", Context.MODE_PRIVATE)

    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                bpm = sp.getInt("bpm", d.bpm),
                beats = sp.getInt("beats", d.beats),
                sub = enumOr(sp.getString("sub", null), d.sub),
                sound = enumOr(sp.getString("sound", null), d.sound),
                accent = sp.getBoolean("accent", d.accent),
                keepOn = sp.getBoolean("keepOn", d.keepOn),
            ).sane()
        }
        set(v) = sp.edit {
            putInt("bpm", v.bpm)
            putInt("beats", v.beats)
            putString("sub", v.sub.name)
            putString("sound", v.sound.name)
            putBoolean("accent", v.accent)
            putBoolean("keepOn", v.keepOn)
        }

    // Benachrichtigungs-Recht endgueltig abgelehnt: nicht mehr fragen
    var notifyBlocked: Boolean
        get() = sp.getBoolean("notifyBlocked", false)
        set(v) = sp.edit { putBoolean("notifyBlocked", v) }

    private companion object {
        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: fallback
    }
}
