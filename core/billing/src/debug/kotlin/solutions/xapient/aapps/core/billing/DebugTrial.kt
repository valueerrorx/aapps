package solutions.xapient.aapps.core.billing

import android.content.SharedPreferences

// Nur im Debug-Build: Testphase sofort ablaufen lassen oder neu starten
internal object DebugTrial {
    fun apply(cmd: String, prefs: SharedPreferences, now: Long): Boolean {
        when (cmd) {
            "expire" -> prefs.edit().putLong(TrialGate.KEY_FIRST, now - TrialPolicy.TRIAL_MS).apply()
            "reset" -> prefs.edit().putLong(TrialGate.KEY_FIRST, now).putBoolean(TrialGate.KEY_PURCHASED, false).apply()
            else -> return false
        }
        return true
    }
}
