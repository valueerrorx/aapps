package solutions.xapient.thinkr.domain

// Speicherformat: Kopfzeile, dann Zeilen mit „|"; kaputte Zeilen werden uebersprungen
object Codec {
    const val HEADER = "thinkr1"
    private const val MAX_LEVEL = 99

    fun encode(j: Journal): String = buildString {
        append(HEADER)
        j.progress.entries.sortedWith(compareBy({ it.key.id.ordinal }, { it.key.relaxed })).forEach { (k, p) ->
            append("\nP|").append(k.id.key).append('|').append(flag(k.relaxed))
            append('|').append(p.level).append('|').append(p.best).append('|').append(p.plays)
        }
        j.history.forEach { r ->
            append("\nR|").append(r.day).append('|').append(r.id.key).append('|').append(r.score)
            append('|').append(r.level).append('|').append(r.accuracy).append('|').append(r.durationMs)
            append('|').append(flag(r.relaxed))
        }
        j.dailyDone.sorted().forEach { append("\nD|").append(it) }
    }

    fun decode(text: String?): Journal {
        val lines = text?.lines() ?: return Journal()
        if (lines.firstOrNull() != HEADER) return Journal()
        val progress = LinkedHashMap<ProgressKey, GameProgress>()
        val history = ArrayList<RoundResult>()
        val done = HashSet<Int>()
        for (line in lines.drop(1)) {
            val f = line.split('|')
            when (f[0]) {
                "P" -> progress(f)?.let { (k, p) -> progress[k] = p }
                "R" -> result(f)?.let(history::add)
                "D" -> if (f.size == 2) f[1].toIntOrNull()?.takeIf(Days::valid)?.let(done::add)
            }
        }
        return Journal(progress, history.takeLast(Journal.KEEP_ROUNDS), done)
    }

    private fun flag(b: Boolean) = if (b) "1" else "0"

    private fun bool(s: String): Boolean? = when (s) {
        "0" -> false
        "1" -> true
        else -> null
    }

    private fun progress(f: List<String>): Pair<ProgressKey, GameProgress>? {
        if (f.size != 6) return null
        val id = GameId.of(f[1]) ?: return null
        val relaxed = bool(f[2]) ?: return null
        val level = f[3].toIntOrNull()?.takeIf { it in 1..MAX_LEVEL } ?: return null
        val best = f[4].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val plays = f[5].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        return ProgressKey(id, relaxed) to GameProgress(level, best, plays)
    }

    private fun result(f: List<String>): RoundResult? {
        if (f.size != 8) return null
        val day = f[1].toIntOrNull()?.takeIf(Days::valid) ?: return null
        val id = GameId.of(f[2]) ?: return null
        val score = f[3].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val level = f[4].toIntOrNull()?.takeIf { it in 1..MAX_LEVEL } ?: return null
        val acc = f[5].toIntOrNull()?.takeIf { it in 0..100 } ?: return null
        val dur = f[6].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val relaxed = bool(f[7]) ?: return null
        return RoundResult(day, id, score, level, acc, dur, relaxed)
    }
}
