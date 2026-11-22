package solutions.xapient.roundr.domain

// Laufzeit aus monotoner Uhr: verstrichen = Versatz + (jetzt - laeuftSeit); nichts wird aufsummiert, nichts driftet
data class Session(val offsetMs: Long = 0, val runningSince: Long? = null) {
    val running: Boolean get() = runningSince != null

    fun elapsed(now: Long): Long = offsetMs + (runningSince?.let { (now - it).coerceAtLeast(0) } ?: 0)

    fun pause(now: Long): Session = if (running) Session(elapsed(now), null) else this

    fun resume(now: Long): Session = if (running) this else Session(offsetMs, now)

    // Springen behaelt den Laufzustand
    fun seek(now: Long, toMs: Long): Session = Session(toMs.coerceAtLeast(0), if (running) now else null)

    // Gleiche Laufzeit, Versatz auf jetzt vorgezogen: Checkpoint fuer einen Geraete-Neustart
    fun rebased(now: Long): Session = if (running) Session(elapsed(now), now) else this

    companion object {
        fun start(now: Long) = Session(0, now)
    }
}

// Laufendes Training samt Kennung des Starts und des Geraetestarts (-1 = unbekannt)
data class Run(val workout: Workout, val session: Session, val id: Long, val boot: Int = -1) {
    val timeline: Timeline by lazy { Timeline(workout) }

    fun finished(now: Long): Boolean = session.elapsed(now) >= timeline.totalMs

    // Nach Geraete-Neustart gilt die monotone Uhr nicht mehr: angehalten am letzten Checkpoint
    fun restored(now: Long, currentBoot: Int): Run {
        val since = session.runningSince ?: return copy(boot = currentBoot)
        val rebooted = boot != currentBoot || since > now
        return copy(session = if (rebooted) Session(session.offsetMs, null) else session, boot = currentBoot)
    }
}
