package com.valueerrorx.scorr.domain

// Spiel plus Zuege vor „Neues Spiel", damit Rueckgaengig auch das zuruecknimmt
data class Session(val game: Game, val before: List<Action>? = null) {
    val canUndo: Boolean get() = game.canUndo || before != null

    // leeres Spiel: nichts zu sichern, ein frueherer Stand bleibt erhalten
    fun newGame(): Session = if (game.actions.isEmpty()) this else Session(game.newGame(), game.actions)

    // nur die Zuege kommen zurueck; Namen, Farben, Ziel bleiben wie jetzt
    fun undo(rounds: Boolean = true): Session = when {
        game.canUndo -> Session(game.undo(rounds))
        before != null -> Session(game.copy(actions = before, lastAt = Long.MIN_VALUE))
        else -> this
    }

    // Spielzug oder Spieler hinzu/weg: alter Stand verfaellt
    fun play(g: Game): Session = Session(g)

    // Name, Farbe, Ziel, Regel: alter Stand bleibt
    fun edit(g: Game): Session = copy(game = g)
}
