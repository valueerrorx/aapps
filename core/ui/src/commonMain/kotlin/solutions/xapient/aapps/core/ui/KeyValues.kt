package solutions.xapient.aapps.core.ui

/** Einfacher Einstellungs-Speicher; Android: SharedPreferences, iOS: UserDefaults */
interface KeyValues {
    fun int(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun float(key: String, default: Float): Float
    fun putFloat(key: String, value: Float)
    fun bool(key: String, default: Boolean): Boolean
    fun putBool(key: String, value: Boolean)
    fun string(key: String, default: String?): String?
    fun putString(key: String, value: String)
}
