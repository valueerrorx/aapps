package solutions.xapient.roundr.domain

object TimeFormat {
    // Restsekunden aufgerundet: 19,01 s zeigt 20, erst bei 0 ist der Abschnitt vorbei
    fun ceilSec(ms: Long): Int = ((ms.coerceAtLeast(0) + 999) / 1000).toInt()

    // Grosse Anzeige: unter 1 min nur Sekunden
    fun big(sec: Int): String = if (sec < 60) sec.toString() else clock(sec)

    // m:ss bzw. h:mm:ss
    fun clock(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        val h = s / 3600
        val m = s / 60 % 60
        val r = s % 60
        return if (h > 0) "$h:${two(m)}:${two(r)}" else "$m:${two(r)}"
    }

    // ohne Locale: immer lateinische Ziffern
    private fun two(n: Int): String = n.toString().padStart(2, '0')
}
