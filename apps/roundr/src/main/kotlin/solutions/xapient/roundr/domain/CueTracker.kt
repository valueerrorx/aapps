package solutions.xapient.roundr.domain

// Buchfuehrung, welche Ansagen schon gespielt sind; e = verstrichene Zeit beim Anlegen
class CueTracker(private val points: List<CuePoint>, e: Long) {
    // Ansagen kommen aus dem Fenster (last, e]; frischer Start nimmt das Startsignal mit, Prozess-Neustart holt nichts nach
    var last: Long = if (e < FRESH_MS) -1 else e
        private set

    // Zustandswechsel des Laufs (Anhalten, Weiter, Springen); offset = Versatz der Sitzung = Sprungziel
    fun onSync(e: Long, running: Boolean, offset: Long) {
        // zurueckgesprungen: Startsignal des Ziel-Abschnitts spielen
        if (e < last) last = offset - 1
        // angehalten: nichts nachholen; steht man auf einem Abschnittsbeginn (Sprung), kommt dessen Signal beim Weiter
        if (!running) last = if (startsAt(e)) e - 1 else e
    }

    // Faellige Ansage seit dem letzten Tick, bei Verspaetung nur die juengste
    fun onTick(e: Long): Cue? {
        val c = Cues.due(points, last, e)
        last = maxOf(last, e)
        return c
    }

    fun nextAt(e: Long): Long? = Cues.nextAt(points, e)

    private fun startsAt(e: Long) = points.any { it.atMs == e && it.cue !is Cue.Count }

    companion object {
        const val FRESH_MS = 1000L
    }
}
