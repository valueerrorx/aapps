package solutions.xapient.thinkr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.thinkr.domain.Codec
import solutions.xapient.thinkr.domain.Journal

// Fortschritt, Verlauf und Einstellungen; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("thinkr", Context.MODE_PRIVATE)

    var journal: Journal
        get() = Codec.decode(sp.getString("journal", null))
        set(v) = sp.edit { putString("journal", Codec.encode(v)) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var sound: Boolean
        get() = sp.getBoolean("sound", false)
        set(v) = sp.edit { putBoolean("sound", v) }

    var relaxed: Boolean
        get() = sp.getBoolean("relaxed", false)
        set(v) = sp.edit { putBoolean("relaxed", v) }

    // Farbenblind-Modus: Tinte nur mit den vier sicheren Farben
    var safeColors: Boolean
        get() = sp.getBoolean("safe_colors", false)
        set(v) = sp.edit { putBoolean("safe_colors", v) }
}
