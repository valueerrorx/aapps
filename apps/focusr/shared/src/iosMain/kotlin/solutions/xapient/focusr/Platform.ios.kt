package solutions.xapient.focusr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import solutions.xapient.focusr.domain.Mode
import solutions.xapient.focusr.domain.Scale
import solutions.xapient.focusr.domain.Timer

// iOS hat keine oeffentliche Laufzeituhr inkl. Ruhezustand: Wanduhr (ein Uhrzeitwechsel verschiebt das Ende)
actual fun timerClock(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

// Fehler-Haptik; folgt wie alle iOS-Haptik der Systemeinstellung
@Composable
actual fun rememberReject(enabled: Boolean): () -> Unit {
    val on = rememberUpdatedState(enabled)
    val generator = remember { UINotificationFeedbackGenerator() }
    return remember(generator) {
        { if (on.value) generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError) }
    }
}

// Timer und Einstellungen in den UserDefaults; Zeiten auf der Wanduhr, daher gilt ein Lauf auch nach einem Neustart
class IosPrefs : FocusrPrefs {
    private val d = NSUserDefaults.standardUserDefaults
    private fun key(k: String) = "focusr.$k"
    private fun has(k: String) = d.objectForKey(key(k)) != null
    private fun long(k: String, def: Long): Long = if (has(k)) d.integerForKey(key(k)) else def
    private fun bool(k: String, def: Boolean): Boolean = if (has(k)) d.boolForKey(key(k)) else def

    override var timer: Timer
        get() {
            val now = timerClock()
            val t = Timer(
                setMs = long("set", Timer.DEFAULT_MS),
                mode = Mode.entries.firstOrNull { it.name == d.stringForKey(key("mode")) } ?: Mode.IDLE,
                endAt = long("end", 0L),
                leftMs = long("left", 0L),
                totalMs = long("total", 0L),
            )
            return t.restored(now, now, t.endAt, sameBoot = true)
        }
        set(v) {
            d.setInteger(v.setMs, key("set"))
            d.setObject(v.mode.name, key("mode"))
            d.setInteger(v.endAt, key("end"))
            d.setInteger(v.leftMs, key("left"))
            d.setInteger(v.totalMs, key("total"))
        }

    override var scale: Scale
        get() = Scale.fromMinutes(long("scale", Scale.DEFAULT.minutes.toLong()).toInt())
        set(v) = d.setInteger(v.minutes.toLong(), key("scale"))

    override var sound: Boolean
        get() = bool("sound", true)
        set(v) = d.setBool(v, key("sound"))

    override var vibrate: Boolean
        get() = bool("vibrate", true)
        set(v) = d.setBool(v, key("vibrate"))

    override var haptics: Boolean
        get() = bool("haptics", true)
        set(v) = d.setBool(v, key("haptics"))

    override var keepOn: Boolean
        get() = bool("keepOn", true)
        set(v) = d.setBool(v, key("keepOn"))

    // Benachrichtigungs-Abfrage nur einmal von selbst
    var askedNotifications: Boolean
        get() = bool("askedNotif", false)
        set(v) = d.setBool(v, key("askedNotif"))
}
