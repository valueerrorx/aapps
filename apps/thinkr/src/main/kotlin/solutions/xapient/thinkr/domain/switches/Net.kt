package solutions.xapient.thinkr.domain.switches

// Knoten: Portal (Start), Weiche (slot = Index der Stellung), Tor (gate = Tor-Index)
enum class Kind { PORTAL, SWITCH, GATE }

data class Node(val kind: Kind, val at: Pt, val gate: Int = -1, val slot: Int = -1, val inEdge: Int = -1, val out: List<Int> = emptyList())

data class Edge(val from: Int, val to: Int, val track: Track)

// Schienennetz als Baum: ein Portal, jede Weiche hat zwei Abgaenge, jedes Blatt ist ein Tor
class Net(val nodes: List<Node>, val edges: List<Edge>, val width: Double, val height: Double) {
    val portal: Int = nodes.indexOfFirst { it.kind == Kind.PORTAL }
    val portalEdge: Int get() = nodes[portal].out.single()
    val switches: List<Int> = nodes.indices.filter { nodes[it].kind == Kind.SWITCH }.sortedBy { nodes[it].slot }
    val gates: List<Int> = nodes.indices.filter { nodes[it].kind == Kind.GATE }.sortedBy { nodes[it].gate }
    val gateCount: Int get() = gates.size

    // Abgang einer Weiche bei Stellung on (false = erster, true = zweiter)
    fun next(node: Int, on: Boolean): Int = nodes[node].out[if (on) 1 else 0]

    // Stellungen, die eine Perle vom Portal zu diesem Tor braucht (slot -> Stellung)
    fun route(gate: Int): Map<Int, Boolean> {
        val m = mutableMapOf<Int, Boolean>()
        var n = gates[gate]
        while (nodes[n].kind != Kind.PORTAL) {
            val e = nodes[n].inEdge
            val parent = edges[e].from
            if (nodes[parent].kind == Kind.SWITCH) m[nodes[parent].slot] = nodes[parent].out[1] == e
            n = parent
        }
        return m
    }

    companion object {
        private val LINKS = mapOf(
            '|' to listOf(0 to -1, 0 to 1),
            '-' to listOf(-1 to 0, 1 to 0),
            '/' to listOf(1 to -1, -1 to 1),
            '\\' to listOf(-1 to -1, 1 to 1),
        )

        // Netz aus Zeichen-Raster: P Portal, S Weiche, A..J Tore, o Knick, | - / \ Schienen, . leer.
        // variant: Bit 0 spiegelt waagrecht, Bit 1 senkrecht
        fun parse(rows: List<String>, variant: Int = 0): Net {
            val cells = HashMap<Pair<Int, Int>, Char>()
            rows.forEachIndexed { y, r -> r.forEachIndexed { x, c -> if (c != '.') cells[x to y] = c } }
            require(cells.isNotEmpty()) { "leeres Netz" }

            fun links(p: Pair<Int, Int>): List<Pair<Int, Int>> {
                val c = cells.getValue(p)
                val own = LINKS[c]
                if (own != null) return own.map { (dx, dy) -> p.first + dx to p.second + dy }.filter { it in cells }
                // Knoten und Knicke haengen an jeder Schiene, die auf sie zeigt
                val out = mutableListOf<Pair<Int, Int>>()
                for (dy in -1..1) for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val q = p.first + dx to p.second + dy
                    val l = LINKS[cells[q] ?: continue] ?: continue
                    if ((-dx to -dy) in l) out += q
                }
                return out
            }

            for ((p, c) in cells) {
                val l = links(p)
                val want = when {
                    c in LINKS -> 2
                    c == 'P' -> 1
                    c == 'S' -> 3
                    c == 'o' -> 2
                    c in 'A'..'J' -> 1
                    else -> error("Zeichen '$c' bei $p")
                }
                require(l.size == want) { "'$c' bei $p hat ${l.size} Verbindungen" }
                for (q in l) require(p in links(q)) { "einseitige Verbindung $p -> $q" }
            }

            val xs = cells.keys.map { it.first }
            val ys = cells.keys.map { it.second }
            val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
            fun pt(p: Pair<Int, Int>): Pt {
                val x = if (variant and 1 != 0) maxX - p.first else p.first - minX
                val y = if (variant and 2 != 0) maxY - p.second else p.second - minY
                return Pt(x.toDouble(), y.toDouble())
            }

            val ids = HashMap<Pair<Int, Int>, Int>()
            val kinds = mutableListOf<Kind>()
            val at = mutableListOf<Pt>()
            val gate = mutableListOf<Int>()
            val slot = mutableListOf<Int>()
            val inEdge = mutableListOf<Int>()
            val outs = mutableListOf<MutableList<Int>>()
            val edges = mutableListOf<Edge>()
            var slots = 0
            fun node(p: Pair<Int, Int>): Int = ids.getOrPut(p) {
                val c = cells.getValue(p)
                kinds += when (c) {
                    'P' -> Kind.PORTAL
                    'S' -> Kind.SWITCH
                    else -> Kind.GATE
                }
                at += pt(p)
                gate += if (c in 'A'..'J') c - 'A' else -1
                slot += if (c == 'S') slots++ else -1
                inEdge += -1
                outs += mutableListOf<Int>()
                kinds.lastIndex
            }

            val portal = cells.entries.singleOrNull { it.value == 'P' }?.key ?: error("genau ein Portal")
            val seen = HashSet<Pair<Int, Int>>().apply { add(portal) }
            // Tiefensuche ab dem Portal; Kanten laufen vom Knoten weg
            val stack = ArrayDeque(listOf(portal to links(portal).single()))
            while (stack.isNotEmpty()) {
                val (start, first) = stack.removeLast()
                val path = mutableListOf(start)
                var prev = start
                var cur = first
                while (true) {
                    require(seen.add(cur)) { "Schleife bei $cur" }
                    val c = cells.getValue(cur)
                    if (c == 'S' || c in 'A'..'J') break
                    path += cur
                    val nx = links(cur).filter { it != prev }
                    prev = cur
                    cur = nx.single()
                }
                path += cur
                val from = node(start)
                val to = node(cur)
                val e = edges.size
                edges += Edge(from, to, Track(corners(path).map(::pt)))
                outs[from] += e
                inEdge[to] = e
                // umgekehrt auf den Stapel: der erste Abgang wird zuerst gebaut und ist Stellung „aus"
                if (cells.getValue(cur) == 'S') links(cur).filter { it != prev }.reversed().forEach { stack.addLast(cur to it) }
            }
            require(seen.size == cells.size) { "nicht erreichte Zellen: ${cells.keys - seen}" }

            val nodes = kinds.indices.map { Node(kinds[it], at[it], gate[it], slot[it], inEdge[it], outs[it].toList()) }
            val gates = nodes.filter { it.kind == Kind.GATE }.map { it.gate }.sorted()
            require(gates == gates.indices.toList()) { "Tore muessen A, B, C … ohne Luecke sein" }
            return Net(nodes, edges, (maxX - minX).toDouble(), (maxY - minY).toDouble())
        }

        // Nur Endpunkte und Richtungswechsel behalten
        private fun corners(path: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
            val out = mutableListOf(path.first())
            for (i in 1 until path.lastIndex) {
                val d1 = path[i].first - path[i - 1].first to path[i].second - path[i - 1].second
                val d2 = path[i + 1].first - path[i].first to path[i + 1].second - path[i].second
                if (d1 != d2) out += path[i]
            }
            out += path.last()
            return out
        }
    }
}
