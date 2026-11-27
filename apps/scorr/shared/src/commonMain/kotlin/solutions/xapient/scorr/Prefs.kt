package solutions.xapient.scorr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.scorr.domain.GameCodec
import solutions.xapient.scorr.domain.Session

// Spielstand und Einstellungen; Backup ist aus (Android)
class Prefs(private val kv: KeyValues) {
    // Spiel plus Stand vor „Neues Spiel"; leerer Text = kein alter Stand
    var session: Session
        get() = GameCodec.decodeSession(kv.string("game", null), kv.string("before", null))
        set(v) {
            kv.putString("game", GameCodec.encode(v.game))
            kv.putString("before", GameCodec.encodeBefore(v) ?: "")
        }

    var haptics: Boolean
        get() = kv.bool("haptics", true)
        set(v) = kv.putBool("haptics", v)

    var keepOn: Boolean
        get() = kv.bool("keepOn", true)
        set(v) = kv.putBool("keepOn", v)
}
