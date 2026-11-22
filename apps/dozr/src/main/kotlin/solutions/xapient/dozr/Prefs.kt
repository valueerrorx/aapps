package solutions.xapient.dozr

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import solutions.xapient.dozr.domain.FadeLength
import solutions.xapient.dozr.domain.FadeMemo
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.domain.Sleep

// Timer, Lautstaerke-Merker und Einstellungen; Backup ist aus
class Prefs(private val context: Context) {
    private val sp = context.getSharedPreferences("dozr", Context.MODE_PRIVATE)

    // Geraetestart erkennen: Laufzeituhr beginnt danach wieder bei 0
    fun boot(): Int = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    } catch (_: Exception) {
        -1
    }

    // synchron schreiben: Dienst und Prozess-Tod muessen den Stand sehen
    var sleep: Sleep
        get() = Sleep(
            setMin = sp.getInt("set", Sleep.DEFAULT_MIN),
            mode = Mode.entries.firstOrNull { it.name == sp.getString("mode", null) } ?: Mode.IDLE,
            startAt = sp.getLong("start", 0L),
            endAt = sp.getLong("end", 0L),
        )
        set(v) = sp.edit(commit = true) {
            putInt("set", v.setMin)
            putString("mode", v.mode.name)
            putLong("start", v.startAt)
            putLong("end", v.endAt)
            putInt("boot", boot())
        }

    val sameBoot: Boolean get() = sp.getInt("boot", -1) == boot()

    // Ausgangs- und zuletzt gesetzte Lautstaerke; ueberlebt Prozess-Tod mitten im Ausblenden
    var memo: FadeMemo?
        get() {
            val orig = sp.getInt("fadeOrig", -1)
            val last = sp.getInt("fadeLast", -1)
            if (orig < 0 || last < 0) return null
            return FadeMemo(orig, last, sp.getBoolean("fadeTouched", false))
        }
        set(v) = sp.edit(commit = true) {
            putInt("fadeOrig", v?.orig ?: -1)
            putInt("fadeLast", v?.last ?: -1)
            putBoolean("fadeTouched", v?.touched ?: false)
        }

    var fade: FadeLength
        get() = FadeLength.of(sp.getString("fade", null))
        set(v) = sp.edit { putString("fade", v.name) }

    var shake: Boolean
        get() = sp.getBoolean("shake", true)
        set(v) = sp.edit { putBoolean("shake", v) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    // Zweite Ablehnung erkannt: System fragt nicht mehr, nur noch Systemeinstellung
    var notifyBlocked: Boolean
        get() = sp.getBoolean("notifyBlocked", false)
        set(v) = sp.edit { putBoolean("notifyBlocked", v) }
}
