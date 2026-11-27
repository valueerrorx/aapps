package solutions.xapient.spinr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.spinr.domain.Wheel
import solutions.xapient.spinr.domain.WheelCodec

// Raeder und Einstellungen
class Prefs(private val kv: KeyValues) {
    var wheels: List<Wheel>
        get() = WheelCodec.decode(kv.string("wheels", null))
        set(v) = kv.putString("wheels", WheelCodec.encode(v))

    var active: Int
        get() = kv.int("active", 0)
        set(v) = kv.putInt("active", v)

    var haptics: Boolean
        get() = kv.bool("haptics", true)
        set(v) = kv.putBool("haptics", v)

    var sound: Boolean
        get() = kv.bool("sound", false)
        set(v) = kv.putBool("sound", v)
}
