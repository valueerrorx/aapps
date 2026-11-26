package solutions.xapient.aapps.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun noInternetText(): String = stringResource(Res.string.aapps_promise_internet)

// Einstellungen einer App in SharedPreferences
class SharedPrefsKeyValues(context: Context, name: String) : KeyValues {
    private val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    override fun int(key: String, default: Int) = sp.getInt(key, default)
    override fun putInt(key: String, value: Int) = sp.edit().putInt(key, value).apply()
    override fun float(key: String, default: Float) = sp.getFloat(key, default)
    override fun putFloat(key: String, value: Float) = sp.edit().putFloat(key, value).apply()
    override fun bool(key: String, default: Boolean) = sp.getBoolean(key, default)
    override fun putBool(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
}
