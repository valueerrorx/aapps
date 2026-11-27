package solutions.xapient.roundr.domain

sealed interface Cue {
    // 3-2-1 vor dem Ende eines Abschnitts
    data class Count(val n: Int) : Cue
    data class Start(val phase: Phase) : Cue
    data object Done : Cue
}

data class CuePoint(val atMs: Long, val cue: Cue)

// Alle Ansagen eines Trainings als Zeitpunkte
object Cues {
    fun points(t: Timeline): List<CuePoint> {
        val out = ArrayList<CuePoint>()
        t.segments.firstOrNull()?.let { out += CuePoint(0, Cue.Start(it.phase)) }
        t.segments.forEachIndexed { i, s ->
            for (n in 3 downTo 1) {
                val at = s.endMs - n * 1000L
                if (at > s.startMs) out += CuePoint(at, Cue.Count(n))
            }
            val next = t.segments.getOrNull(i + 1)
            out += CuePoint(s.endMs, if (next == null) Cue.Done else Cue.Start(next.phase))
        }
        return out
    }

    // Ansagen im Fenster (from, to]; bei verspaetetem Tick zaehlt nur die juengste
    fun due(points: List<CuePoint>, fromMs: Long, toMs: Long): Cue? =
        points.lastOrNull { it.atMs > fromMs && it.atMs <= toMs }?.cue

    // Naechster Zeitpunkt nach e, null = keiner mehr
    fun nextAt(points: List<CuePoint>, e: Long): Long? = points.firstOrNull { it.atMs > e }?.atMs
}
