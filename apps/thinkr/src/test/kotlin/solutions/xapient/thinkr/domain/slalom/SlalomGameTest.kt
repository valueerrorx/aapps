package solutions.xapient.thinkr.domain.slalom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.thinkr.domain.Round
import kotlin.math.abs

class SlalomGameTest {
    private val g = SlalomGame

    // perfekter Fahrer: wechselt rechtzeitig vor jeder Reihe auf deren freie Spur
    private fun autopilot(s0: SlalomState, until: Long, step: Long = 16): SlalomState {
        var s = s0
        var t = s.round.now
        while (t < until && !s.round.isOver) {
            t += step
            s = g.tick(s, t)
            val row = s.rows.firstOrNull { it.z > s.z } ?: continue
            if (s.lane != row.safe && t - s.laneAt > SlalomRules.LANE_MS) s = g.input(s, if (row.safe > s.lane) 1 else -1, t)
        }
        return s
    }

    // Zustand, in dem die naechste Reihe die aktuelle Spur sperrt
    private fun facing(s0: SlalomState): Pair<SlalomState, Row> {
        var s = s0
        var t = 0L
        while (!s.round.isOver) {
            val row = s.rows.first { it.z > s.z }
            if (s.lane in row.blocked) return s to row
            t += 16
            s = g.tick(s, t)
        }
        return s to s.rows.first()
    }

    @Test fun everyRowLeavesAFreeLaneReachableInTime() {
        for (level in listOf(1, 5, 9, 12, 14)) {
            val lanes = SlalomRules.lanes(level)
            val v = SlalomRules.speed(level)
            var prev = Rows.make(level.toLong(), 0, 0.0, lanes / 2, lanes, level, v)
            repeat(600) { n ->
                val r = Rows.make(level.toLong(), n + 1, prev.z, prev.safe, lanes, level, v)
                assertFalse(r.safe in r.blocked)
                assertTrue(r.blocked.size in 1 until lanes)
                assertTrue(r.obstacles.all { it.lane in 0 until lanes && it.from in 0 until lanes })
                val moved = abs(r.safe - prev.safe)
                val seconds = (r.z - prev.z) / v
                assertTrue("$level $n", seconds >= SlalomRules.REACT_S + SlalomRules.SHIFT_S * moved - 1e-9)
                // rutschende Brocken sind frueh genug am Ziel
                r.obstacles.filter { it.slides }.forEach { o ->
                    assertTrue((o.slideAt - o.slideLen) / v >= SlalomRules.REACT_S + SlalomRules.SHIFT_S)
                    assertEquals(o.lane.toDouble(), o.laneAt(0.0), 1e-9)
                    assertEquals(o.from.toDouble(), o.laneAt(o.slideAt + 1), 1e-9)
                }
                prev = r
            }
        }
    }

    @Test fun moreLanesAndTrafficWithLevel() {
        assertEquals(3, SlalomRules.lanes(1))
        assertEquals(4, SlalomRules.lanes(5))
        assertEquals(5, SlalomRules.lanes(10))
        assertEquals(0.0, SlalomRules.slideShare(5), 0.0)
        assertTrue(SlalomRules.slideShare(12) > 0.0)
        for (l in 2..SlalomRules.MAX_LEVEL) assertTrue(SlalomRules.speed(l) > SlalomRules.speed(l - 1))
        for (l in 1..SlalomRules.MAX_LEVEL) assertEquals(l, SlalomRules.levelOf(SlalomRules.speed(l)))
    }

    @Test fun perfectDriverNeverCrashes() {
        for (level in listOf(1, 6, 11, 14)) {
            val end = autopilot(g.start(level * 7L, level, false), Round.DURATION_MS + 100)
            assertEquals("$level", 0, end.crashes)
            assertTrue(end.round.isOver)
            assertEquals(Round.DURATION_MS, end.round.now)
            assertTrue(end.passed > 30)
        }
    }

    @Test fun cleanDrivingSpeedsUpAndRaisesLevelAtMostThree() {
        val s = g.start(3, 4, false)
        val end = autopilot(s, Round.DURATION_MS)
        // 60 s sauber: fuenfmal schneller (nach 10, 20, … 50 s)
        assertEquals(SlalomRules.speed(4) * Math.pow(SlalomRules.SPEED_UP, 5.0), end.speed, 1e-6)
        assertEquals(4 + SlalomRules.MAX_UP, end.level)
    }

    @Test fun crashCostsLifeSpeedAndShakes() {
        val (s, row) = facing(g.start(5, 3, false))
        val v = s.speed
        val t = s.round.now + ((row.z - s.z) / v * 1000).toLong() + 20
        val x = g.tick(s, t)
        assertEquals(SlalomRules.LIVES - 1, x.lives)
        assertEquals(1, x.crashes)
        assertEquals(1, x.jolts)
        assertEquals(1, x.round.wrong)
        assertEquals(v * SlalomRules.SPEED_DOWN, x.speed, 1e-9)
        assertTrue(x.crashAt in (t - 40)..t)
    }

    @Test fun threeCrashesEndTheRound() {
        var s = g.start(5, 3, false)
        while (!s.round.isOver) {
            val (f, row) = facing(s)
            if (f.round.isOver) break
            s = g.tick(f, f.round.now + ((row.z - f.z) / f.speed * 1000).toLong() + 5)
            // nach dem Schonmoment weiter
            if (!s.round.isOver) s = g.tick(s, s.safeUntil + 1)
        }
        assertEquals(SlalomRules.LIVES, s.crashes)
        assertTrue(s.round.now < Round.DURATION_MS)
        assertEquals(0, s.lives)
        assertEquals(s, g.input(s, 1, s.round.now + 100))
    }

    @Test fun laneChangeCountsFromItsMiddle() {
        val s = g.start(1, 2, false)
        val x = g.input(s, -1, 100)
        assertEquals(0, x.lane)
        assertEquals(1, x.laneFrom)
        assertEquals(1, x.laneAtTime(100 + SlalomRules.LANE_MS / 2 - 1))
        assertEquals(0, x.laneAtTime(100 + SlalomRules.LANE_MS / 2))
        // am Rand bleibt es
        val y = g.input(x, -1, 400)
        assertEquals(0, y.lane)
        assertEquals(x.laneAt, y.laneAt)
    }

    @Test fun dodgingJustInTimeIsANearMiss() {
        val (s, row) = facing(g.start(8, 3, false))
        val passAt = s.round.now + ((row.z - s.z) / s.speed * 1000).toLong()
        val dir = if (row.safe > s.lane) 1 else -1
        // erst 300 ms vor der Reihe wechseln
        val moved = g.input(g.tick(s, passAt - 300), dir, passAt - 300)
        if (moved.lane !in row.blocked) {
            val x = g.tick(moved, passAt + 20)
            assertEquals(0, x.crashes)
            val near = moved.lane - 1 in row.blocked || moved.lane + 1 in row.blocked
            assertEquals(if (near) 1 else 0, x.nears)
        }
    }

    @Test fun resultIndependentOfFrameRate() {
        val a = g.tick(g.start(4, 6, false), 30_000)
        var b = g.start(4, 6, false)
        var t = 0L
        while (t < 30_000) {
            t += 17
            b = g.tick(b, minOf(t, 30_000))
        }
        assertEquals(a.crashes, b.crashes)
        assertEquals(a.round.score, b.round.score)
        assertEquals(a.speed, b.speed, 1e-9)
        assertEquals(a.z, b.z, 0.05)
    }

    @Test fun relaxedIsSlowerButRatedTheSame() {
        val r = g.start(2, 6, true)
        assertEquals(SlalomRules.speed(6) / SlalomRules.RELAXED, r.speed, 1e-9)
        assertEquals(6, r.level)
    }

    @Test fun warmUpAndBounds() {
        assertEquals(1, g.startLevel(1))
        assertEquals(6, g.startLevel(8))
        assertEquals(SlalomRules.MAX_LEVEL - 2, g.startLevel(50))
    }
}
