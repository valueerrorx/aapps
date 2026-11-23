package solutions.xapient.dozr.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

// Ausblend-Kurve: weicher Cosinus von 1 auf 0; Lautstaerke-Stufen sind schon logarithmisch
object FadeCurve {
    fun level(progress: Double): Double {
        val p = progress.coerceIn(0.0, 1.0)
        return 0.5 * (1.0 + cos(PI * p))
    }

    fun index(orig: Int, progress: Double): Int = (orig * level(progress)).roundToInt().coerceIn(0, orig)
}

// Medienlautstaerke als Stufen (STREAM_MUSIC); Android-Teil liegt ausserhalb
interface Volume {
    val current: Int
    // Ausgabegeraet (z. B. Lautsprecher, Bluetooth); jedes hat eigene Stufen
    val device: Int
    fun set(index: Int)
}

// Was beim Prozess-Tod gesichert sein muss: Ausgangsstufe, zuletzt selbst gesetzte Stufe, Nutzer hat gedreht, Geraet (-1 = egal)
data class FadeMemo(val orig: Int, val last: Int, val touched: Boolean = false, val device: Int = -1)

// Blendet aus und stellt wieder her; fasst die Lautstaerke nur an, solange sie niemand anderer aendert
class Fader(private val volume: Volume, memo: FadeMemo? = null) {
    var memo: FadeMemo? = memo
        private set

    // Ausgabe gewechselt (Bluetooth weg): andere Stufen, weder ausblenden noch als Nutzer-Eingriff werten
    private fun elsewhere(m: FadeMemo) = m.device >= 0 && volume.device != m.device

    // true = memo geaendert (sichern)
    fun step(progress: Double): Boolean {
        val m = memo
        if (m == null) {
            val v = volume.current
            memo = FadeMemo(v, v, device = volume.device)
            apply(progress)
            return true
        }
        return apply(progress)
    }

    private fun apply(progress: Double): Boolean {
        val m = memo ?: return false
        if (m.touched || elsewhere(m)) return false
        val cur = volume.current
        if (cur != m.last) {
            memo = m.copy(touched = true)
            return true
        }
        val target = FadeCurve.index(m.orig, progress)
        if (target == cur) return false
        volume.set(target)
        // Geraet kann begrenzen: tatsaechliche Stufe merken
        memo = m.copy(last = volume.current)
        return true
    }

    // Ausgangsstufe zurueck, wenn unberuehrt; true = memo geaendert (sichern).
    // Bleibt gemerkt, solange ein anderes Geraet spielt oder das System die Stufe begrenzt (Gehoerschutz)
    fun restore(): Boolean {
        val m = memo ?: return false
        if (elsewhere(m)) return false
        if (m.touched || volume.current != m.last || m.last == m.orig) {
            memo = null
            return true
        }
        volume.set(m.orig)
        val now = volume.current
        memo = if (now == m.orig) null else m.copy(last = now)
        return true
    }

    val faded: Boolean get() = memo != null
}
