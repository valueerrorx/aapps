package solutions.xapient.levlr

import android.content.Context
import androidx.core.content.edit
import solutions.xapient.levlr.domain.AngleUnit
import solutions.xapient.levlr.domain.Bias
import solutions.xapient.levlr.domain.Calibration

// Einstellungen und Kalibrierung; Backup ist aus, Kalibrierung gilt nur fuer dieses Geraet
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("levlr", Context.MODE_PRIVATE)

    var unit: AngleUnit
        get() = AngleUnit.entries.getOrNull(sp.getInt("unit", 0)) ?: AngleUnit.DEGREE
        set(v) = sp.edit { putInt("unit", v.ordinal) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var calibration: Calibration
        get() = Calibration(
            flat = Bias(sp.getFloat("flatX", 0f), sp.getFloat("flatY", 0f)),
            edge = Bias(sp.getFloat("edgeX", 0f), sp.getFloat("edgeY", 0f)),
        )
        set(c) = sp.edit {
            putFloat("flatX", c.flat.x); putFloat("flatY", c.flat.y)
            putFloat("edgeX", c.edge.x); putFloat("edgeY", c.edge.y)
        }
}
