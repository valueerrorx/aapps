package solutions.xapient.thinkr.domain.drops

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.random.Random

class DropsGameTest {
    private val g = DropsGame

    private fun type(s: DropsState, text: String, t: Long): DropsState {
        var x = s
        text.forEach { x = g.input(x, it.digitToInt(), t) }
        return x
    }

    // Zustand mit festen Tropfen, ohne dass vor until neue kommen
    private fun with(vararg drops: Drop, level: Int = 3, until: Long = 100_000): DropsState =
        g.start(1, level, false).copy(drops = drops.toList(), nextId = 100, nextSpawnAt = until)

    private fun drop(id: Int, a: Int, op: Op, b: Int, at: Long = 0, fall: Long = 5_000, glow: Boolean = false) =
        Drop(id, Sum(a, op, b), 0.5f, at, fall, glow)

    @Test fun sumsAreWholeAndNotNegative() {
        val rng = Random(7)
        for (level in 1..DropsRules.MAX_LEVEL) {
            val mix = DropsRules.mix(level)
            repeat(400) {
                val s = Sums.make(rng, level)
                assertTrue(s.op in mix.ops, "$level $s")
                assertTrue(s.answer >= 0, "$level $s")
                assertTrue(s.a > 0 && s.b > 0, "$level $s")
                if (s.op == Op.DIVIDE) assertEquals(0, s.a % s.b)
                if (s.op == Op.PLUS || s.op == Op.MINUS) assertTrue(s.a <= mix.addMax && s.answer <= mix.addMax)
                assertTrue(s.answer.toString().length <= DropsRules.MAX_DIGITS)
            }
        }
    }

    @Test fun operationsComeInOrder() {
        assertEquals(listOf(Op.PLUS), DropsRules.mix(1).ops)
        assertFalse(Op.MINUS in DropsRules.mix(2).ops)
        assertTrue(Op.MINUS in DropsRules.mix(3).ops)
        assertTrue(Op.TIMES in DropsRules.mix(5).ops)
        assertTrue(Op.DIVIDE in DropsRules.mix(7).ops)
        assertTrue(DropsRules.mix(10).wide > 0)
        for (l in 2..DropsRules.MAX_LEVEL) {
            assertTrue(DropsRules.fallMs(l) < DropsRules.fallMs(l - 1))
            assertTrue(DropsRules.maxDrops(l) >= DropsRules.maxDrops(l - 1))
        }
    }

    @Test fun pointsFollowDifficulty() {
        assertEquals(10, Sum(3, Op.PLUS, 4).points)
        assertEquals(15, Sum(30, Op.PLUS, 14).points)
        assertEquals(20, Sum(6, Op.TIMES, 7).points)
        assertEquals(20, Sum(42, Op.DIVIDE, 7).points)
        assertEquals(30, Sum(14, Op.TIMES, 6).points)
    }

    @Test fun dropsFallInOnTime() {
        var s = g.start(3, 1, false)
        s = g.tick(s, DropsRules.FIRST_MS - 1)
        assertTrue(s.drops.isEmpty())
        s = g.tick(s, DropsRules.FIRST_MS)
        assertEquals(1, s.drops.size)
        assertEquals(DropsRules.fallMs(1), s.drops[0].fallMs)
        // nie mehr als maxDrops gleichzeitig
        s = g.tick(s, 6_000)
        assertTrue(s.drops.size <= DropsRules.maxDrops(1))
        assertEquals(0, s.splashes)
    }

    @Test fun landingRaisesWaterAndThreeEndTheRound() {
        var s = g.start(3, 2, false)
        s = g.tick(s, 60_000)
        assertTrue(s.full)
        assertEquals(DropsRules.LIVES, s.splashes)
        assertTrue(s.round.isOver)
        assertEquals(DropsRules.LIVES, s.round.wrong)
        // endet genau beim dritten Aufschlag, nicht nach fester Zeit
        assertTrue(s.round.now < 60_000)
        assertEquals(s.splashAt, s.round.now)
        // danach passiert nichts mehr
        assertEquals(s.round, g.tick(s, 90_000).round)
    }

    @Test fun resultIndependentOfFrameRate() {
        val a = g.tick(g.start(8, 6, false), 40_000)
        var b = g.start(8, 6, false)
        var t = 0L
        while (t < 40_000) {
            t += 16
            b = g.tick(b, t)
        }
        assertEquals(a.copy(bursts = emptyList()), b.copy(bursts = emptyList()))
    }

    @Test fun typingSolvesWhenUnique() {
        val s = with(drop(1, 3, Op.PLUS, 4), drop(2, 9, Op.PLUS, 8))
        val x = type(s, "7", 1_000)
        assertTrue(x.drops.none { it.id == 1 })
        assertEquals(1, x.round.correct)
        assertEquals("", x.entry)
        assertEquals(10, x.round.score)
    }

    @Test fun waitsWhileALongerAnswerStartsTheSame() {
        // 1 und 17: nach „1" noch offen, Enter loest 1
        val s = with(drop(1, 5, Op.MINUS, 4), drop(2, 9, Op.PLUS, 8))
        val x = type(s, "1", 1_000)
        assertEquals("1", x.entry)
        assertEquals(2, x.drops.size)
        val y = g.input(x, DropsKeys.OK, 1_010)
        assertEquals(listOf(2), y.drops.map { it.id })
        // „17" ist eindeutig
        assertTrue(type(s, "17", 1_000).drops.none { it.id == 2 })
    }

    @Test fun sameAnswerPopsTogetherWithBonus() {
        val s = with(drop(1, 3, Op.PLUS, 3), drop(2, 2, Op.TIMES, 3, at = 200), drop(3, 1, Op.PLUS, 1))
        val x = type(s, "6", 1_000)
        assertEquals(listOf(3), x.drops.map { it.id })
        assertEquals(2, x.round.correct)
        assertEquals(10 + 20 + DropsRules.CHAIN_BONUS, x.round.score)
        assertEquals(2, x.bursts.count { it.good })
    }

    @Test fun glowDropClearsTheSky() {
        val s = with(drop(1, 3, Op.PLUS, 3, glow = true), drop(2, 9, Op.PLUS, 8), drop(3, 1, Op.PLUS, 1))
        val x = type(s, "6", 1_000)
        assertTrue(x.drops.isEmpty())
        assertEquals(3, x.round.correct)
    }

    @Test fun wrongEnterCountsButDoesNotFill() {
        val s = with(drop(1, 3, Op.PLUS, 4))
        val x = g.input(type(s, "8", 1_000), DropsKeys.OK, 1_100)
        assertEquals(1, x.round.wrong)
        assertEquals(0, x.splashes)
        assertEquals("", x.entry)
        assertEquals(1_100, x.wrongAt)
        assertEquals(1, x.drops.size)
    }

    @Test fun deleteAndLimits() {
        val s = with(drop(1, 900, Op.PLUS, 99, fall = 50_000), level = 12)
        var x = type(s, "12345", 1_000)
        assertEquals("1234", x.entry)
        x = g.input(x, DropsKeys.DEL, 1_001)
        assertEquals("123", x.entry)
        // Enter ohne Eingabe tut nichts
        val e = g.input(s, DropsKeys.OK, 1_000)
        assertEquals(0, e.round.wrong)
        // fuehrende Null passt nicht zu 7
        val z = type(with(drop(1, 3, Op.PLUS, 4)), "07", 1_000)
        assertEquals(1, z.drops.size)
    }

    @Test fun staircaseUpOnSolvedDownOnSplash() {
        var s = with(*Array(4) { drop(it, it + 1, Op.PLUS, 1, fall = 50_000) }, level = 3)
        s = type(s, "2", 1_000)
        s = type(s, "3", 1_000)
        s = type(s, "4", 1_000)
        assertEquals(3, s.stage)
        s = type(s, "5", 1_000)
        assertEquals(4, s.stage)
        assertEquals(0, s.run)
        // Aufschlag: eine Stufe zurueck
        s = s.copy(drops = listOf(drop(9, 1, Op.PLUS, 1, at = 1_000, fall = 1_000)))
        s = g.tick(s, 2_000)
        assertEquals(3, s.stage)
        assertEquals(1, s.splashes)
    }

    @Test fun maxLevelSpeedsUpInstead() {
        var s = with(*Array(4) { drop(it, it + 1, Op.PLUS, 1, fall = 50_000) }, level = DropsRules.MAX_LEVEL)
        listOf("2", "3", "4", "5").forEach { s = type(s, it, 1_000) }
        assertEquals(DropsRules.MAX_LEVEL, s.stage)
        assertEquals(DropsRules.BOOST, s.boost, 1e-9)
    }

    @Test fun relaxedFallsSlower() {
        val a = g.tick(g.start(2, 4, false), DropsRules.FIRST_MS)
        val b = g.tick(g.start(2, 4, true), DropsRules.FIRST_MS)
        assertEquals((DropsRules.fallMs(4) * 1.5).toLong(), b.drops[0].fallMs)
        assertTrue(b.drops[0].fallMs > a.drops[0].fallMs)
    }

    @Test fun noInputAfterTheEnd() {
        val s = g.tick(g.start(3, 2, false), 60_000)
        assertEquals(s, g.input(s, 4, 61_000))
    }

    @Test fun warmUp() {
        assertEquals(1, g.startLevel(1))
        assertEquals(5, g.startLevel(7))
        assertEquals(DropsRules.MAX_LEVEL - 2, g.startLevel(40))
    }
}
