package solutions.xapient.scorr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.scorr.domain.GameCodec
import solutions.xapient.scorr.domain.Session

// Spielstand und Einstellungen; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("scorr", Context.MODE_PRIVATE)

    // Spiel plus Stand vor „Neues Spiel"
    var session: Session
        get() = GameCodec.decodeSession(sp.getString("game", null), sp.getString("before", null))
        set(v) = sp.edit {
            putString("game", GameCodec.encode(v.game))
            val b = GameCodec.encodeBefore(v)
            if (b == null) remove("before") else putString("before", b)
        }

    var rounds: Boolean
        get() = sp.getBoolean("rounds", true)
        set(v) = sp.edit { putBoolean("rounds", v) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var keepOn: Boolean
        get() = sp.getBoolean("keepOn", true)
        set(v) = sp.edit { putBoolean("keepOn", v) }
}
