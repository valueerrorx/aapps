package solutions.xapient.levlr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.levlr.domain.AngleUnit
import solutions.xapient.levlr.domain.Bias
import solutions.xapient.levlr.domain.Calibration

// Einstellungen und Kalibrierung; Kalibrierung gilt nur fuer dieses Geraet (kein Backup)
class Prefs(private val kv: KeyValues) {
    var unit: AngleUnit
        get() = AngleUnit.entries.getOrNull(kv.int("unit", 0)) ?: AngleUnit.DEGREE
        set(v) = kv.putInt("unit", v.ordinal)

    var haptics: Boolean
        get() = kv.bool("haptics", true)
        set(v) = kv.putBool("haptics", v)

    var calibration: Calibration
        get() = Calibration(
            flat = Bias(kv.float("flatX", 0f), kv.float("flatY", 0f)),
            edge = Bias(kv.float("edgeX", 0f), kv.float("edgeY", 0f)),
        )
        set(c) {
            kv.putFloat("flatX", c.flat.x); kv.putFloat("flatY", c.flat.y)
            kv.putFloat("edgeX", c.edge.x); kv.putFloat("edgeY", c.edge.y)
        }
}
