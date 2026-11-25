package solutions.xapient.aapps.core.billing

import android.content.SharedPreferences

// Release: kein Debug-Schalter
internal object DebugTrial {
    @Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
    fun apply(cmd: String, prefs: SharedPreferences, now: Long): Boolean = false
}
