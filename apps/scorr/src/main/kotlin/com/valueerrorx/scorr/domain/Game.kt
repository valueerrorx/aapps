package com.valueerrorx.scorr.domain

// Ein Spieler bzw. Team; color = Index in die Palette
data class Player(val name: String, val color: Int)

// Verlauf als Liste von Aktionen; Punkte und Runden sind daraus abgeleitet
sealed interface Action {
    data class Score(val player: Int, val delta: Int) : Action
    data object EndRound : Action
}

enum class WinRule { MOST, FEWEST }

// Ganzes Spiel, unveraenderlich; lastAt = Zeitpunkt des letzten Zugs (nicht gespeichert)
data class Game(
    val players: List<Player>,
    val actions: List<Action> = emptyList(),
    val target: Int? = null,
    val rule: WinRule = WinRule.MOST,
    val lastAt: Long = Long.MIN_VALUE,
) {
    val scores: List<Int> by lazy {
        val s = IntArray(players.size)
        actions.forEach { if (it is Action.Score && it.player in s.indices) s[it.player] = clamp(s[it.player].toLong() + it.delta) }
        s.toList()
    }

    // Laufende Runde, ab 1
    val round: Int get() = actions.count { it == Action.EndRound } + 1

    val canUndo: Boolean get() = actions.isNotEmpty()

    // Punkteunterschied je Runde und Spieler, die laufende Runde zuletzt
    fun roundTable(): List<List<Int>> {
        val rows = mutableListOf(IntArray(players.size))
        actions.forEach { a ->
            when (a) {
                Action.EndRound -> rows += IntArray(players.size)
                is Action.Score -> if (a.player in players.indices) {
                    rows.last()[a.player] = clamp(rows.last()[a.player].toLong() + a.delta)
                }
            }
        }
        return rows.map { it.toList() }
    }

    // Ziel erreicht: Spiel vorbei
    val finished: Boolean get() = target != null && scores.any { it >= target }

    // Gewinner nach Spielende, sonst leer; Gleichstand = mehrere
    val winners: Set<Int> get() = if (finished) best() else emptySet()

    // Fuehrung waehrend des Spiels; leer, wenn alle gleich
    val leaders: Set<Int> get() = if (players.size < 2 || scores.distinct().size < 2) emptySet() else best()

    private fun best(): Set<Int> {
        val b = if (rule == WinRule.MOST) scores.max() else scores.min()
        return scores.indices.filter { scores[it] == b }.toSet()
    }

    // Letzter Zug dieses Spielers, solange er noch zusammengefasst wird
    fun burst(player: Int, now: Long): Int? {
        val last = actions.lastOrNull() as? Action.Score ?: return null
        return if (last.player == player && recent(now)) last.delta else null
    }

    private fun recent(now: Long) = lastAt != Long.MIN_VALUE && now - lastAt in 0 until MERGE_MS

    // Punkte buchen; schnelle Folge am selben Spieler wird ein Zug, Summe 0 loescht ihn
    fun score(player: Int, delta: Int, now: Long): Game {
        if (player !in players.indices || delta == 0) return this
        val last = actions.lastOrNull()
        if (last is Action.Score && last.player == player && recent(now)) {
            val sum = clamp(last.delta.toLong() + delta)
            val rest = actions.dropLast(1)
            return copy(actions = if (sum == 0) rest else rest + Action.Score(player, sum), lastAt = now)
        }
        return copy(actions = actions + Action.Score(player, clamp(delta.toLong())), lastAt = now)
    }

    fun endRound(): Game = copy(actions = actions + Action.EndRound, lastAt = Long.MIN_VALUE)

    fun undo(): Game = copy(actions = actions.dropLast(1), lastAt = Long.MIN_VALUE)

    fun newGame(): Game = copy(actions = emptyList(), lastAt = Long.MIN_VALUE)

    fun withTarget(t: Int?): Game = copy(target = t?.coerceIn(-MAX, MAX))

    fun withRule(r: WinRule): Game = copy(rule = r)

    fun rename(i: Int, name: String): Game = edit(i) { it.copy(name = clean(name)) }

    fun recolor(i: Int, color: Int): Game = edit(i) { it.copy(color = color.mod(COLORS)) }

    // Neuer Spieler mit der ersten freien Farbe
    fun addPlayer(): Game {
        if (players.size >= MAX_PLAYERS) return this
        val used = players.map { it.color }.toSet()
        val color = (0 until COLORS).firstOrNull { it !in used } ?: (players.size % COLORS)
        return copy(players = players + Player("", color), lastAt = Long.MIN_VALUE)
    }

    // Spieler samt seiner Zuege entfernen, spaetere Indizes ruecken nach
    fun removePlayer(i: Int): Game {
        if (players.size <= 1 || i !in players.indices) return this
        val acts = actions.mapNotNull { a ->
            when {
                a !is Action.Score -> a
                a.player == i -> null
                a.player > i -> a.copy(player = a.player - 1)
                else -> a
            }
        }
        return copy(players = players.filterIndexed { k, _ -> k != i }, actions = acts, lastAt = Long.MIN_VALUE)
    }

    fun hasActions(i: Int): Boolean = actions.any { it is Action.Score && it.player == i }

    private fun edit(i: Int, f: (Player) -> Player): Game =
        if (i !in players.indices) this else copy(players = players.toMutableList().also { it[i] = f(it[i]) })

    companion object {
        const val MAX_PLAYERS = 8
        const val COLORS = 8
        const val MAX_NAME = 24
        const val MAX = 999_999
        const val MERGE_MS = 2000L

        fun initial(): Game = Game(listOf(Player("", 0), Player("", 2)))

        fun clamp(v: Long): Int = v.coerceIn(-MAX.toLong(), MAX.toLong()).toInt()

        // Steuerzeichen raus, Zeilen zu Leerzeichen, Laenge begrenzt, kein halbes Emoji
        fun clean(text: String): String {
            val s = text.map { if (it == '\n' || it == '\r' || it == '\t') ' ' else it }
                .filter { !it.isISOControl() }
                .joinToString("")
                .take(MAX_NAME)
            return if (s.isNotEmpty() && s.last().isHighSurrogate()) s.dropLast(1) else s
        }
    }
}
