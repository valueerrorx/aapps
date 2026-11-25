package solutions.xapient.thinkr.domain

// Spiele mit stabilem Speicher-Schluessel (nie Ordinal)
enum class GameId(val key: String) {
    ECHO("echo"),
    SWITCHES("switches"),
    STARS("stars"),
    INK("ink"),
    SWARM("swarm"),
    BOUNCE("bounce"),
    DROPS("drops"),
    GLANCE("glance"),
    SLALOM("slalom");

    companion object {
        fun of(key: String): GameId? = entries.firstOrNull { it.key == key }
    }
}

data class GameProgress(val level: Int = 1, val best: Int = 0, val plays: Int = 0)

// Gemaechlich hat eigene Stufe und eigenen Bestwert
data class ProgressKey(val id: GameId, val relaxed: Boolean)

data class RoundResult(
    val day: Int,
    val id: GameId,
    val score: Int,
    val level: Int,
    val accuracy: Int,
    val durationMs: Long,
    val relaxed: Boolean,
)

// Alles Gespeicherte: Fortschritt je Spiel, Rundenverlauf, Tage mit erledigtem Tagestraining
data class Journal(
    val progress: Map<ProgressKey, GameProgress> = emptyMap(),
    val history: List<RoundResult> = emptyList(),
    val dailyDone: Set<Int> = emptySet(),
) {
    fun progress(id: GameId, relaxed: Boolean): GameProgress = progress[ProgressKey(id, relaxed)] ?: GameProgress()

    fun playedOn(day: Int): Set<GameId> = history.asSequence().filter { it.day == day }.map { it.id }.toSet()

    // Tagesspiele, die heute schon gespielt wurden
    fun doneToday(day: Int, daily: List<GameId>): Set<GameId> = daily.toSet() intersect playedOn(day)

    // Runde eintragen; ist damit das Tagestraining komplett, zaehlt der Tag
    fun record(r: RoundResult, daily: List<GameId>): Journal {
        val key = ProgressKey(r.id, r.relaxed)
        val old = progress(r.id, r.relaxed)
        val p = progress + (key to GameProgress(r.level, maxOf(old.best, r.score), old.plays + 1))
        val minDay = Days.add(r.day, -KEEP_DAYS)
        val h = (history + r).filter { it.day > minDay }.takeLast(KEEP_ROUNDS)
        val next = copy(progress = p, history = h)
        val complete = daily.isNotEmpty() && next.doneToday(r.day, daily).size == daily.size
        val done = (if (complete) dailyDone + r.day else dailyDone).filter { it > minDay }.toSet()
        return next.copy(dailyDone = done)
    }

    // Stufe beider Spielarten zurueck auf 1, Bestwerte bleiben
    fun resetLevel(id: GameId): Journal =
        copy(progress = progress.mapValues { (k, v) -> if (k.id == id) v.copy(level = 1) else v })

    companion object {
        const val KEEP_ROUNDS = 1000
        const val KEEP_DAYS = 400
    }
}
