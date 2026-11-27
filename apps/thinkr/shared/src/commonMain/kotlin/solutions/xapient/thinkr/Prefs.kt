package solutions.xapient.thinkr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.thinkr.domain.Codec
import solutions.xapient.thinkr.domain.Journal

// Fortschritt, Verlauf und Einstellungen; Backup ist aus
class Prefs(private val kv: KeyValues) {
    var journal: Journal
        get() = Codec.decode(kv.string("journal", null))
        set(v) = kv.putString("journal", Codec.encode(v))

    var haptics: Boolean
        get() = kv.bool("haptics", true)
        set(v) = kv.putBool("haptics", v)

    var sound: Boolean
        get() = kv.bool("sound", false)
        set(v) = kv.putBool("sound", v)

    var relaxed: Boolean
        get() = kv.bool("relaxed", false)
        set(v) = kv.putBool("relaxed", v)

    // Farbenblind-Modus: Tinte nur mit den vier sicheren Farben
    var safeColors: Boolean
        get() = kv.bool("safe_colors", false)
        set(v) = kv.putBool("safe_colors", v)
}
