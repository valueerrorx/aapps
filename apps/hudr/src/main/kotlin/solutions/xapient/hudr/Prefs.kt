package solutions.xapient.hudr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.hudr.domain.Limit
import solutions.xapient.hudr.domain.SpeedUnit
import java.util.Locale

data class Settings(
    val unit: SpeedUnit = SpeedUnit.KMH,
    val limitOn: Boolean = false,
    val limit: Int = Limit.default(unit),
    val sound: Boolean = true,
    val keepOn: Boolean = true,
    val mirror: Boolean = true,
    val bright: Boolean = false,
) {
    // Schwelle nur, wenn eingeschaltet
    val activeLimit: Int? get() = if (limitOn) limit else null

    // Einheit wechseln, Schwelle mitnehmen
    fun withUnit(u: SpeedUnit): Settings = if (u == unit) this else copy(unit = u, limit = Limit.convert(limit, unit, u))
}

// Einstellungen und Rechte-Verlauf; Backup ist aus
class Prefs(context: Context, private val locale: () -> Locale = { Locale.getDefault() }) {
    private val sp = context.applicationContext.getSharedPreferences("hudr", Context.MODE_PRIVATE)

    var settings: Settings
        get() {
            val unit = SpeedUnit.entries.firstOrNull { it.name == sp.getString("unit", null) }
                ?: SpeedUnit.defaultFor(locale().country)
            val d = Settings(unit)
            return Settings(
                unit = unit,
                limitOn = sp.getBoolean("limitOn", d.limitOn),
                limit = Limit.clamp(sp.getInt("limit", d.limit)),
                sound = sp.getBoolean("sound", d.sound),
                keepOn = sp.getBoolean("keepOn", d.keepOn),
                mirror = sp.getBoolean("mirror", d.mirror),
                bright = sp.getBoolean("bright", d.bright),
            )
        }
        set(v) = sp.edit {
            putString("unit", v.unit.name)
            putBoolean("limitOn", v.limitOn)
            putInt("limit", Limit.clamp(v.limit))
            putBoolean("sound", v.sound)
            putBoolean("keepOn", v.keepOn)
            putBoolean("mirror", v.mirror)
            putBoolean("bright", v.bright)
        }

    var askedLocation: Boolean
        get() = sp.getBoolean("askedLocation", false)
        set(v) = sp.edit { putBoolean("askedLocation", v) }

    // das System hat schon einmal eine Begruendung verlangt, also wurde wirklich abgelehnt
    var rationaleSeen: Boolean
        get() = sp.getBoolean("rationaleSeen", false)
        set(v) = sp.edit { putBoolean("rationaleSeen", v) }
}
