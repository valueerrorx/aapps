package solutions.xapient.sleepr

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Anzeige-Einstellungen; ändern nur, was sichtbar ist, nie den Klang */
class Display(private val p: SharedPreferences) {
    constructor(ctx: Context) : this(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    var showWaves: Boolean
        get() = p.getBoolean(KEY_WAVES, true)
        set(v) = p.edit { putBoolean(KEY_WAVES, v) }

    var showMixes: Boolean
        get() = p.getBoolean(KEY_MIXES, true)
        set(v) = p.edit { putBoolean(KEY_MIXES, v) }

    companion object {
        const val PREFS = "app"
        const val KEY_WAVES = "showWaves"
        const val KEY_MIXES = "showMixes"

        // Wellenlängen-Regler nur bei aktiven Wellen und eingeblendeten Wellen-Schaltern
        fun waveBoxVisible(waveOn: Boolean, showWaves: Boolean) = waveOn && showWaves
    }
}
