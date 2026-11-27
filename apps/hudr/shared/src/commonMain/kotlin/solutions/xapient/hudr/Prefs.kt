package solutions.xapient.hudr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.hudr.domain.Limit
import solutions.xapient.hudr.domain.SpeedUnit

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

// Einstellungen und Rechte-Verlauf; country = Laendercode der Sprache fuer die Start-Einheit
class Prefs(private val kv: KeyValues, private val country: () -> String) {
    var settings: Settings
        get() {
            val unit = SpeedUnit.entries.firstOrNull { it.name == kv.string("unit", null) }
                ?: SpeedUnit.defaultFor(country())
            val d = Settings(unit)
            return Settings(
                unit = unit,
                limitOn = kv.bool("limitOn", d.limitOn),
                limit = Limit.clamp(kv.int("limit", d.limit)),
                sound = kv.bool("sound", d.sound),
                keepOn = kv.bool("keepOn", d.keepOn),
                mirror = kv.bool("mirror", d.mirror),
                bright = kv.bool("bright", d.bright),
            )
        }
        set(v) {
            kv.putString("unit", v.unit.name)
            kv.putBool("limitOn", v.limitOn)
            kv.putInt("limit", Limit.clamp(v.limit))
            kv.putBool("sound", v.sound)
            kv.putBool("keepOn", v.keepOn)
            kv.putBool("mirror", v.mirror)
            kv.putBool("bright", v.bright)
        }

    var askedLocation: Boolean
        get() = kv.bool("askedLocation", false)
        set(v) = kv.putBool("askedLocation", v)

    // Upgrade auf genau kam ohne Dialog zurueck: nur noch ueber die Einstellungen
    var upgradeRefused: Boolean
        get() = kv.bool("upgradeRefused", false)
        set(v) = kv.putBool("upgradeRefused", v)

    // das System hat schon einmal eine Begruendung verlangt, also wurde wirklich abgelehnt
    var rationaleSeen: Boolean
        get() = kv.bool("rationaleSeen", false)
        set(v) = kv.putBool("rationaleSeen", v)
}
