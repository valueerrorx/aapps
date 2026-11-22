package com.valueerrorx.nordr

import android.content.Context
import androidx.core.content.edit
import com.valueerrorx.nordr.domain.NorthRef
import com.valueerrorx.nordr.domain.Region
import com.valueerrorx.nordr.domain.Regions
import java.util.TimeZone

// Einstellungen und gehaltener Kurs (magnetisch); Backup ist aus
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("nordr", Context.MODE_PRIVATE)

    // Region: gespeichert, sonst aus der Zeitzone, sonst Wien
    var region: Region
        get() = Regions.byId(sp.getString("region", null)) ?: guessedRegion ?: Regions.all.first()
        set(v) = sp.edit { putString("region", v.id) }

    // Startwert: geografisch, wenn die Zeitzone eine Region verraet
    var northRef: NorthRef
        get() = NorthRef.entries.getOrNull(sp.getInt("north", -1))
            ?: if (guessedRegion != null) NorthRef.TRUE else NorthRef.MAGNETIC
        set(v) = sp.edit { putInt("north", v.ordinal) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var course: Float?
        get() = sp.getFloat("course", Float.NaN).takeUnless { it.isNaN() }
        set(v) = sp.edit { putFloat("course", v ?: Float.NaN) }

    private val guessedRegion: Region? get() = Regions.forTimeZone(TimeZone.getDefault().id)
}
