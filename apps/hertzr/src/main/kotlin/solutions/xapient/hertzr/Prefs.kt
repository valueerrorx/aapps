package solutions.xapient.hertzr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.hertzr.domain.Settings

// Frequenz, Form, Pegel, Sweep und Einstellungen; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("hertzr", Context.MODE_PRIVATE)

    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                hz = sp.getFloat("hz", d.hz.toFloat()).toDouble(),
                wave = enumOr(sp.getString("wave", null), d.wave),
                levelDb = sp.getInt("levelDb", d.levelDb),
                mode = enumOr(sp.getString("mode", null), d.mode),
                sweepFrom = sp.getFloat("sweepFrom", d.sweepFrom.toFloat()).toDouble(),
                sweepTo = sp.getFloat("sweepTo", d.sweepTo.toFloat()).toDouble(),
                sweepSec = sp.getInt("sweepSec", d.sweepSec),
                output = enumOr(sp.getString("output", null), d.output),
                keepOn = sp.getBoolean("keepOn", d.keepOn),
            )
        }
        set(v) = sp.edit {
            putFloat("hz", v.hz.toFloat())
            putString("wave", v.wave.name)
            putInt("levelDb", v.levelDb)
            putString("mode", v.mode.name)
            putFloat("sweepFrom", v.sweepFrom.toFloat())
            putFloat("sweepTo", v.sweepTo.toFloat())
            putInt("sweepSec", v.sweepSec)
            putString("output", v.output.name)
            putBoolean("keepOn", v.keepOn)
        }

    // Benachrichtigungs-Recht nur einmal erfragen
    var askedNotify: Boolean
        get() = sp.getBoolean("askedNotify", false)
        set(v) = sp.edit { putBoolean("askedNotify", v) }

    private companion object {
        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: fallback
    }
}
