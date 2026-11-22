package solutions.xapient.tunr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.tunr.domain.Settings

// Einstellungen sofort gespeichert; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("tunr", Context.MODE_PRIVATE)

    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                a4 = sp.getInt("a4", d.a4),
                notation = enumOr(sp.getString("notation", null), d.notation),
                instrument = enumOr(sp.getString("instrument", null), d.instrument),
                haptics = sp.getBoolean("haptics", d.haptics),
            ).sane()
        }
        set(v) = sp.edit {
            putInt("a4", v.a4)
            putString("notation", v.notation.name)
            putString("instrument", v.instrument.name)
            putBoolean("haptics", v.haptics)
        }

    // Mikrofon schon einmal erfragt (fuer "dauerhaft abgelehnt")
    var askedMic: Boolean
        get() = sp.getBoolean("askedMic", false)
        set(v) = sp.edit { putBoolean("askedMic", v) }

    private companion object {
        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: fallback
    }
}
