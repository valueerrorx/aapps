package solutions.xapient.roundr.domain

enum class Phase { PREP, WORK, REST, SET_REST }

// Ein Abschnitt; Zeiten in ms ab Trainingsbeginn
data class Segment(val phase: Phase, val startMs: Long, val durationMs: Long, val round: Int, val set: Int) {
    val endMs: Long get() = startMs + durationMs
}

// Wo man gerade steht
data class Position(val index: Int, val segment: Segment, val remainingMs: Long) {
    // Anteil, der in diesem Abschnitt noch uebrig ist (1 = gerade begonnen)
    val fraction: Float get() = (remainingMs.toFloat() / segment.durationMs).coerceIn(0f, 1f)
}

// Ganze Abfolge eines Trainings; reine Rechnung, keine Uhr
class Timeline(val workout: Workout) {
    val segments: List<Segment>
    val totalMs: Long

    init {
        val w = workout.clamped()
        val list = ArrayList<Segment>()
        var t = 0L
        fun add(p: Phase, sec: Int, round: Int, set: Int) {
            if (sec <= 0) return
            list += Segment(p, t, sec * 1000L, round, set)
            t += sec * 1000L
        }
        add(Phase.PREP, w.prepSec, 1, 1)
        for (set in 1..w.sets) {
            for (r in 1..w.rounds) {
                add(Phase.WORK, w.workSec, r, set)
                when {
                    r < w.rounds -> add(Phase.REST, w.restSec, r, set)
                    set < w.sets && w.setRestSec > 0 -> add(Phase.SET_REST, w.setRestSec, r, set)
                    set < w.sets -> add(Phase.REST, w.restSec, r, set)
                }
            }
        }
        segments = list
        totalMs = t
    }

    val rounds: Int get() = workout.clamped().rounds
    val sets: Int get() = workout.clamped().sets

    // null = fertig
    fun at(elapsedMs: Long): Position? {
        if (elapsedMs >= totalMs) return null
        val e = elapsedMs.coerceAtLeast(0)
        // binaere Suche: letzter Abschnitt mit start <= e
        var lo = 0
        var hi = segments.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (segments[mid].startMs <= e) lo = mid else hi = mid - 1
        }
        val s = segments[lo]
        return Position(lo, s, s.endMs - e)
    }

    fun next(index: Int): Segment? = segments.getOrNull(index + 1)

    // Ziel fuer „weiter": Anfang des naechsten Abschnitts bzw. Ende
    fun skipTarget(elapsedMs: Long): Long {
        val p = at(elapsedMs) ?: return totalMs
        return p.segment.endMs
    }

    // Ziel fuer „zurueck": Anfang dieses Abschnitts, in den ersten 2 s der vorige
    fun backTarget(elapsedMs: Long): Long {
        val p = at(elapsedMs) ?: return segments.lastOrNull()?.startMs ?: 0
        val into = elapsedMs - p.segment.startMs
        return if (into > BACK_GRACE_MS || p.index == 0) p.segment.startMs else segments[p.index - 1].startMs
    }

    companion object {
        const val BACK_GRACE_MS = 2000L
    }
}
