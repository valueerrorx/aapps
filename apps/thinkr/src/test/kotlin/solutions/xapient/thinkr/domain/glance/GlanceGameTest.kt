package solutions.xapient.thinkr.domain.glance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.thinkr.domain.Round

class GlanceGameTest {
    private val g = GlanceGame

    // Frames mit fester Dauer bis zur Antwortphase; zaehlt die Frames, in denen das Aufblitzen stand
    private fun run(s: GlanceState, frameMs: Double = 1000.0 / 60): Pair<GlanceState, Int> {
        var x = s
        var t = x.round.now.toDouble()
        var flashFrames = 0
        while (x.phase() != GlancePhase.ANSWER && !x.round.isOver) {
            t += frameMs
            x = g.tick(x, t.toLong())
            if (x.phase() == GlancePhase.FLASH) flashFrames++
        }
        return x to flashFrames
    }

    private fun answer(s: GlanceState, right: Boolean): GlanceState {
        val (x, _) = run(s)
        if (x.phase() != GlancePhase.ANSWER) return x
        val tr = x.trial
        val t = x.round.now + 400
        val a = g.input(x, GlanceInput.Center(tr.mark), t)
        val spot = if (right) tr.spot else (tr.spot + 1) % Spots.count(x.rings)
        val b = g.input(a, GlanceInput.Spot(spot), t + 300)
        return g.tick(b, b.nextAt)
    }

    @Test fun flashLastsTheStaircaseFrames() {
        for (rung in GlanceRules.FRAMES.indices) {
            val s = g.start(5, 1, false).let { it.copy(rung = rung).let { r -> r.copy(trial = g.newTrial(r, 0, 0)) } }
            val (x, frames) = run(s)
            assertEquals("$rung", GlanceRules.FRAMES[rung], frames)
            assertFalse(x.trial.void)
        }
    }

    @Test fun highRefreshShowsTheSameTime() {
        // 120 Hz: doppelt so viele Frames, gleiche Zeit
        val s = g.start(5, 1, false).let { it.copy(rung = 12, frameMs = 8.3).let { r -> r.copy(trial = g.newTrial(r, 0, 0)) } }
        var x = s
        var t = 0.0
        var n = 0
        while (x.phase() != GlancePhase.ANSWER) {
            t += 8.333
            x = g.tick(x, t.toLong())
            if (x.phase() == GlancePhase.FLASH) n++
        }
        assertEquals(2 * GlanceRules.FRAMES[12], n)
        assertFalse(x.trial.void)
    }

    @Test fun droppedFramesVoidTheTrial() {
        var x = g.start(5, 1, false).let { it.copy(rung = 13).let { r -> r.copy(trial = g.newTrial(r, 0, 0)) } }
        var t = 0L
        while (x.phase() == GlancePhase.FIX) {
            t += 16
            x = g.tick(x, t)
        }
        // ein Haenger von 90 ms statt 4 Frames
        x = g.tick(x, t + 90)
        assertTrue(x.trial.void)
        assertEquals(1, x.voids)
        val (y, _) = run(x)
        val a = g.input(g.input(y, GlanceInput.Center(y.trial.mark), y.round.now + 10), GlanceInput.Spot(y.trial.spot), y.round.now + 20)
        // nicht gewertet: keine Punkte, kein Fehler, Treppe bleibt
        assertEquals(0, a.round.answered)
        assertEquals(x.rung, a.rung)
        assertTrue(a.trial.done)
    }

    @Test fun phasesInOrder() {
        var x = g.start(2, 3, false)
        assertEquals(GlancePhase.FIX, x.phase())
        val seen = mutableListOf(x.phase())
        var t = 0L
        while (x.phase() != GlancePhase.ANSWER) {
            t += 16
            x = g.tick(x, t)
            if (seen.last() != x.phase()) seen += x.phase()
        }
        assertEquals(listOf(GlancePhase.FIX, GlancePhase.FLASH, GlancePhase.MASK, GlancePhase.ANSWER), seen)
        assertTrue(x.trial.flashAt >= x.trial.fixMs)
    }

    @Test fun bothPartsNeededInAnyOrder() {
        val (x, _) = run(g.start(3, 2, false))
        val tr = x.trial
        val a = g.input(x, GlanceInput.Spot(tr.spot), x.round.now + 100)
        assertFalse(a.trial.done)
        assertEquals(0, a.round.answered)
        // zweiter Tipp auf eine Stelle aendert nichts
        assertEquals(tr.spot, g.input(a, GlanceInput.Spot((tr.spot + 3) % 8), x.round.now + 120).trial.saidSpot)
        val b = g.input(a, GlanceInput.Center(tr.mark), x.round.now + 200)
        assertTrue(b.trial.done)
        assertEquals(1, b.round.correct)
        assertEquals(GlanceRules.POINTS + GlanceRules.RING_BONUS * Spots.ring(tr.spot), b.round.score)
    }

    @Test fun wrongCenterIsAMiss() {
        val (x, _) = run(g.start(3, 2, false))
        val other = if (x.trial.mark == Mark.ORB) Mark.DIAMOND else Mark.ORB
        val a = g.input(g.input(x, GlanceInput.Center(other), x.round.now + 10), GlanceInput.Spot(x.trial.spot), x.round.now + 20)
        assertEquals(1, a.round.wrong)
        assertEquals(0, a.round.score)
    }

    @Test fun noAnswerBeforeTheMaskIsGone() {
        var x = g.start(3, 2, false)
        assertNull(g.input(x, GlanceInput.Spot(0), 10).trial.saidSpot)
        var t = 0L
        while (x.phase() != GlancePhase.MASK) {
            t += 16
            x = g.tick(x, t)
        }
        assertNull(g.input(x, GlanceInput.Center(Mark.ORB), t).trial.saidMark)
    }

    @Test fun staircaseTwoRightOneStepFaster() {
        var s = g.start(7, 1, false)
        assertEquals(0, s.rung)
        s = answer(s, true)
        assertEquals(0, s.rung)
        s = answer(s, true)
        assertEquals(1, s.rung)
        s = answer(s, false)
        assertEquals(0, s.rung)
        s = answer(s, false)
        assertEquals(0, s.rung)
    }

    @Test fun levelFollowsDisplayTime() {
        val s = g.start(7, 1, false)
        assertEquals(1, s.level)
        assertEquals(2, s.copy(rung = 3).level)
        // ganz unten: hoechstens eine Spielart hinauf
        assertEquals(7, s.copy(rung = GlanceRules.FRAMES.lastIndex).level)
        val two = g.start(7, 9, false)
        assertEquals(7, two.modeStart)
        assertEquals(9, two.level)
        assertEquals(2, two.rings)
        assertEquals(7, two.copy(rung = 0).level)
    }

    @Test fun fieldGrowsWithMode() {
        assertEquals(1, g.start(1, 3, false).rings)
        assertEquals(2, g.start(1, 8, false).rings)
        val three = g.start(1, 15, false)
        assertEquals(3, three.rings)
        assertEquals(GlanceRules.decoys(15), three.trial.decoys.size)
        assertFalse(three.trial.spot in three.trial.decoys)
        assertEquals(0, GlanceRules.decoys(12))
    }

    @Test fun targetsSpreadOverRingsAndSpots() {
        val s = g.start(11, 15, false)
        val trials = (0 until 600).map { g.newTrial(s, it, 0) }
        val rings = trials.groupingBy { Spots.ring(it.spot) }.eachCount()
        assertEquals(3, rings.size)
        rings.values.forEach { assertTrue(it in 150..250) }
        assertEquals(24, trials.map { it.spot }.toSet().size)
        val orbs = trials.count { it.mark == Mark.ORB }
        assertTrue(orbs in 240..360)
        assertTrue(trials.all { it.fixMs in GlanceRules.FIX_MIN_MS..GlanceRules.FIX_MAX_MS })
    }

    @Test fun relaxedShowsLongerButAtLeast150ms() {
        val r = g.start(3, 1, true)
        assertEquals(45, g.framesFor(r))
        assertEquals(GlanceRules.RELAXED_MIN_FRAMES, g.framesFor(r.copy(rung = GlanceRules.FRAMES.lastIndex)))
        assertEquals(3, g.framesFor(g.start(3, 1, false).copy(rung = GlanceRules.FRAMES.lastIndex)))
    }

    @Test fun warmUpStaysInMode() {
        assertEquals(1, g.startLevel(2))
        assertEquals(7, g.startLevel(8))
        assertEquals(8, g.startLevel(10))
    }

    @Test fun roundEndsAtSixtySeconds() {
        var s = g.start(4, 2, false)
        while (!s.round.isOver && !s.trial.done) s = answer(s, true)
        val end = g.tick(s, Round.DURATION_MS + 100)
        assertTrue(end.round.isOver)
        assertTrue(end.round.correct >= 12)
        val after = g.input(end, GlanceInput.Center(Mark.ORB), Round.DURATION_MS + 200)
        assertEquals(end.trial, after.trial)
        assertEquals(end.round, after.round)
    }
}
