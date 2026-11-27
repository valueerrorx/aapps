package solutions.xapient.tunr

import androidx.compose.ui.text.intl.Locale
import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.tunr.domain.Settings

// Einstellungen sofort gespeichert; Android: SharedPreferences "tunr" (Backup aus), iOS: UserDefaults
class Prefs(private val kv: KeyValues) {
    var settings: Settings
        get() {
            val d = Settings(notation = Settings.notationFor(Locale.current.language))
            return Settings(
                a4 = kv.int("a4", d.a4),
                notation = enumOr(kv.string("notation", null), d.notation),
                instrument = enumOr(kv.string("instrument", null), d.instrument),
                haptics = kv.bool("haptics", d.haptics),
            ).sane()
        }
        set(v) {
            kv.putInt("a4", v.a4)
            kv.putString("notation", v.notation.name)
            kv.putString("instrument", v.instrument.name)
            kv.putBool("haptics", v.haptics)
        }

    // Mikrofon schon einmal erfragt (nur einmal von selbst fragen)
    var askedMic: Boolean
        get() = kv.bool("askedMic", false)
        set(v) = kv.putBool("askedMic", v)

    // Anfrage kam ohne Dialog abgelehnt zurueck: dauerhaft gesperrt (nur Android)
    var micBlocked: Boolean
        get() = kv.bool("micBlocked", false)
        set(v) = kv.putBool("micBlocked", v)

    private companion object {
        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: fallback
    }
}
