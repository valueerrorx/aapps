package solutions.xapient.focusr.domain

// Gespeicherter Modus; DONE ergibt sich erst aus der Uhr
enum class Mode { IDLE, RUNNING, PAUSED }

enum class Phase { IDLE, RUNNING, PAUSED, DONE }

// Ein Timer: alle Zeiten in ms auf der Laufzeituhr (seit Geraetestart, unberuehrt von Uhrzeitwechseln)
data class Timer(
    val setMs: Long = DEFAULT_MS,
    val mode: Mode = Mode.IDLE,
    val endAt: Long = 0L,
    val leftMs: Long = 0L,
    // Laufdauer bis zum Ende ohne Pausen, Verstellen eingerechnet (fuer "Zeit um")
    val totalMs: Long = 0L,
) {
    fun phase(now: Long): Phase = when (mode) {
        Mode.IDLE -> Phase.IDLE
        Mode.PAUSED -> Phase.PAUSED
        Mode.RUNNING -> if (now >= endAt) Phase.DONE else Phase.RUNNING
    }

    fun remaining(now: Long): Long = when (mode) {
        Mode.IDLE -> setMs
        Mode.PAUSED -> leftMs
        Mode.RUNNING -> (endAt - now).coerceAtLeast(0L)
    }

    // Laeuft und ist noch nicht abgelaufen: nur dann braucht es einen Alarm
    fun alarmAt(now: Long): Long? = if (phase(now) == Phase.RUNNING) endAt else null

    // START, WEITER und NOCHMAL; mit 0 passiert nichts
    fun start(now: Long): Timer = when (phase(now)) {
        Phase.IDLE, Phase.DONE -> if (setMs <= 0) this else copy(mode = Mode.RUNNING, endAt = now + setMs, leftMs = 0, totalMs = setMs)
        Phase.PAUSED -> copy(mode = Mode.RUNNING, endAt = now + leftMs, leftMs = 0)
        Phase.RUNNING -> this
    }

    fun pause(now: Long): Timer =
        if (phase(now) == Phase.RUNNING) copy(mode = Mode.PAUSED, leftMs = endAt - now, endAt = 0) else this

    fun reset(): Timer = copy(mode = Mode.IDLE, endAt = 0, leftMs = 0, totalMs = 0)

    fun canStart(now: Long): Boolean = when (phase(now)) {
        Phase.PAUSED -> true
        Phase.RUNNING -> false
        else -> setMs > 0
    }

    // Ergebnis des Ziehens: wird auch die neue Referenz fuer NOCHMAL
    fun set(ms: Long, now: Long): Timer {
        val v = ms.coerceIn(0L, MAX_MS)
        if (v == 0L) return Timer(setMs = 0)
        return when (phase(now)) {
            Phase.IDLE, Phase.DONE -> Timer(setMs = v)
            Phase.RUNNING -> copy(setMs = v, endAt = now + v, totalMs = totalMs - remaining(now) + v)
            Phase.PAUSED -> copy(setMs = v, leftMs = v, totalMs = totalMs - leftMs + v)
        }
    }

    // Skala gewechselt: eingestellte Zeit begrenzen, laufender Timer bleibt
    fun limitTo(scale: Scale): Timer = if (setMs > scale.ms) copy(setMs = scale.ms) else this

    // Laden: im selben Geraetestart gilt die Laufzeituhr, nach einem Neustart rechnet die Wanduhr das Ende um
    fun restored(now: Long, wallNow: Long, endWall: Long, sameBoot: Boolean): Timer {
        val moved = mode == Mode.RUNNING && endWall > 0 && (!sameBoot || endAt - now > MAX_MS + SLACK_MS)
        return (if (moved) copy(endAt = now + (endWall - wallNow)) else this).sanitized(now)
    }

    // Kaputte Daten: Werte begrenzen; ein Lauf bleibt ein Lauf (schlimmstenfalls "Zeit um")
    fun sanitized(now: Long): Timer {
        val s = setMs.coerceIn(0L, MAX_MS)
        val total = totalMs.coerceIn(0L, MAX_TOTAL_MS)
        return when (mode) {
            Mode.IDLE -> Timer(setMs = s)
            Mode.PAUSED -> if (leftMs in 1..MAX_MS) Timer(s, Mode.PAUSED, 0, leftMs, total) else Timer(setMs = s)
            Mode.RUNNING -> Timer(s, Mode.RUNNING, endAt.coerceAtMost(now + MAX_MS), 0, total)
        }
    }

    companion object {
        const val MINUTE = 60_000L
        const val DEFAULT_MS = 25 * MINUTE
        const val MAX_MS = 120 * MINUTE
        private const val SLACK_MS = MINUTE
        private const val MAX_TOTAL_MS = 24 * 60 * MINUTE
    }
}
