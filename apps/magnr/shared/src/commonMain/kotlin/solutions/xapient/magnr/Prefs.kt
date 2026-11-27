package solutions.xapient.magnr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.magnr.domain.Sensitivity

data class Settings(
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val drift: Boolean = true,
    val sound: Boolean = false,
    val haptics: Boolean = true,
    val keepOn: Boolean = true,
)

// Einstellungen und ob die Einfuehrung gesehen wurde
class Prefs(private val kv: KeyValues) {
    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                sensitivity = Sensitivity.entries.firstOrNull { it.name == kv.string("sensitivity", null) } ?: d.sensitivity,
                drift = kv.bool("drift", d.drift),
                sound = kv.bool("sound", d.sound),
                haptics = kv.bool("haptics", d.haptics),
                keepOn = kv.bool("keepOn", d.keepOn),
            )
        }
        set(v) {
            kv.putString("sensitivity", v.sensitivity.name)
            kv.putBool("drift", v.drift)
            kv.putBool("sound", v.sound)
            kv.putBool("haptics", v.haptics)
            kv.putBool("keepOn", v.keepOn)
        }

    var introSeen: Boolean
        get() = kv.bool("introSeen", false)
        set(v) = kv.putBool("introSeen", v)
}
