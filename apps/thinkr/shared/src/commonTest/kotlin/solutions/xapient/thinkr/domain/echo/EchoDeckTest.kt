package solutions.xapient.thinkr.domain.echo

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.random.Random

class EchoDeckTest {
    private fun deal(seed: Long, n: Int = 1, similar: Boolean = false, count: Int = 120) =
        EchoDeck.deal(Random(seed), count, n, similar)

    @Test fun matchesAgreeWithCards() {
        for (n in 1..2) for (seed in 0L until 100) {
            val d = deal(seed, n)
            for (i in n until d.cards.size) {
                assertEquals(d.cards[i] == d.cards[i - n], d.matches[i], "seed $seed i $i")
            }
        }
    }

    @Test fun shareOfMatchesStaysNearHalf() {
        for (n in 1..2) for (seed in 0L until 300) {
            val answers = deal(seed, n).matches.drop(n)
            // typische Runde: 30 bis 60 Antworten
            for (len in listOf(30, 60)) {
                val share = answers.take(len).count { it }.toDouble() / len
                assertTrue(share in 0.4..0.6, "seed $seed n $n len $len share $share")
            }
        }
    }

    @Test fun noMoreThanFourEqualAnswersInARow() {
        for (seed in 0L until 300) {
            var run = 0
            var prev: Boolean? = null
            for (m in deal(seed, 1, count = 400).matches.drop(1)) {
                run = if (m == prev) run + 1 else 1
                prev = m
                assertTrue(run <= EchoDeck.MAX_RUN, "seed $seed")
            }
        }
    }

    @Test fun memoCardsDiffer() {
        for (seed in 0L until 50) {
            val d = deal(seed, 2)
            assertNotEquals(d.cards[0], d.cards[1])
            assertEquals(listOf(false, false), d.matches.take(2))
        }
    }

    @Test fun sameSeedSameDeck() {
        assertEquals(deal(42, 2, true), deal(42, 2, true))
    }

    @Test fun similarModeUsesTwinsAsLures() {
        var twins = 0
        var plain = 0
        for (seed in 0L until 40) {
            val d = deal(seed, 2, similar = true)
            val e = deal(seed, 2, similar = false)
            for (i in 2 until d.cards.size) if (!d.matches[i] && d.cards[i] == d.cards[i - 2].twin) twins++
            for (i in 2 until e.cards.size) if (!e.matches[i] && e.cards[i] == e.cards[i - 2].twin) plain++
        }
        assertTrue(twins > plain * 2, "twins $twins plain $plain")
    }

    @Test fun twoBackUsesPreviousCardAsLure() {
        var lures = 0
        var total = 0
        for (seed in 0L until 40) {
            val d = deal(seed, 2)
            for (i in 2 until d.cards.size) if (!d.matches[i]) {
                total++
                if (d.cards[i] == d.cards[i - 1]) lures++
            }
        }
        // Koeder in etwa einem Drittel der Nein-Karten
        assertTrue(lures > total / 4, "lures $lures of $total")
    }
}
