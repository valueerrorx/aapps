package solutions.xapient.spinr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.spinr.domain.Wheel
import solutions.xapient.spinr.domain.WheelCodec

// Raeder und Einstellungen; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("spinr", Context.MODE_PRIVATE)

    var wheels: List<Wheel>
        get() = WheelCodec.decode(sp.getString("wheels", null))
        set(v) = sp.edit { putString("wheels", WheelCodec.encode(v)) }

    var active: Int
        get() = sp.getInt("active", 0)
        set(v) = sp.edit { putInt("active", v) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var sound: Boolean
        get() = sp.getBoolean("sound", false)
        set(v) = sp.edit { putBoolean("sound", v) }
}
