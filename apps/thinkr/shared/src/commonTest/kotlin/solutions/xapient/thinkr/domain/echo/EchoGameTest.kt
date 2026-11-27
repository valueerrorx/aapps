package solutions.xapient.thinkr.domain.echo

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import solutions.xapient.thinkr.domain.Round

class EchoGameTest {
    private val g = EchoGame

    // eine Runde mit 1-back ab Stufe 1, erste Antwort-Karte steht ab 1320 ms
    private fun firstAnswerCard(level: Int = 1, relaxed: Boolean = false): EchoState {
        val s = g.start(7, level, relaxed)
        return g.tick(s, s.dueAt)
    }

    private fun EchoState.truth(): Boolean = deck.matches[index]

    // antwortet richtig (bzw. falsch) nach `after` ms auf jede Karte, Merk-Karten laufen von selbst
    private fun playAll(s0: EchoState, right: Boolean, after: Long = 100): EchoState {
        var s = s0
        while (!s.round.isOver) {
            s = if (s.isMemo) g.tick(s, s.dueAt) else g.input(s, if (right) s.truth() else !s.truth(), s.shownAt + after)
            if (!s.round.isOver && s.shownAt + after >= s.round.durationMs) s = g.tick(s, s.round.durationMs)
        }
        return s
    }

    @Test fun startsOnLevelDeadline() {
        val s = g.start(1, 5, false)
        assertEquals(5, s.level)
        assertEquals(5, s.startLevel)
        assertEquals(1, s.nBack)
        assertEquals(EchoRules.deadline(1, 5).toLong(), s.deadlineMs)
        val t = g.start(1, 11, false)
        assertEquals(2, t.nBack)
        assertEquals(11, t.level)
    }

    @Test fun warmupStaysInsideMode() {
        assertEquals(1, g.startLevel(1))
        assertEquals(1, g.startLevel(3))
        assertEquals(3, g.startLevel(5))
        assertEquals(7, g.startLevel(9))
        assertEquals(10, g.startLevel(10))
        assertEquals(10, g.startLevel(12))
        assertEquals(11, g.startLevel(13))
        assertEquals(14, g.startLevel(15))
        assertEquals(18, g.startLevel(20))
        assertEquals(18, g.startLevel(99))
    }

    @Test fun memoCardAdvancesByItself() {
        val s = g.start(7, 1, false)
        assertTrue(s.isMemo)
        assertEquals(0, g.tick(s, EchoRules.MEMO_MS - 1).index)
        val t = g.tick(s, EchoRules.MEMO_MS)
        assertEquals(1, t.index)
        assertEquals(EchoRules.MEMO_MS + EchoRules.GAP_MS, t.shownAt)
        assertEquals(0, t.round.answered)
    }

    @Test fun inputOnMemoCardIsIgnored() {
        val s = g.start(7, 1, false)
        val t = g.input(s, true, 300)
        assertEquals(0, t.index)
        assertEquals(0, t.round.answered)
    }

    @Test fun fastCorrectAnswerScoresWithBonus() {
        val s = firstAnswerCard()
        val t = g.input(s, s.truth(), s.shownAt + 100)
        assertEquals(EchoRules.POINTS + EchoRules.FAST_BONUS, t.round.score)
        assertEquals(1, t.round.correct)
        assertEquals(s.index + 1, t.index)
        assertEquals(s.shownAt + 100 + EchoRules.GAP_MS, t.shownAt)
        assertEquals(true, t.last!!.correct)
        assertEquals(EchoRules.POINTS + EchoRules.FAST_BONUS, t.last!!.points)
    }

    @Test fun slowCorrectAnswerScoresWithoutBonus() {
        val s = firstAnswerCard()
        val t = g.input(s, s.truth(), s.shownAt + s.deadlineMs / 2 + 1)
        assertEquals(EchoRules.POINTS, t.round.score)
    }

    @Test fun wrongAnswerCountsAndSlowsDown() {
        val s = firstAnswerCard()
        val t = g.input(s, !s.truth(), s.shownAt + 100)
        assertEquals(1, t.round.wrong)
        assertEquals(0, t.round.score)
        assertEquals(false, t.last!!.correct)
        assertEquals(s.stair.value * 1.12, t.stair.value, 1e-6)
    }

    @Test fun inputDuringSwitchIsIgnored() {
        val s = firstAnswerCard()
        val t = g.input(s, s.truth(), s.shownAt + 100)
        val u = g.input(t, true, t.shownAt - 1)
        assertEquals(t.index, u.index)
        assertEquals(t.round.answered, u.round.answered)
    }

    @Test fun deadlineExpiryIsAMiss() {
        val s = firstAnswerCard()
        assertEquals(s.index, g.tick(s, s.dueAt - 1).index)
        val t = g.tick(s, s.dueAt)
        assertEquals(1, t.round.wrong)
        assertEquals(s.index + 1, t.index)
        assertNull(t.last!!.said)
        assertEquals(s.dueAt + EchoRules.GAP_MS, t.shownAt)
    }

    @Test fun longFrameCatchesUpSeveralExpiries() {
        val s = firstAnswerCard()
        // jede abgelaufene Frist verlaengert die naechste um 12 %
        val v = s.stair.value
        val span = v.toLong() + (v * 1.12).toLong() + (v * 1.12 * 1.12).toLong() + 2 * EchoRules.GAP_MS
        val t = g.tick(s, s.shownAt + span + 10)
        assertEquals(3, t.round.wrong)
        assertEquals(s.index + 3, t.index)
    }

    @Test fun relaxedGivesHalfAgainAsMuchTime() {
        val n = g.start(7, 4, false)
        val r = g.start(7, 4, true)
        assertEquals((n.stair.value * 1.5).toLong(), r.deadlineMs)
        assertEquals((EchoRules.MEMO_MS * 1.5).toLong(), r.memoMs)
        // Stufe haengt an der Treppe, nicht an der Gemaechlich-Zeit
        assertEquals(4, r.level)
    }

    @Test fun threeCorrectInARowShortenDeadline() {
        var s = firstAnswerCard()
        val before = s.stair.value
        repeat(3) { s = g.input(s, s.truth(), s.shownAt + 100) }
        assertEquals(before * 0.92, s.stair.value, 1e-6)
    }

    @Test fun roundEndsAfterSixtySecondsAndIgnoresLateInput() {
        val s = firstAnswerCard()
        val over = g.tick(s, 61_000)
        assertTrue(over.round.isOver)
        assertEquals(Round.DURATION_MS, over.round.now)
        val after = g.input(over, true, 61_500)
        assertEquals(over.round, after.round)
    }

    @Test fun idleRoundSameWithFramesOrOneJump() {
        // keine Antworten: Frame fuer Frame ergibt dasselbe wie ein Sprung, Fehler nur fuer Fristen vor dem Ende
        val s = g.start(3, 1, false)
        val jump = g.tick(s, 200_000)
        var frames = s
        var t = 0L
        while (t < 70_000) { t += 16; frames = g.tick(frames, t) }
        assertEquals(jump, frames)
        assertTrue(jump.round.isOver)
        assertTrue(jump.last!!.at < Round.DURATION_MS)
        assertTrue(jump.round.wrong in 15..30, "wrong ${jump.round.wrong}")
        assertEquals(0, jump.round.correct)
    }

    @Test fun perfectPlayClimbsOneModeAtMost() {
        val s = playAll(g.start(5, 1, false), right = true)
        assertEquals(EchoRules.TWO_BACK, s.level)
        assertEquals(100, s.round.accuracy)
        assertTrue(s.round.score > 0)
        assertEquals(1, s.startLevel)
    }

    @Test fun hopelessPlayDropsOneModeAtMost() {
        val s = playAll(g.start(5, 12, false), right = false)
        assertEquals(EchoRules.TWO_BACK - 1, s.level)
        assertEquals(0, s.round.score)
    }

    @Test fun levelBandsFollowDeadline() {
        assertEquals(5, EchoRules.levelOf(1, EchoRules.deadline(1, 5)))
        assertEquals(4, EchoRules.levelOf(1, EchoRules.deadline(1, 5) + 1))
        assertEquals(1, EchoRules.levelOf(1, 9_999.0))
        assertEquals(10, EchoRules.levelOf(1, 1.0))
        assertEquals(9, EchoRules.levelOf(10, 9_999.0))
        assertEquals(14, EchoRules.levelOf(10, 1.0))
        assertEquals(20, EchoRules.levelOf(14, 1.0))
    }

    @Test fun deadlinesShrinkWithinMode() {
        for (start in listOf(1, 10, 14)) {
            for (l in start until EchoRules.modeEnd(start)) {
                assertTrue(EchoRules.deadline(start, l + 1) < EchoRules.deadline(start, l))
            }
        }
        // neue Spielart beginnt wieder gemaechlicher
        assertTrue(EchoRules.deadline(10, 10) > EchoRules.deadline(1, 9))
        assertTrue(EchoRules.deadline(14, 14) > EchoRules.deadline(10, 13))
    }

    @Test fun sameSeedSamePlay() {
        assertEquals(playAll(g.start(9, 3, false), true, 333), playAll(g.start(9, 3, false), true, 333))
    }
}
