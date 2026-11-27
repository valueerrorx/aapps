package solutions.xapient.loudr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.domain.TimeWeighting

data class Settings(
    val weighting: TimeWeighting = TimeWeighting.FAST,
    val offset: Float = 0f,
    val keepOn: Boolean = true,
)

// Einstellungen und ob schon nach dem Mikrofon gefragt wurde; Android: SharedPreferences "loudr" (Backup aus), iOS: UserDefaults
class Prefs(private val kv: KeyValues) {
    var settings: Settings
        get() {
            val d = Settings()
            return Settings(
                weighting = TimeWeighting.entries.firstOrNull { it.name == kv.string("weighting", null) } ?: d.weighting,
                offset = Calibration.clamp(kv.float("offset", d.offset)),
                keepOn = kv.bool("keepOn", d.keepOn),
            )
        }
        set(v) {
            kv.putString("weighting", v.weighting.name)
            kv.putFloat("offset", Calibration.clamp(v.offset))
            kv.putBool("keepOn", v.keepOn)
        }

    var askedMic: Boolean
        get() = kv.bool("askedMic", false)
        set(v) = kv.putBool("askedMic", v)

    // das System hat schon einmal eine Begruendung verlangt, also wurde wirklich abgelehnt (nur Android)
    var rationaleSeen: Boolean
        get() = kv.bool("rationaleSeen", false)
        set(v) = kv.putBool("rationaleSeen", v)
}
