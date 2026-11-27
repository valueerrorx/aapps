package solutions.xapient.thinkr.domain.switches

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import solutions.xapient.thinkr.domain.Round

class SwitchesGameTest {
    private fun idle(s: SwitchesState, until: Long, step: Long): SwitchesState {
        var x = s
        var t = x.round.now
        while (t < until) {
            t = minOf(t + step, until)
            x = SwitchesGame.tick(x, t)
        }
        return x
    }

    @Test fun startByLevel() {
        for (l in 1..SwitchesRules.MAX_LEVEL) {
            val s = SwitchesGame.start(l * 13L, l, false)
            assertEquals(SwitchesRules.gates(l), s.net.gateCount)
            assertEquals((0 until s.net.gateCount).toSet(), s.colors.toSet())
            assertEquals(s.net.switches.size, s.on.size)
            assertEquals(SwitchesRules.DURATION_MS, s.round.durationMs)
        }
        assertEquals(3, SwitchesRules.gates(1))
        assertEquals(10, SwitchesRules.gates(8))
        assertEquals(10, SwitchesRules.gates(12))
    }

    @Test fun deterministicPerSeed() {
        val a = idle(SwitchesGame.start(7, 5, false), 20_000, 16)
        val b = idle(SwitchesGame.start(7, 5, false), 20_000, 16)
        assertEquals(a.net.nodes, b.net.nodes)
        assertEquals(a.copy(net = b.net), b)
        val c = SwitchesGame.start(8, 5, false)
        assertTrue(c.colors != a.colors || c.on != a.on || c.net.nodes != a.net.nodes)
    }

    // Ergebnis haengt nicht an der Bildrate
    @Test fun frameRateIndependent() {
        val s = SwitchesGame.start(3, 6, false)
        val fine = idle(s, 30_000, 8)
        val coarse = idle(s, 30_000, 97)
        assertEquals(fine.round, coarse.round)
        assertEquals(fine.beads, coarse.beads)
        assertEquals(fine.nextId, coarse.nextId)
    }

    @Test fun firstBeadLeavesPortalAndRollsEvenly() {
        var s = SwitchesGame.start(1, 1, false)
        s = SwitchesGame.tick(s, 500)
        assertTrue(s.beads.isEmpty())
        s = SwitchesGame.tick(s, 1_600)
        val b = s.beads.single()
        assertEquals(s.net.portalEdge, b.edge)
        assertEquals(SwitchesRules.FIRST_MS, b.enteredAt, 1e-9)
        assertEquals(1.0 * SwitchesRules.speed(1), s.progress(b), 1e-6)
    }

    // Fairness: von der Abfahrt bis zur ersten Weiche bleibt Zeit zum Schauen und Stellen, in jedem Netz jeder Stufe
    @Test fun timeToFirstSwitch() {
        for (level in 1..SwitchesRules.MAX_LEVEL) {
            val speed = SwitchesGame.start(0, level, false).speed
            val min = if (level == 1) 2_500.0 else 1_800.0
            NetBook.plans.getValue(SwitchesRules.gates(level)).forEachIndexed { i, rows ->
                for (v in 0 until NetBook.VARIANTS) {
                    val net = Net.parse(rows, v)
                    val ms = net.edges[net.portalEdge].track.length / speed
                    assertTrue(ms >= min, "Stufe $level Netz $i/v$v: $ms ms")
                }
            }
        }
    }

    // Nur eine Perle: alle Weichen auf das Tor gate stellen und ankommen lassen
    private fun lone(seed: Long, gate: (Bead) -> Int): Pair<SwitchesState, Bead> {
        var s = SwitchesGame.tick(SwitchesGame.start(seed, 2, false), 700).let { it.copy(nextAt = 1e12) }
        val first = s.beads.single()
        for ((slot, on) in s.net.route(gate(first))) if (s.on[slot] != on) s = SwitchesGame.input(s, slot, 700)
        return SwitchesGame.tick(s, s.exitAtGate(first).toLong() + 1) to first
    }

    @Test fun rightGateScores() {
        val (s, first) = lone(21) { it.target }
        val a = s.arrivals.single()
        assertEquals(first.id, a.id)
        assertTrue(a.correct)
        assertEquals(first.target, a.gate)
        assertEquals(1, s.round.correct)
        assertEquals(SwitchesRules.POINTS, s.round.score)
        assertTrue(s.beads.isEmpty())
    }

    @Test fun wrongGateIsMissAndLoosensPace() {
        val start = SwitchesGame.start(21, 2, false)
        val (s, first) = lone(21) { b -> (0 until 4).first { it != b.target } }
        val a = s.arrivals.single()
        assertFalse(a.correct)
        assertNotEquals(first.target, a.gate)
        assertEquals(1, s.round.wrong)
        assertEquals(0, s.round.score)
        assertEquals(start.stair.value * 1.15, s.stair.value, 1e-6)
    }

    // Ein perfekter Spieler bringt jede Perle heim, der Takt wird dabei dichter
    @Test fun perfectPlayIsPossibleAndSpeedsUp() {
        for (level in listOf(1, 4, 8, 12)) {
            val start = SwitchesGame.start(level * 101L, level, false)
            val end = Pilot.play(start, SwitchesRules.DURATION_MS)
            assertTrue(end.round.isOver)
            assertEquals(0, end.round.wrong, "Stufe $level")
            assertTrue(end.round.correct >= 15, "Stufe $level: ${end.round.correct}")
            assertTrue(end.stair.value < start.stair.value)
            assertEquals(minOf(level + 1, SwitchesRules.MAX_LEVEL), end.level)
        }
    }

    @Test fun inputFlipsOnlyValidSwitch() {
        var s = SwitchesGame.tick(SwitchesGame.start(4, 3, false), 1_000)
        val before = s.on
        s = SwitchesGame.input(s, 0, 1_200)
        assertNotEquals(before[0], s.on[0])
        assertEquals(1_200.0, s.flippedAt[0], 1e-9)
        assertEquals(before.drop(1), s.on.drop(1))
        assertEquals(s.on, SwitchesGame.input(s, 99, 1_300).on)
        assertEquals(s.on, SwitchesGame.input(s, -1, 1_300).on)
    }

    @Test fun overIgnoresInputAndStopsSpawning() {
        var s = idle(SwitchesGame.start(9, 5, false), SwitchesRules.DURATION_MS + 5_000, 50)
        assertTrue(s.round.isOver)
        assertEquals(SwitchesRules.DURATION_MS, s.round.now)
        assertEquals(s.on, SwitchesGame.input(s, 0, SwitchesRules.DURATION_MS + 6_000).on)
        val last = s.lastSpawnAt
        assertTrue(last < SwitchesRules.DURATION_MS - SwitchesRules.SPAWN_STOP_MS)
        // Zeit laeuft nie rueckwaerts
        s = SwitchesGame.tick(s, 10)
        assertEquals(SwitchesRules.DURATION_MS, s.round.now)
    }

    @Test fun targetsVaryWithoutLongRuns() {
        val all = HashMap<Int, Int>()
        var x = SwitchesGame.start(5, 8, false)
        var prev = -1
        var run = 0
        var t = 0L
        while (t < 80_000) {
            t += 40
            x = SwitchesGame.tick(x, t)
            for (b in x.beads) if (b.id !in all) {
                all[b.id] = b.target
                run = if (b.target == prev) run + 1 else 1
                prev = b.target
                assertTrue(run <= SwitchesRules.MAX_RUN)
            }
        }
        assertTrue(x.nextId > 15)
        assertTrue(all.values.toSet().size >= x.net.gateCount / 2)
    }

    @Test fun relaxedIsSlower() {
        val a = SwitchesGame.start(2, 4, false)
        val b = SwitchesGame.start(2, 4, true)
        assertEquals(a.speed / SwitchesRules.RELAXED, b.speed, 1e-12)
        assertEquals(a.intervalMs * SwitchesRules.RELAXED, b.intervalMs, 1e-9)
    }

    @Test fun levelAfterRound() {
        fun r(c: Int, w: Int) = Round(correct = c, wrong = w)
        assertEquals(5, SwitchesRules.nextLevel(4, r(27, 3)))
        assertEquals(4, SwitchesRules.nextLevel(4, r(26, 4)))
        assertEquals(4, SwitchesRules.nextLevel(4, r(7, 3)))
        assertEquals(3, SwitchesRules.nextLevel(4, r(6, 4)))
        assertEquals(4, SwitchesRules.nextLevel(4, r(3, 2)))
        assertEquals(1, SwitchesRules.nextLevel(1, r(0, 10)))
        assertEquals(SwitchesRules.MAX_LEVEL, SwitchesRules.nextLevel(SwitchesRules.MAX_LEVEL, r(30, 0)))
        assertEquals(7, SwitchesGame.startLevel(7))
        assertEquals(1, SwitchesGame.startLevel(0))
        assertEquals(SwitchesRules.MAX_LEVEL, SwitchesGame.startLevel(99))
    }

    // Zeitpunkt, an dem die Perle bei unveraenderten Weichen ihr Tor erreicht
    private fun SwitchesState.exitAtGate(b: Bead): Double {
        var at = exitAt(b)
        var e = b.edge
        while (true) {
            val n = net.edges[e].to
            if (net.nodes[n].kind == Kind.GATE) return at
            e = net.next(n, on[net.nodes[n].slot])
            at += net.edges[e].track.length / speed
        }
    }
}
