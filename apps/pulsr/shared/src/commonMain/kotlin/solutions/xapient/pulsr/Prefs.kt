package solutions.xapient.pulsr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.pulsr.domain.Settings

// Tempo, Takt und Einstellungen; Backup ist aus
class Prefs(private val kv: KeyValues) {
    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                bpm = kv.int("bpm", d.bpm),
                beats = kv.int("beats", d.beats),
                sub = enumOr(kv.string("sub", null), d.sub),
                sound = enumOr(kv.string("sound", null), d.sound),
                accent = kv.bool("accent", d.accent),
                keepOn = kv.bool("keepOn", d.keepOn),
            ).sane()
        }
        set(v) {
            kv.putInt("bpm", v.bpm)
            kv.putInt("beats", v.beats)
            kv.putString("sub", v.sub.name)
            kv.putString("sound", v.sound.name)
            kv.putBool("accent", v.accent)
            kv.putBool("keepOn", v.keepOn)
        }

    // Android: Benachrichtigungs-Recht endgueltig abgelehnt, nicht mehr fragen
    var notifyBlocked: Boolean
        get() = kv.bool("notifyBlocked", false)
        set(v) = kv.putBool("notifyBlocked", v)

    private companion object {
        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: fallback
    }
}
