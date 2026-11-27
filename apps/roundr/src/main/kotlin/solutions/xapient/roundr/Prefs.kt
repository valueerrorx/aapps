package solutions.xapient.roundr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.roundr.domain.Codec
import solutions.xapient.roundr.domain.Presets
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Workout

// Training, Vorlagen, Lauf und Einstellungen; Backup ist aus
class Prefs(context: Context) : RoundrPrefs {
    private val sp = context.applicationContext.getSharedPreferences("roundr", Context.MODE_PRIVATE)

    override var workout: Workout
        get() = Codec.decode(sp.getString("workout", null)) ?: Presets.default
        set(v) = sp.edit { putString("workout", Codec.encode(v)) }

    override var saved: List<Workout>
        get() = Codec.decodeList(sp.getString("saved", null))
        set(v) = sp.edit { putString("saved", Codec.encodeList(v)) }

    // commit: der Lauf muss auch einen sofortigen Prozess-Tod ueberstehen
    var run: Run?
        get() = Codec.decodeRun(sp.getString("run", null))
        set(v) = sp.edit(commit = true) { if (v == null) remove("run") else putString("run", Codec.encodeRun(v)) }

    override var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit { putBoolean("sound", v) }

    override var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit { putBoolean("vibrate", v) }

    override var keepOn: Boolean
        get() = sp.getBoolean("keepOn", true)
        set(v) = sp.edit { putBoolean("keepOn", v) }

    // Benachrichtigungs-Recht nur einmal erfragen
    var askedNotify: Boolean
        get() = sp.getBoolean("askedNotify", false)
        set(v) = sp.edit { putBoolean("askedNotify", v) }
}
