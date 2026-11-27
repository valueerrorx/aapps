package solutions.xapient.nordr

import solutions.xapient.aapps.core.ui.KeyValues
import solutions.xapient.nordr.domain.NorthRef
import solutions.xapient.nordr.domain.Region
import solutions.xapient.nordr.domain.Regions

// Einstellungen und gehaltener Kurs (magnetisch); timeZone = Zeitzonen-ID des Geraets fuer den Startwert
class Prefs(private val kv: KeyValues, private val timeZone: String) {
    // Region: gespeichert, sonst aus der Zeitzone, sonst Wien
    var region: Region
        get() = Regions.byId(kv.string("region", null)) ?: guessedRegion ?: Regions.all.first()
        set(v) = kv.putString("region", v.id)

    // Startwert: geografisch, wenn die Zeitzone eine Region verraet
    var northRef: NorthRef
        get() = NorthRef.entries.getOrNull(kv.int("north", -1))
            ?: if (guessedRegion != null) NorthRef.TRUE else NorthRef.MAGNETIC
        set(v) = kv.putInt("north", v.ordinal)

    var haptics: Boolean
        get() = kv.bool("haptics", true)
        set(v) = kv.putBool("haptics", v)

    // kein Kurs = negativ (aeltere Android-Versionen speicherten NaN)
    var course: Float?
        get() = kv.float("course", Float.NaN).takeIf { it >= 0f }
        set(v) = kv.putFloat("course", v ?: -1f)

    private val guessedRegion: Region? get() = Regions.forTimeZone(timeZone)
}
