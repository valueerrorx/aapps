package solutions.xapient.roundr

import android.content.Context
import android.provider.Settings

// Ein Lauf pro Prozess, geteilt von Oberflaeche und Dienst
private object Shared {
    @Volatile var instance: RunStore? = null
}

fun RunStore.Companion.get(context: Context): RunStore = Shared.instance ?: synchronized(Shared) {
    Shared.instance ?: Prefs(context).let { p -> RunStore({ p.run }, { p.run = it }, boot = bootCount(context)) }.also { Shared.instance = it }
}

// Zaehler der Geraetestarts; -1 wenn nicht lesbar (dann bleibt nur die Uhr-Pruefung)
private fun bootCount(context: Context): Int = try {
    Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
} catch (_: Exception) {
    -1
}
