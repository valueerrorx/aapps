package solutions.xapient.roundr

import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import solutions.xapient.roundr.domain.Codec
import solutions.xapient.roundr.domain.Presets
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Workout

// iOS hat keine oeffentliche Laufzeituhr inkl. Ruhezustand: Wanduhr (ein Uhrzeitwechsel verschiebt den Lauf)
actual fun runClock(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

// Training, Vorlagen, Lauf und Einstellungen in den UserDefaults
class IosPrefs : RoundrPrefs {
    private val d = NSUserDefaults.standardUserDefaults
    private fun key(k: String) = "roundr.$k"
    private fun bool(k: String, def: Boolean): Boolean = if (d.objectForKey(key(k)) != null) d.boolForKey(key(k)) else def

    override var workout: Workout
        get() = Codec.decode(d.stringForKey(key("workout"))) ?: Presets.default
        set(v) = d.setObject(Codec.encode(v), key("workout"))

    override var saved: List<Workout>
        get() = Codec.decodeList(d.stringForKey(key("saved")))
        set(v) = d.setObject(Codec.encodeList(v), key("saved"))

    var run: Run?
        get() = Codec.decodeRun(d.stringForKey(key("run")))
        set(v) = if (v == null) d.removeObjectForKey(key("run")) else d.setObject(Codec.encodeRun(v), key("run"))

    override var sound: Boolean
        get() = bool("sound", true)
        set(v) = d.setBool(v, key("sound"))

    override var vibrate: Boolean
        get() = bool("vibrate", true)
        set(v) = d.setBool(v, key("vibrate"))

    override var keepOn: Boolean
        get() = bool("keepOn", true)
        set(v) = d.setBool(v, key("keepOn"))

    // Mitteilungs-Abfrage nur einmal von selbst
    var askedNotify: Boolean
        get() = bool("askedNotify", false)
        set(v) = d.setBool(v, key("askedNotify"))
}
