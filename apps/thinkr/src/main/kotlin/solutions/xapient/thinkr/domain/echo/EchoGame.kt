package solutions.xapient.thinkr.domain.echo

import solutions.xapient.thinkr.domain.Bands
import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import solutions.xapient.thinkr.domain.Staircase
import kotlin.random.Random

// Stufen und Fristen an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object EchoRules {
    const val MAX_LEVEL = 20
    const val TWO_BACK = 10
    const val SIMILAR = 14
    const val STEP = 0.88
    const val MEMO_MS = 1200L
    const val GAP_MS = 120L
    const val RELAXED = 1.5
    const val POINTS = 50
    const val FAST_BONUS = 15
    const val DECK = 400

    // Spielart: 1-back (1–9), 2-back (10–13), 2-back mit aehnlichen Formen (14–20)
    val bands = Bands(listOf(1, TWO_BACK, SIMILAR), listOf(2400.0, 3000.0, 2800.0), MAX_LEVEL, STEP)

    fun modeStart(level: Int): Int = bands.modeStart(level)

    fun modeEnd(start: Int): Int = bands.modeEnd(start)

    fun nBack(start: Int): Int = if (start >= TWO_BACK) 2 else 1

    fun similar(start: Int): Boolean = start >= SIMILAR

    fun deadline(start: Int, level: Int): Double = bands.deadline(start, level)

    fun levelOf(start: Int, ms: Double): Int = bands.levelOf(start, ms)

    fun stairFor(start: Int, level: Int): Staircase = bands.stairFor(start, level)
}

// Letzte Auswertung fuer die Optik; said = null: Frist abgelaufen
data class EchoAnswer(val index: Int, val said: Boolean?, val correct: Boolean, val at: Long, val points: Int)

data class EchoState(
    override val round: Round,
    override val startLevel: Int,
    val modeStart: Int,
    val deck: Deck,
    val index: Int,
    val shownAt: Long,
    val stair: Staircase,
    val relaxed: Boolean,
    val last: EchoAnswer? = null,
) : Play {
    override val level: Int get() = EchoRules.levelOf(modeStart, stair.value)
    val nBack: Int get() = EchoRules.nBack(modeStart)
    val isMemo: Boolean get() = index < nBack
    val card: Glyph get() = deck.cards[index]
    private val scale: Double get() = if (relaxed) EchoRules.RELAXED else 1.0
    val deadlineMs: Long get() = (stair.value * scale).toLong()
    val memoMs: Long get() = (EchoRules.MEMO_MS * scale).toLong()

    // Wie lange die aktuelle Karte steht, bevor sie von selbst wechselt
    val dueAt: Long get() = shownAt + if (isMemo) memoMs else deadlineMs
}

// Echo: ist die Form gleich wie die vorige (ab Stufe 10: wie die vor zwei)? Eingabe true = gleich
object EchoGame : MiniGame<EchoState, Boolean> {
    // Aufwaermen ueberschreitet nie den Beginn der Spielart
    override fun startLevel(saved: Int): Int = EchoRules.bands.warmUp(saved)

    override fun start(seed: Long, level: Int, relaxed: Boolean): EchoState {
        val l = level.coerceIn(1, EchoRules.MAX_LEVEL)
        val m = EchoRules.modeStart(l)
        val deck = EchoDeck.deal(Random(seed), EchoRules.DECK, EchoRules.nBack(m), EchoRules.similar(m))
        return EchoState(Round(), l, m, deck, index = 0, shownAt = 0, stair = EchoRules.stairFor(m, l), relaxed = relaxed)
    }

    override fun tick(s: EchoState, t: Long): EchoState {
        var x = s.copy(round = s.round.at(t))
        // auch mehrere Wechsel nach einem langen Frame nachholen, aber nichts nach dem Rundenende
        while (x.dueAt <= t && x.dueAt < x.round.durationMs && x.index < x.deck.cards.lastIndex) {
            val due = x.dueAt
            x = if (x.isMemo) {
                x.copy(index = x.index + 1, shownAt = due + EchoRules.GAP_MS)
            } else {
                x.copy(
                    round = x.round.miss(),
                    stair = x.stair.miss(),
                    last = EchoAnswer(x.index, null, false, due, 0),
                    index = x.index + 1,
                    shownAt = due + EchoRules.GAP_MS,
                )
            }
        }
        // Stapel aufgebraucht: Runde endet (praktisch nie)
        if (x.index >= x.deck.cards.lastIndex) x = x.copy(round = x.round.at(x.round.durationMs))
        return x
    }

    override fun input(s: EchoState, input: Boolean, t: Long): EchoState {
        val x = tick(s, t)
        // Merk-Karte, Wechsel und Rundenende nehmen keine Antwort an
        if (x.round.isOver || x.isMemo || t < x.shownAt) return x
        val correct = input == x.deck.matches[x.index]
        val fast = (t - x.shownAt) * 2 < x.deadlineMs
        val round = if (correct) x.round.hit(EchoRules.POINTS + (if (fast) EchoRules.FAST_BONUS else 0)) else x.round.miss()
        return x.copy(
            round = round,
            stair = if (correct) x.stair.hit() else x.stair.miss(),
            last = EchoAnswer(x.index, input, correct, t, round.score - x.round.score),
            index = x.index + 1,
            shownAt = t + EchoRules.GAP_MS,
        )
    }
}
