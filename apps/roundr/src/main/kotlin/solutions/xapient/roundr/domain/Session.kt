package solutions.xapient.roundr.domain

// Laufzeit aus monotoner Uhr: verstrichen = Versatz + (jetzt - laeuftSeit); nichts wird aufsummiert, nichts driftet
data class Session(val offsetMs: Long = 0, val runningSince: Long? = null) {
    val running: Boolean get() = runningSince != null

    fun elapsed(now: Long): Long = offsetMs + (runningSince?.let { (now - it).coerceAtLeast(0) } ?: 0)

    fun pause(now: Long): Session = if (running) Session(elapsed(now), null) else this

    fun resume(now: Long): Session = if (running) this else Session(offsetMs, now)

    // Springen behaelt den Laufzustand
    fun seek(now: Long, toMs: Long): Session = Session(toMs.coerceAtLeast(0), if (running) now else null)

    // Monotone Uhr startet nach Geraete-Neustart bei 0: dann angehalten am letzten bekannten Stand
    fun restored(now: Long): Session = if (runningSince != null && runningSince > now) Session(offsetMs, null) else this

    companion object {
        fun start(now: Long) = Session(0, now)
    }
}

// Laufendes Training samt Kennung des Starts
data class Run(val workout: Workout, val session: Session, val id: Long) {
    val timeline: Timeline by lazy { Timeline(workout) }

    fun finished(now: Long): Boolean = session.elapsed(now) >= timeline.totalMs
}
