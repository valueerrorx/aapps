package solutions.xapient.scorr.domain

// Speicherformat: eine Zeile je Eintrag, Felder mit '|' (Name zuletzt, darf '|' enthalten)
object GameCodec {
    private const val VERSION = "scorr1"

    fun encode(g: Game): String = buildString {
        appendLine(VERSION)
        appendLine("T|${g.target ?: ""}")
        appendLine("R|${g.rule.name}")
        g.players.forEach { appendLine("P|${it.color}|${Game.clean(it.name)}") }
        g.actions.forEach {
            when (it) {
                is Action.Score -> appendLine("S|${it.player}|${it.delta}")
                Action.EndRound -> appendLine("E")
            }
        }
    }

    // Stand vor „Neues Spiel": gleiches Format mit den alten Zuegen; null = keiner
    fun encodeBefore(s: Session): String? = s.before?.let { encode(s.game.copy(actions = it)) }

    // alter Stand gilt nur, wenn die Spielerzahl noch passt
    fun decodeSession(game: String?, before: String?): Session {
        val g = decode(game)
        val b = before?.takeIf { it.isNotEmpty() }?.let { decode(it) }?.takeIf { it.players.size == g.players.size }
        return Session(g, b?.actions?.takeIf { it.isNotEmpty() })
    }

    // Tolerant: unbekannte oder kaputte Zeilen werden uebersprungen; ohne Spieler gibt es ein neues Spiel
    fun decode(text: String?): Game {
        if (text.isNullOrEmpty()) return Game.initial()
        val lines = text.lines()
        if (lines.firstOrNull() != VERSION) return Game.initial()
        var target: Int? = null
        var rule = WinRule.MOST
        val players = mutableListOf<Player>()
        val raw = mutableListOf<Action>()
        for (line in lines.drop(1)) {
            val f = line.split('|', limit = 3)
            when (f[0]) {
                "T" -> target = Game.validTarget(f.getOrNull(1)?.toIntOrNull())
                "R" -> rule = WinRule.entries.firstOrNull { it.name == f.getOrNull(1) } ?: WinRule.MOST
                "P" -> if (players.size < Game.MAX_PLAYERS) {
                    val color = f.getOrNull(1)?.toIntOrNull() ?: continue
                    players += Player(Game.clean(f.getOrNull(2) ?: ""), color.mod(Game.COLORS))
                }
                "S" -> {
                    val p = f.getOrNull(1)?.toIntOrNull() ?: continue
                    val d = f.getOrNull(2)?.toIntOrNull() ?: continue
                    if (d != 0) raw += Action.Score(p, Game.clamp(d.toLong()))
                }
                "E" -> raw += Action.EndRound
            }
        }
        if (players.isEmpty()) return Game.initial()
        val actions = raw.filter { it !is Action.Score || it.player in players.indices }
        return Game(players, actions, target, rule)
    }
}
