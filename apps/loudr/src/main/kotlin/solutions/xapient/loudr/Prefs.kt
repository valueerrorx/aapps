package solutions.xapient.loudr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.domain.TimeWeighting

data class Settings(
    val weighting: TimeWeighting = TimeWeighting.FAST,
    val offset: Float = 0f,
    val keepOn: Boolean = true,
)

// Einstellungen und ob schon nach dem Mikrofon gefragt wurde; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("loudr", Context.MODE_PRIVATE)

    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                weighting = TimeWeighting.entries.firstOrNull { it.name == sp.getString("weighting", null) } ?: d.weighting,
                offset = Calibration.clamp(sp.getFloat("offset", d.offset)),
                keepOn = sp.getBoolean("keepOn", d.keepOn),
            )
        }
        set(v) = sp.edit {
            putString("weighting", v.weighting.name)
            putFloat("offset", Calibration.clamp(v.offset))
            putBoolean("keepOn", v.keepOn)
        }

    var askedMic: Boolean
        get() = sp.getBoolean("askedMic", false)
        set(v) = sp.edit { putBoolean("askedMic", v) }

    // das System hat schon einmal eine Begruendung verlangt, also wurde wirklich abgelehnt
    var rationaleSeen: Boolean
        get() = sp.getBoolean("rationaleSeen", false)
        set(v) = sp.edit { putBoolean("rationaleSeen", v) }
}
