package solutions.xapient.thinkr.domain.ink

import solutions.xapient.thinkr.domain.Bands
import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import solutions.xapient.thinkr.domain.Staircase
import kotlin.random.Random

// Farben als ARGB; die ersten vier bleiben auch bei Rot-Gruen-Schwaeche unterscheidbar (Test prueft das)
enum class InkColor(val argb: Long) {
    BLUE(0xFF2E7BEA),
    ORANGE(0xFFDD6F12),
    YELLOW(0xFFF7E75C),
    PURPLE(0xFFCFA0FF),
    RED(0xFFF0503C),
    GREEN(0xFF34C46E);

    companion object {
        val SAFE: List<InkColor> = listOf(BLUE, ORANGE, YELLOW, PURPLE)
    }
}

// Links die Bedeutung (meaningInk = Schriftfarbe ab Stufe 10, sonst neutral), rechts ein Wort in Schriftfarbe ink
data class InkCard(val meaning: InkColor, val meaningInk: InkColor?, val word: InkColor, val ink: InkColor) {
    // gefragt: passt die Bedeutung links zur Schriftfarbe rechts?
    val match: Boolean get() = meaning == ink
    val congruent: Boolean get() = word == ink
}

// Stufen und Fristen an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object InkRules {
    const val MAX_LEVEL = 16
    const val SIX_COLORS = 6
    const val TINTED_LEFT = 10
    const val POINTS = 50
    const val GAP_MS = 140L
    const val RELAXED = 1.5
    const val DECK = 400

    // Spielarten: 4 Farben (1–5), 6 Farben (6–9), dazu farbiges linkes Wort (10–16)
    val bands = Bands(listOf(1, SIX_COLORS, TINTED_LEFT), listOf(2600.0, 2900.0, 3100.0), MAX_LEVEL, 0.9)

    // Farbenblind-Modus: immer nur die vier sicheren Farben
    fun colors(start: Int, safe: Boolean): List<InkColor> = if (start >= SIX_COLORS && !safe) InkColor.entries else InkColor.SAFE

    // Anteil der Karten, deren rechtes Wort nicht zur eigenen Schriftfarbe passt
    fun incongruent(level: Int): Double = (0.3 + 0.05 * (level - 1)).coerceAtMost(0.75)

    fun tintLeft(start: Int): Boolean = start >= TINTED_LEFT
}

object InkDeck {
    const val MAX_RUN = 4

    fun deal(rng: Random, count: Int, colors: List<InkColor>, incongruent: Double, tintLeft: Boolean): List<InkCard> {
        val out = ArrayList<InkCard>(count)
        var yes = 0
        var runValue = false
        var run = 0
        for (i in 0 until count) {
            // Anteil „passt" zieht zur Mitte, nie mehr als MAX_RUN gleiche Antworten in Folge
            val match = if (run >= MAX_RUN) !runValue else rng.nextDouble() < (0.5 + 0.1 * (i * 0.5 - yes)).coerceIn(0.2, 0.8)
            var card: InkCard
            do {
                card = card(rng, colors, match, incongruent, tintLeft)
            } while (card == out.lastOrNull())
            out += card
            if (match) yes++
            if (i > 0 && match == runValue) run++ else run = 1
            runValue = match
        }
        return out
    }

    private fun card(rng: Random, colors: List<InkColor>, match: Boolean, incongruent: Double, tintLeft: Boolean): InkCard {
        val meaning = colors.random(rng)
        val ink = if (match) meaning else colors.filter { it != meaning }.random(rng)
        val word = when {
            rng.nextDouble() >= incongruent -> ink
            // Koeder: das Wort rechts sagt die Bedeutung von links, die Schrift aber nicht
            !match && rng.nextDouble() < 0.5 -> meaning
            else -> colors.filter { it != ink }.random(rng)
        }
        val meaningInk = when {
            !tintLeft -> null
            rng.nextDouble() < 0.7 -> colors.filter { it != meaning }.random(rng)
            else -> meaning
        }
        return InkCard(meaning, meaningInk, word, ink)
    }
}

// Letzte Auswertung fuer die Optik; said = null: Frist abgelaufen
data class InkAnswer(val index: Int, val said: Boolean?, val correct: Boolean, val at: Long)

data class InkState(
    override val round: Round,
    override val startLevel: Int,
    val modeStart: Int,
    val cards: List<InkCard>,
    val index: Int,
    val shownAt: Long,
    val stair: Staircase,
    val relaxed: Boolean,
    val safe: Boolean,
    val last: InkAnswer? = null,
) : Play {
    override val level: Int get() = InkRules.bands.levelOf(modeStart, stair.value)
    val card: InkCard get() = cards[index]
    val deadlineMs: Long get() = (stair.value * if (relaxed) InkRules.RELAXED else 1.0).toLong()
    val dueAt: Long get() = shownAt + deadlineMs
}

// Tinte: passt die Bedeutung links zur Schriftfarbe rechts? Eingabe true = ja
object InkGame : MiniGame<InkState, Boolean> {
    override fun startLevel(saved: Int): Int = InkRules.bands.warmUp(saved)

    override fun start(seed: Long, level: Int, relaxed: Boolean): InkState = start(seed, level, relaxed, false)

    override fun start(seed: Long, level: Int, relaxed: Boolean, safeColors: Boolean): InkState {
        val l = level.coerceIn(1, InkRules.MAX_LEVEL)
        val m = InkRules.bands.modeStart(l)
        val cards = InkDeck.deal(Random(seed), InkRules.DECK, InkRules.colors(m, safeColors), InkRules.incongruent(l), InkRules.tintLeft(m))
        return InkState(Round(), l, m, cards, 0, 0, InkRules.bands.stairFor(m, l), relaxed, safeColors)
    }

    override fun tick(s: InkState, t: Long): InkState {
        var x = s.copy(round = s.round.at(t))
        // auch mehrere Abläufe nach einem langen Frame nachholen, aber nichts nach dem Rundenende
        while (x.dueAt <= t && x.dueAt < x.round.durationMs && x.index < x.cards.lastIndex) {
            val due = x.dueAt
            x = x.copy(
                round = x.round.miss(),
                stair = x.stair.miss(),
                last = InkAnswer(x.index, null, false, due),
                index = x.index + 1,
                shownAt = due + InkRules.GAP_MS,
            )
        }
        if (x.index >= x.cards.lastIndex) x = x.copy(round = x.round.at(x.round.durationMs))
        return x
    }

    override fun input(s: InkState, input: Boolean, t: Long): InkState {
        val x = tick(s, t)
        // Wechsel und Rundenende nehmen keine Antwort an
        if (x.round.isOver || t < x.shownAt) return x
        val correct = input == x.card.match
        return x.copy(
            round = if (correct) x.round.hit(InkRules.POINTS) else x.round.miss(),
            stair = if (correct) x.stair.hit() else x.stair.miss(),
            last = InkAnswer(x.index, input, correct, t),
            index = x.index + 1,
            shownAt = t + InkRules.GAP_MS,
        )
    }
}
