package solutions.xapient.thinkr.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class RoundTest {
    private fun Combo.hits(n: Int): Combo = (1..n).fold(this) { c, _ -> c.hit() }

    @Test fun comboRisesEveryFourHits() {
        assertEquals(Combo(1, 3), Combo().hits(3))
        assertEquals(Combo(2, 0), Combo().hits(4))
        assertEquals(Combo(3, 0), Combo().hits(8))
        assertEquals(Combo(5, 0), Combo().hits(16))
    }

    @Test fun comboStopsAtMax() {
        assertEquals(Combo(Combo.MAX, 0), Combo().hits(100))
    }

    @Test fun missResetsCombo() {
        assertEquals(Combo(), Combo().hits(9).miss())
    }

    @Test fun staircaseStepsAfterThreeHits() {
        val s = Staircase(1000.0, 100.0, 5000.0)
        assertEquals(1000.0, s.hit().hit().value, 1e-9)
        assertEquals(920.0, s.hit().hit().hit().value, 1e-9)
        assertEquals(0, s.hit().hit().hit().run)
    }

    @Test fun staircaseMissSlowsDownAndResetsRun() {
        val s = Staircase(1000.0, 100.0, 5000.0).hit().hit().miss()
        assertEquals(1120.0, s.value, 1e-9)
        assertEquals(0, s.run)
        // nach dem Fehler braucht es wieder drei Treffer
        assertEquals(1120.0, s.hit().hit().value, 1e-9)
    }

    @Test fun staircaseIsClamped() {
        var s = Staircase(1000.0, 900.0, 1100.0)
        repeat(30) { s = s.hit() }
        assertEquals(900.0, s.value, 1e-9)
        repeat(30) { s = s.miss() }
        assertEquals(1100.0, s.value, 1e-9)
    }

    @Test fun hitUsesMultiplierBeforeTheHit() {
        var r = Round()
        repeat(4) { r = r.hit(50) }
        assertEquals(200, r.score)
        assertEquals(2, r.combo.mult)
        r = r.hit(50)
        assertEquals(300, r.score)
    }

    @Test fun accuracyRoundsDown() {
        var r = Round()
        repeat(99) { r = r.hit(1) }
        r = r.miss()
        assertEquals(99, r.accuracy)
        assertEquals(0, Round().accuracy)
    }

    @Test fun timeOnlyMovesForwardAndStopsAtEnd() {
        val r = Round().at(5000)
        assertEquals(5000, r.at(4000).now)
        assertFalse(r.isOver)
        val end = r.at(70_000)
        assertEquals(Round.DURATION_MS, end.now)
        assertTrue(end.isOver)
        assertEquals(0, end.remainingMs)
    }

    @Test fun endStopsTheRoundNow() {
        val r = Round().at(12_000).end()
        assertTrue(r.isOver)
        assertEquals(12_000, r.now)
        // spaetere Zeit aendert nichts mehr
        assertEquals(12_000, r.at(30_000).now)
    }
}
