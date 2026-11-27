package solutions.xapient.hertzr

import android.content.Context
import android.os.Handler
import android.os.Looper
import solutions.xapient.aapps.core.ui.SharedPrefsKeyValues
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Settings

// Ziehen am Band schreibt nicht je Bild
private const val SAVE_DELAY_MS = 400L

fun Prefs(context: Context) = Prefs(SharedPrefsKeyValues(context.applicationContext, "hertzr"))

// Ein Generator pro Prozess, geteilt von Oberflaeche und Dienst.
// Schutz (Kopfhoerer, Telefonat) haengt am Store, nicht am Dienst: wirkt auch ohne Vordergrund
fun ToneStore.Companion.get(context: Context): ToneStore = current ?: synchronized(this) {
    current ?: Prefs(context).let { p ->
        val save = Debounced(SAVE_DELAY_MS) { s: Settings -> p.settings = s }
        ToneStore({ p.settings }, save::post, AudioEngine(), Freq.maxFor(AudioEngine.nativeRate()))
    }.also {
        install(it)
        AudioGuard(context, it)
    }
}

// Schreibt erst nach einer Ruhepause den letzten Wert (Hauptthread)
class Debounced<T>(private val delayMs: Long, private val write: (T) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: T? = null
    private val flush = Runnable { pending?.let(write); pending = null }

    fun post(v: T) {
        pending = v
        handler.removeCallbacks(flush)
        handler.postDelayed(flush, delayMs)
    }
}
