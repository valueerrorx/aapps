package solutions.xapient.thinkr.domain.bounce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.thinkr.domain.Round
import kotlin.random.Random

class BounceGameTest {
    private val g = BounceGame

    private fun toAnswer(s: BounceState): BounceState = g.tick(s, s.hideAt)

    // antworten und bis zur naechsten Aufgabe laufen lassen
    private fun answer(s: BounceState, right: Boolean): BounceState {
        val x = toAnswer(s)
        val exit = x.shot.trace.exit
        val wrong = Optics.ports(x.shot.side).first { it != exit && it != x.shot.entry }
        val a = g.input(x, if (right) exit else wrong, x.round.now + 300)
        return g.tick(a, a.nextAt)
    }

    @Test fun straightBeamLeavesOpposite() {
        val t = Optics.trace(5, emptyMap(), Port(Edge.LEFT, 2))
        assertEquals(Port(Edge.RIGHT, 2), t.exit)
        assertEquals(6, t.length)
        assertTrue(t.hits.isEmpty())
    }

    @Test fun prismsTurnNinetyDegrees() {
        // von links nach rechts: „/" lenkt nach oben, „\" nach unten
        val up = Optics.trace(5, mapOf(2 * 5 + 2 to Prism.SLASH), Port(Edge.LEFT, 2))
        assertEquals(Port(Edge.TOP, 2), up.exit)
        val down = Optics.trace(5, mapOf(2 * 5 + 2 to Prism.BACKSLASH), Port(Edge.LEFT, 2))
        assertEquals(Port(Edge.BOTTOM, 2), down.exit)
        // von oben nach unten: „/" lenkt nach links, „\" nach rechts
        assertEquals(Port(Edge.LEFT, 1), Optics.trace(5, mapOf(1 * 5 + 3 to Prism.SLASH), Port(Edge.TOP, 3)).exit)
        assertEquals(Port(Edge.RIGHT, 1), Optics.trace(5, mapOf(1 * 5 + 3 to Prism.BACKSLASH), Port(Edge.TOP, 3)).exit)
    }

    @Test fun twoPrismsMakeAZigzag() {
        // links rein bei Zeile 3, „/" bei (1,3) nach oben, „/" bei (1,0) nach rechts
        val prisms = mapOf(3 * 5 + 1 to Prism.SLASH, 0 * 5 + 1 to Prism.SLASH)
        val t = Optics.trace(5, prisms, Port(Edge.LEFT, 3))
        assertEquals(Port(Edge.RIGHT, 0), t.exit)
        assertEquals(listOf(Cell(-1, 3), Cell(1, 3), Cell(1, 0), Cell(5, 0)), t.corners)
        assertEquals(2, t.distinct)
        assertEquals(2 + 3 + 4, t.length)
    }

    @Test fun beamNeverLeavesWhereItCameIn() {
        val rng = Random(3)
        repeat(400) {
            val side = 5 + rng.nextInt(4)
            val prisms = (0 until side * side).shuffled(rng).take(rng.nextInt(1, 12))
                .associateWith { if (rng.nextBoolean()) Prism.SLASH else Prism.BACKSLASH }
            for (p in Optics.ports(side)) {
                val t = Optics.trace(side, prisms, p)
                assertNotEquals(p, t.exit)
                // umkehrbar: vom Ausgang zurueck landet man am Eingang
                assertEquals(p, Optics.trace(side, prisms, t.exit).exit)
            }
        }
    }

    @Test fun generatorMeetsMinimumHits() {
        for (level in 1..BounceRules.MAX_LEVEL) {
            repeat(30) { n ->
                val sh = g.newShot(level * 100L + n, n, level, 0)
                assertEquals(BounceRules.side(level), sh.side)
                assertEquals(BounceRules.prisms(level), sh.prisms.size)
                assertTrue(sh.prisms.keys.all { it in 0 until sh.side * sh.side })
                assertTrue("$level", sh.trace.distinct >= BounceRules.minHits(level))
                assertEquals(sh.trace, Optics.trace(sh.side, sh.prisms, sh.entry))
            }
        }
    }

    @Test fun levelTableGrows() {
        assertEquals(5, BounceRules.side(1))
        assertEquals(8, BounceRules.side(BounceRules.MAX_LEVEL))
        assertEquals(2, BounceRules.prisms(1))
        assertEquals(10, BounceRules.prisms(BounceRules.MAX_LEVEL))
        for (l in 2..BounceRules.MAX_LEVEL) {
            assertTrue(BounceRules.prisms(l) >= BounceRules.prisms(l - 1))
            assertTrue(BounceRules.showMs(l) <= BounceRules.showMs(l - 1))
        }
    }

    @Test fun deterministicPerSeed() {
        assertEquals(g.start(9, 5, false), g.start(9, 5, false))
        assertNotEquals(g.start(9, 5, false).shot, g.start(10, 5, false).shot)
    }

    @Test fun phasesFollowTime() {
        val s = g.start(4, 3, false)
        assertEquals(BouncePhase.LEAD, s.phase(0))
        assertEquals(BouncePhase.SHOW, s.phase(BounceRules.LEAD_MS))
        assertEquals(BouncePhase.ANSWER, s.phase(s.hideAt))
        // ohne Antwort bleibt es beim Abfragen
        assertEquals(BouncePhase.ANSWER, g.tick(s, 40_000).phase())
    }

    @Test fun relaxedShowsLonger() {
        val a = g.start(4, 3, false)
        val b = g.start(4, 3, true)
        assertEquals(BounceRules.LEAD_MS + (BounceRules.showMs(3) * 1.5).toLong(), b.hideAt)
        assertTrue(b.hideAt > a.hideAt)
    }

    @Test fun tapsOnlyCountWhileAsking() {
        val s = g.start(4, 3, false)
        val exit = s.shot.trace.exit
        // waehrend des Zeigens zaehlt nichts
        assertFalse(g.input(s, exit, BounceRules.LEAD_MS + 10).shot.answered)
        val x = toAnswer(s)
        // Eingang und Felder ausserhalb zaehlen nicht
        assertFalse(g.input(x, x.shot.entry, x.round.now).shot.answered)
        assertFalse(g.input(x, Port(Edge.TOP, x.shot.side), x.round.now).shot.answered)
        val a = g.input(x, exit, x.round.now + 1)
        assertTrue(a.shot.answered)
        // zweiter Tipp aendert nichts
        val other = Optics.ports(a.shot.side).first { it != exit && it != a.shot.entry }
        assertEquals(exit, g.input(a, other, a.round.now + 5).shot.said)
    }

    @Test fun scoredOnlyWhenLightIsOut() {
        val x = toAnswer(g.start(4, 3, false))
        val a = g.input(x, x.shot.trace.exit, x.round.now)
        assertEquals(0, a.round.score)
        assertEquals(BouncePhase.RUN, a.phase())
        val mid = g.tick(a, a.doneAt - 1)
        assertEquals(0, mid.round.correct)
        val done = g.tick(a, a.doneAt)
        assertEquals(1, done.round.correct)
        assertEquals(BounceRules.POINTS * a.shot.trace.distinct, done.round.score)
        assertEquals(BouncePhase.HOLD, done.phase())
    }

    @Test fun bouncesCountAsSteps() {
        val x = toAnswer(g.start(21, 8, false))
        val a = g.input(x, x.shot.trace.exit, x.round.now)
        val hits = a.shot.trace.hits.size
        assertTrue(hits >= BounceRules.minHits(8))
        assertEquals(0, a.steps)
        assertEquals(hits, g.tick(a, a.doneAt).steps)
        // naechste Aufgabe: Zaehler laeuft weiter, nie rueckwaerts
        val next = g.tick(a, g.tick(a, a.doneAt).nextAt)
        assertEquals(1, next.shot.no)
        assertEquals(hits, next.steps)
    }

    @Test fun staircaseUpSameDown() {
        var s = g.start(7, 5, false)
        s = answer(s, true)
        assertEquals(6, s.stage)
        assertEquals(6, s.shot.level)
        s = answer(s, false)
        assertEquals(6, s.stage)
        s = answer(s, false)
        assertEquals(5, s.stage)
        // nach dem Abstieg beginnt die Fehlerzaehlung neu
        s = answer(s, false)
        assertEquals(5, s.stage)
        s = answer(s, true)
        s = answer(s, false)
        assertEquals(6, s.stage)
    }

    @Test fun levelBounds() {
        assertEquals(BounceRules.MAX_LEVEL, BounceRules.next(BounceRules.MAX_LEVEL, true, 0))
        assertEquals(1, BounceRules.next(1, false, 2))
        assertEquals(1, g.startLevel(2))
        assertEquals(10, g.startLevel(12))
        assertEquals(BounceRules.MAX_LEVEL - 2, g.startLevel(99))
    }

    @Test fun roundEndsAfterSixtySeconds() {
        var s = g.start(5, 2, false)
        while (!s.round.isOver && s.hideAt < Round.DURATION_MS - 400) s = answer(s, true)
        val end = g.tick(s, Round.DURATION_MS + 5_000)
        assertTrue(end.round.isOver)
        assertEquals(Round.DURATION_MS, end.round.now)
        assertEquals(0, end.round.wrong)
        assertTrue(end.round.correct > 5)
        // nach dem Ende keine Eingabe mehr
        assertEquals(end, g.input(end, end.shot.trace.exit, Round.DURATION_MS + 6_000))
    }

    @Test fun answerStillRunningAtTheEndIsScored() {
        val x = toAnswer(g.start(5, 6, false))
        val a = g.input(x, x.shot.trace.exit, x.round.now)
        // Runde endet, bevor das Licht draussen ist
        val short = a.copy(round = a.round.copy(durationMs = a.round.now + 10))
        val end = g.tick(short, a.round.now + 20)
        assertTrue(end.round.isOver)
        assertTrue(end.shot.scored)
        assertEquals(1, end.round.correct)
    }
}
