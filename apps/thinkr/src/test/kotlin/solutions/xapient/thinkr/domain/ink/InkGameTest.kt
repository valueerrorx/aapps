package solutions.xapient.thinkr.domain.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class InkGameTest {
    private val g = InkGame

    @Test fun deckBalancedNoLongRunsNoRepeats() {
        for (seed in 1L..30L) {
            val d = InkDeck.deal(Random(seed), 400, InkColor.entries, 0.5, seed % 2 == 0L)
            val yes = d.count { it.match }
            assertTrue("$seed: $yes", yes in 160..240)
            var run = 1
            d.zipWithNext().forEach { (a, b) ->
                assertNotEquals(a, b)
                run = if (a.match == b.match) run + 1 else 1
                assertTrue(run <= InkDeck.MAX_RUN)
            }
        }
    }

    @Test fun incongruentShareFollowsLevel() {
        for (level in listOf(1, 5, 9, 16)) {
            val p = InkRules.incongruent(level)
            val d = InkDeck.deal(Random(level.toLong()), 2000, InkColor.entries, p, false)
            val share = d.count { !it.congruent }.toDouble() / d.size
            assertTrue("$level: $share vs $p", abs(share - p) < 0.05)
        }
        assertTrue(InkRules.incongruent(1) < InkRules.incongruent(9))
        assertEquals(0.75, InkRules.incongruent(InkRules.MAX_LEVEL), 1e-9)
    }

    @Test fun answerIsMeaningAgainstInkNotWord() {
        val c = InkCard(InkColor.BLUE, null, InkColor.BLUE, InkColor.ORANGE)
        assertFalse(c.match)
        assertTrue(InkCard(InkColor.BLUE, InkColor.YELLOW, InkColor.RED, InkColor.BLUE).match)
    }

    @Test fun colorsAndLeftTintByMode() {
        assertEquals(InkColor.SAFE, InkRules.colors(1, false))
        assertEquals(6, InkRules.colors(6, false).size)
        assertEquals(InkColor.SAFE, InkRules.colors(12, true))
        val early = g.start(3, 4, false)
        assertTrue(early.cards.all { it.meaningInk == null && it.meaning in InkColor.SAFE && it.ink in InkColor.SAFE && it.word in InkColor.SAFE })
        val late = g.start(3, 12, false)
        assertTrue(late.cards.all { it.meaningInk != null })
        assertTrue(late.cards.any { it.ink == InkColor.RED || it.ink == InkColor.GREEN })
    }

    @Test fun safeModeNeverUsesRedOrGreen() {
        val s = g.start(3, 14, false, safeColors = true)
        assertTrue(s.safe)
        val used = s.cards.flatMap { listOfNotNull(it.meaning, it.meaningInk, it.word, it.ink) }.toSet()
        assertEquals(InkColor.SAFE.toSet(), used)
    }

    @Test fun rightAnswerScoresWrongResets() {
        var s = g.start(5, 1, false)
        repeat(4) { s = g.input(s, s.card.match, s.shownAt + 300) }
        assertEquals(4 * InkRules.POINTS, s.round.score)
        assertEquals(2, s.round.combo.mult)
        s = g.input(s, !s.card.match, s.shownAt + 300)
        assertEquals(1, s.round.combo.mult)
        assertEquals(1, s.round.wrong)
        assertEquals(5, s.index)
        assertFalse(s.last!!.correct)
    }

    @Test fun deadlineExpiresAsMiss() {
        val s = g.start(5, 3, false)
        assertEquals(0, g.tick(s, s.dueAt - 1).index)
        val t = g.tick(s, s.dueAt)
        assertEquals(1, t.index)
        assertEquals(1, t.round.wrong)
        assertNull(t.last!!.said)
        assertEquals(s.dueAt + InkRules.GAP_MS, t.shownAt)
    }

    @Test fun inputDuringGapIgnored() {
        var s = g.start(5, 3, false)
        s = g.input(s, true, 200)
        val t = g.input(s, true, 250)
        assertEquals(s.index, t.index)
        assertEquals(s.round.answered, t.round.answered)
    }

    @Test fun relaxedGivesMoreTime() {
        assertTrue(abs((g.start(1, 3, false).deadlineMs * 1.5).toLong() - g.start(1, 3, true).deadlineMs) <= 1)
    }

    @Test fun levelClimbsWithFastRightAnswers() {
        var s = g.start(8, 3, false)
        repeat(40) { s = g.input(s, s.card.match, s.shownAt + 200) }
        assertTrue(s.level > 3)
        // hoechstens eine Spielart hinauf
        assertTrue(s.level <= InkRules.SIX_COLORS)
        assertEquals(3, g.startLevel(5))
        assertEquals(InkRules.SIX_COLORS, g.startLevel(7))
    }

    @Test fun roundEndsAfterSixtySeconds() {
        var s = g.start(2, 1, false)
        s = g.tick(s, 61_000)
        assertTrue(s.round.isOver)
        assertTrue(s.round.wrong > 10)
        assertEquals(0, s.round.score)
    }

    // Die vier sicheren Farben bleiben bei Protanopie und Deuteranopie getrennt (Machado 2009, Abstand in CIELAB)
    @Test fun safeColorsStayApartWithColourBlindness() {
        val protan = arrayOf(doubleArrayOf(0.152286, 1.052583, -0.204868), doubleArrayOf(0.114503, 0.786281, 0.099216), doubleArrayOf(-0.003882, -0.048116, 1.051998))
        val deutan = arrayOf(doubleArrayOf(0.367322, 0.860646, -0.227968), doubleArrayOf(0.280085, 0.672501, 0.047413), doubleArrayOf(-0.011820, 0.042940, 0.968881))
        for (m in listOf(null, protan, deutan)) {
            val labs = InkColor.SAFE.map { lab(simulate(it.argb, m)) }
            for (i in labs.indices) for (j in i + 1 until labs.size) {
                val d = dist(labs[i], labs[j])
                assertTrue("${InkColor.SAFE[i]}-${InkColor.SAFE[j]}: $d", d >= 22.0)
            }
        }
        // ohne Sehschwaeche auch alle sechs klar getrennt
        val all = InkColor.entries.map { lab(simulate(it.argb, null)) }
        for (i in all.indices) for (j in i + 1 until all.size) assertTrue(dist(all[i], all[j]) >= 25.0)
    }

    private fun lin(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun simulate(argb: Long, m: Array<DoubleArray>?): DoubleArray {
        val rgb = doubleArrayOf(((argb shr 16) and 0xFF) / 255.0, ((argb shr 8) and 0xFF) / 255.0, (argb and 0xFF) / 255.0).map(::lin)
        if (m == null) return rgb.toDoubleArray()
        return DoubleArray(3) { k -> (0..2).sumOf { m[k][it] * rgb[it] }.coerceIn(0.0, 1.0) }
    }

    private fun lab(l: DoubleArray): DoubleArray {
        val x = (0.4124 * l[0] + 0.3576 * l[1] + 0.1805 * l[2]) / 0.95047
        val y = 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2]
        val z = (0.0193 * l[0] + 0.1192 * l[1] + 0.9505 * l[2]) / 1.08883
        fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116
        return doubleArrayOf(116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
    }

    private fun dist(a: DoubleArray, b: DoubleArray) = sqrt((0..2).sumOf { (a[it] - b[it]).pow(2) })
}
