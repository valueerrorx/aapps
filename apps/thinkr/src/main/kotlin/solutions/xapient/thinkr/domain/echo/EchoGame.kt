package solutions.xapient.thinkr.domain.echo

import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import solutions.xapient.thinkr.domain.Staircase
import kotlin.math.min
import kotlin.math.pow
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
    fun modeStart(level: Int): Int = when {
        level >= SIMILAR -> SIMILAR
        level >= TWO_BACK -> TWO_BACK
        else -> 1
    }

    fun modeEnd(start: Int): Int = when (start) {
        SIMILAR -> MAX_LEVEL
        TWO_BACK -> SIMILAR - 1
        else -> TWO_BACK - 1
    }

    fun nBack(start: Int): Int = if (start >= TWO_BACK) 2 else 1

    fun similar(start: Int): Boolean = start >= SIMILAR

    private fun base(start: Int): Double = when (start) {
        SIMILAR -> 2800.0
        TWO_BACK -> 3000.0
        else -> 2400.0
    }

    // Frist zu Beginn einer Stufe (Band-Obergrenze) innerhalb der Spielart ab start
    fun deadline(start: Int, level: Int): Double = base(start) * STEP.pow(level - start)

    // Stufe zur aktuellen Frist; hoechstens eine ueber das Ende bzw. eine unter den Beginn der Spielart
    fun levelOf(start: Int, ms: Double): Int {
        val top = min(modeEnd(start) + 1, MAX_LEVEL)
        var level = (start - 1).coerceAtLeast(1)
        for (x in start..top) if (deadline(start, x) >= ms - 1e-6) level = x
        return level
    }

    fun stairFor(start: Int, level: Int): Staircase {
        val top = min(modeEnd(start) + 1, MAX_LEVEL)
        return Staircase(deadline(start, level), min = deadline(start, top) * 0.9, max = deadline(start, start) * 1.3)
    }
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
    override fun startLevel(saved: Int): Int {
        val s = saved.coerceIn(1, EchoRules.MAX_LEVEL)
        return maxOf(EchoRules.modeStart(s), s - 2)
    }

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
