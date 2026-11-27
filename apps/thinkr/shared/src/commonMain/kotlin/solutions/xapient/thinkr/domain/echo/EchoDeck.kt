package solutions.xapient.thinkr.domain.echo

import kotlin.random.Random

// Formen sind das einzige Merkmal; twin = aehnliche Form als Koeder ab Stufe 14
enum class Glyph {
    CIRCLE, TRIANGLE, DIAMOND, STAR, WAVE, RING;

    val twin: Glyph?
        get() = when (this) {
            CIRCLE -> RING
            RING -> CIRCLE
            TRIANGLE -> DIAMOND
            DIAMOND -> TRIANGLE
            else -> null
        }
}

// Kartenfolge: matches[i] = Karte i gleich wie Karte i-nBack (Merk-Karten am Anfang immer false)
data class Deck(val cards: List<Glyph>, val matches: List<Boolean>)

object EchoDeck {
    const val MAX_RUN = 4

    fun deal(rng: Random, count: Int, nBack: Int, similar: Boolean): Deck {
        val all = Glyph.entries
        val cards = ArrayList<Glyph>(count)
        val matches = ArrayList<Boolean>(count)
        // Merk-Karten untereinander verschieden
        repeat(nBack.coerceAtMost(count)) {
            cards += all.filter { it !in cards }.random(rng)
            matches += false
        }
        var yes = 0
        var asked = 0
        var runValue = false
        var run = 0
        for (i in nBack until count) {
            // Anteil „gleich" zieht zur Mitte, nie mehr als MAX_RUN gleiche Antworten in Folge
            val match = if (run >= MAX_RUN) {
                !runValue
            } else {
                rng.nextDouble() < (0.5 + 0.1 * (asked * 0.5 - yes)).coerceIn(0.2, 0.8)
            }
            val ref = cards[i - nBack]
            cards += if (match) ref else other(rng, ref, cards[i - 1], nBack, similar)
            matches += match
            asked++
            if (match) yes++
            if (asked > 1 && match == runValue) run++ else run = 1
            runValue = match
        }
        return Deck(cards, matches)
    }

    // Andere Form: ab Stufe 14 gern die aehnliche, bei 2-back gern die Karte direkt davor
    private fun other(rng: Random, ref: Glyph, prev: Glyph, nBack: Int, similar: Boolean): Glyph {
        val twin = ref.twin
        if (similar && twin != null && rng.nextDouble() < 0.5) return twin
        if (nBack == 2 && prev != ref && rng.nextDouble() < 0.35) return prev
        return Glyph.entries.filter { it != ref }.random(rng)
    }
}
