package solutions.xapient.hertzr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.hertzr.domain.Settings

// Frequenz, Form, Pegel, Sweep und Einstellungen; Android: SharedPreferences "hertzr" (Backup aus), iOS: UserDefaults
class Prefs(private val kv: KeyValues) {
    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                hz = kv.float("hz", d.hz.toFloat()).toDouble(),
                wave = enumOr(kv.string("wave", null), d.wave),
                levelDb = kv.int("levelDb", d.levelDb),
                mode = enumOr(kv.string("mode", null), d.mode),
                sweepFrom = kv.float("sweepFrom", d.sweepFrom.toFloat()).toDouble(),
                sweepTo = kv.float("sweepTo", d.sweepTo.toFloat()).toDouble(),
                sweepSec = kv.int("sweepSec", d.sweepSec),
                output = enumOr(kv.string("output", null), d.output),
                keepOn = kv.bool("keepOn", d.keepOn),
            )
        }
        set(v) {
            kv.putFloat("hz", v.hz.toFloat())
            kv.putString("wave", v.wave.name)
            kv.putInt("levelDb", v.levelDb)
            kv.putString("mode", v.mode.name)
            kv.putFloat("sweepFrom", v.sweepFrom.toFloat())
            kv.putFloat("sweepTo", v.sweepTo.toFloat())
            kv.putInt("sweepSec", v.sweepSec)
            kv.putString("output", v.output.name)
            kv.putBool("keepOn", v.keepOn)
        }

    // Benachrichtigungs-Recht endgueltig abgelehnt: nicht mehr fragen (weggetippt zaehlt nicht); nur Android
    var notifyBlocked: Boolean
        get() = kv.bool("notifyBlocked", false)
        set(v) = kv.putBool("notifyBlocked", v)

    private companion object {
        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: fallback
    }
}
