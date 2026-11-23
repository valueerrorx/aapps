package solutions.xapient.thinkr.domain.switches

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

class NetTest {
    private fun all(): List<Pair<String, Net>> = NetBook.plans.flatMap { (gates, plans) ->
        plans.flatMapIndexed { i, rows -> List(NetBook.VARIANTS) { v -> "$gates/$i/v$v" to Net.parse(rows, v) } }
    }

    @Test fun everyGateCountHasNets() {
        for (g in 3..SwitchesRules.MAX_GATES) assertTrue("Torzahl $g", (NetBook.plans[g]?.size ?: 0) >= 2)
        for (l in 1..SwitchesRules.MAX_LEVEL) assertTrue(SwitchesRules.gates(l) in NetBook.plans)
    }

    @Test fun treeShape() {
        for ((name, net) in all()) {
            val gates = name.substringBefore('/').toInt()
            assertEquals(name, gates, net.gateCount)
            assertEquals(name, gates - 1, net.switches.size)
            assertEquals(name, net.nodes.size - 1, net.edges.size)
            // jede Weiche zwei Abgaenge, Tore keinen, Portal einen
            for (n in net.nodes) {
                val want = when (n.kind) {
                    Kind.PORTAL -> 1
                    Kind.SWITCH -> 2
                    Kind.GATE -> 0
                }
                assertEquals(name, want, n.out.size)
            }
        }
    }

    // Jedes Tor ist vom Portal erreichbar, und genau mit seiner Weichenstellung
    @Test fun everyGateReachableByItsRoute() {
        for ((name, net) in all()) {
            val routes = (0 until net.gateCount).map { net.route(it) }
            for (g in 0 until net.gateCount) {
                var n = net.portal
                var e = net.portalEdge
                var steps = 0
                while (true) {
                    n = net.edges[e].to
                    if (net.nodes[n].kind == Kind.GATE) break
                    e = net.next(n, routes[g].getValue(net.nodes[n].slot))
                    assertTrue(name, ++steps < 50)
                }
                assertEquals(name, g, net.nodes[n].gate)
            }
            assertEquals(name, net.gateCount, routes.toSet().size)
        }
    }

    // Leuchtlinien wie ein Linienplan: nur waagrecht, senkrecht oder 45 Grad
    @Test fun onlyStraightAndDiagonal() {
        for ((name, net) in all()) for (e in net.edges) {
            e.track.points.zipWithNext { a, b ->
                val dx = abs(b.x - a.x)
                val dy = abs(b.y - a.y)
                assertTrue(name, dx == 0.0 || dy == 0.0 || abs(dx - dy) < 1e-9)
            }
        }
    }

    // Lesbar auf kleinem Schirm: Knoten mit Abstand, Tore frei von fremden Schienen
    @Test fun spacing() {
        for ((name, net) in all()) {
            val pts = net.nodes.map { it.at }
            for (i in pts.indices) for (j in i + 1 until pts.size) {
                assertTrue("$name ${pts[i]} ${pts[j]}", (pts[i] - pts[j]).length >= 2.8 - 1e-9)
            }
            for (g in net.gates) {
                val p = net.nodes[g].at
                net.edges.forEachIndexed { k, e ->
                    if (e.to == g) return@forEachIndexed
                    val d = e.track.sample(0.1).minOf { (it - p).length }
                    assertTrue("$name Tor $p Kante $k: $d", d >= 1.9)
                }
            }
            for (n in net.nodes) assertTrue(name, n.at.x in 0.0..net.width && n.at.y in 0.0..net.height)
        }
    }

    @Test fun variantsMirror() {
        val rows = NetBook.plans.getValue(3)[0]
        val a = Net.parse(rows, 0)
        val b = Net.parse(rows, 1)
        val c = Net.parse(rows, 2)
        val pa = a.nodes[a.portal].at
        assertEquals(a.width - pa.x, b.nodes[b.portal].at.x, 1e-9)
        assertEquals(pa.y, b.nodes[b.portal].at.y, 1e-9)
        assertEquals(a.height - pa.y, c.nodes[c.portal].at.y, 1e-9)
    }

    @Test fun rejectsBrokenPlans() {
        // Sackgasse: Schiene endet im Nichts
        assertThrows(IllegalArgumentException::class.java) { Net.parse(listOf("P-S-A", "...|.", "....."), 0) }
        // Tor-Buchstaben mit Luecke
        assertThrows(IllegalArgumentException::class.java) { Net.parse(listOf("P-S-A", "...\\.", "....C"), 0) }
        // zwei Portale
        assertThrows(IllegalStateException::class.java) { Net.parse(listOf("P-A", "...", "P-B"), 0) }
        // Schleife
        assertThrows(IllegalArgumentException::class.java) {
            Net.parse(listOf("P-S--o", "..|..|", "..o--S", ".....|", ".....A"), 0)
        }
    }

    @Test fun smallPlanParses() {
        val n = Net.parse(listOf("P-S-A", "...\\.", "....B"), 0)
        assertEquals(2, n.gateCount)
        assertEquals(mapOf(0 to false), n.route(0))
        assertEquals(mapOf(0 to true), n.route(1))
        assertFalse(n.edges.isEmpty())
    }
}

class TrackTest {
    @Test fun straight() {
        val t = Track(listOf(Pt(0.0, 0.0), Pt(4.0, 0.0)))
        assertEquals(4.0, t.length, 1e-9)
        assertEquals(Pt(2.0, 0.0), t.at(2.0))
        assertEquals(Pt(4.0, 0.0), t.at(99.0))
        assertEquals(Pt(1.0, 0.0), t.heading)
    }

    @Test fun roundedRightAngle() {
        val t = Track(listOf(Pt(0.0, 0.0), Pt(3.0, 0.0), Pt(3.0, 3.0)), corner = 1.0)
        assertEquals(6.0 - 2.0 + PI / 2, t.length, 1e-9)
        val end = t.at(t.length)
        assertEquals(3.0, end.x, 1e-9)
        assertEquals(3.0, end.y, 1e-9)
        // Bogenmitte liegt innen vor der Ecke
        val mid = t.at(t.length / 2)
        assertEquals(hypot(3.0 - mid.x, mid.y), (Pt(3.0, 0.0) - mid).length, 1e-9)
        assertTrue(mid.x < 3.0 && mid.y > 0.0)
    }

    @Test fun continuousAlongDiagonalBends() {
        val t = Track(listOf(Pt(0.0, 0.0), Pt(0.0, 3.0), Pt(3.0, 6.0), Pt(3.0, 9.0)))
        t.sample(0.05).zipWithNext { a, b -> assertTrue((b - a).length < 0.06) }
        assertEquals(Pt(3.0, 9.0), t.at(t.length))
    }
}
