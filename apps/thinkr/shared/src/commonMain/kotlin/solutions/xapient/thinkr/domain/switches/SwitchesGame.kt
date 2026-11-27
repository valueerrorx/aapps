package solutions.xapient.thinkr.domain.switches

import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import solutions.xapient.thinkr.domain.Staircase
import kotlin.math.pow
import kotlin.random.Random

// Stufen, Tempo und Takt an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object SwitchesRules {
    const val MAX_LEVEL = 12
    const val MAX_GATES = 10
    const val DURATION_MS = 90_000L
    const val POINTS = 40
    const val FIRST_MS = 600.0
    const val SPAWN_STOP_MS = 2_000L
    const val RELAXED = 1.5
    const val UP_PERCENT = 90
    const val KEEP_PERCENT = 70
    const val MIN_ARRIVALS = 6
    const val MAX_RUN = 2
    const val SHOW_MS = 1_200.0

    // Stufe 1 = 3 Tore, ab Stufe 8 alle 10; darueber nur schneller
    fun gates(level: Int): Int = (level + 2).coerceAtMost(MAX_GATES)

    // Rasterfelder je Sekunde
    fun speed(level: Int): Double = 2.2 * 1.035.pow(level - 1)

    // Abstand zwischen zwei Abfahrten zu Rundenbeginn in ms
    fun interval(level: Int): Double = 3800.0 * 0.97.pow(level - 1)

    // dichter nach 3 richtigen Ankuenften in Folge (-8 %), lockerer nach jeder Fehlleitung (+15 %)
    fun stair(level: Int): Staircase =
        Staircase(interval(level), min = 900.0, max = 6000.0, hitsToStep = 3, hitFactor = 0.92, missFactor = 1.15)

    // Nach der Runde: ab 90 % hinauf, ab 70 % gleich, sonst hinunter; zu wenige Ankuenfte zaehlen nicht
    fun nextLevel(start: Int, r: Round): Int = when {
        r.answered < MIN_ARRIVALS -> start
        r.accuracy >= UP_PERCENT -> start + 1
        r.accuracy >= KEEP_PERCENT -> start
        else -> start - 1
    }.coerceIn(1, MAX_LEVEL)
}

// Perle auf Kante edge seit enteredAt (Spielzeit ms); prevEdge fuer die Lichtspur
data class Bead(val id: Int, val target: Int, val edge: Int, val enteredAt: Double, val prevEdge: Int = -1)

// Ankunft am Tor, fuer Wertung und Optik
data class Arrival(val id: Int, val target: Int, val gate: Int, val at: Double, val correct: Boolean)

data class SwitchesState(
    override val round: Round,
    override val startLevel: Int,
    val net: Net,
    // Tor -> Farbe/Form (Index in der Palette)
    val colors: List<Int>,
    // Weichenstellung je slot
    val on: List<Boolean>,
    val flippedAt: List<Double>,
    val beads: List<Bead>,
    val arrivals: List<Arrival>,
    val nextId: Int,
    val nextAt: Double,
    val lastSpawnAt: Double,
    val stair: Staircase,
    // Rasterfelder je ms
    val speed: Double,
    val relaxed: Boolean,
    val seed: Long,
    val lastTarget: Int = -1,
    val run: Int = 0,
) : Play {
    override val level: Int get() = SwitchesRules.nextLevel(startLevel, round)
    private val scale: Double get() = if (relaxed) SwitchesRules.RELAXED else 1.0
    val intervalMs: Double get() = stair.value * scale

    // Weg auf der aktuellen Kante
    fun progress(b: Bead): Double = ((round.now - b.enteredAt) * speed).coerceIn(0.0, net.edges[b.edge].track.length)

    fun exitAt(b: Bead): Double = b.enteredAt + net.edges[b.edge].track.length / speed
}

// Weichen: Perlen rollen aus dem Portal, Weichen lenken sie; Eingabe = slot der angetippten Weiche
object SwitchesGame : MiniGame<SwitchesState, Int> {
    // Stufe aendert sich nur um 1 je Runde, darum kein Abschlag; aufgewaermt wird ueber den lockeren Takt zu Beginn
    override fun startLevel(saved: Int): Int = saved.coerceIn(1, SwitchesRules.MAX_LEVEL)

    override fun start(seed: Long, level: Int, relaxed: Boolean): SwitchesState {
        val l = level.coerceIn(1, SwitchesRules.MAX_LEVEL)
        val rnd = Random(seed)
        val plans = NetBook.plans.getValue(SwitchesRules.gates(l))
        val net = Net.parse(plans[rnd.nextInt(plans.size)], rnd.nextInt(NetBook.VARIANTS))
        val scale = if (relaxed) SwitchesRules.RELAXED else 1.0
        return SwitchesState(
            round = Round(durationMs = SwitchesRules.DURATION_MS),
            startLevel = l,
            net = net,
            colors = (0 until net.gateCount).shuffled(rnd),
            on = List(net.switches.size) { rnd.nextBoolean() },
            flippedAt = List(net.switches.size) { LONG_AGO },
            beads = emptyList(),
            arrivals = emptyList(),
            nextId = 0,
            nextAt = SwitchesRules.FIRST_MS,
            lastSpawnAt = LONG_AGO,
            stair = SwitchesRules.stair(l),
            speed = SwitchesRules.speed(l) / 1000.0 / scale,
            relaxed = relaxed,
            seed = seed,
        )
    }

    // Ereignisse (Abfahrt, Knoten erreicht) streng nach Zeit abarbeiten: Ergebnis haengt nicht an der Bildrate
    override fun tick(s: SwitchesState, t: Long): SwitchesState {
        val until = s.round.at(t)
        val now = until.now.toDouble()
        val stop = (s.round.durationMs - SwitchesRules.SPAWN_STOP_MS).toDouble()
        var x = s
        while (true) {
            val spawnAt = if (x.nextAt < stop) x.nextAt else Double.POSITIVE_INFINITY
            var bi = -1
            var cross = Double.POSITIVE_INFINITY
            x.beads.forEachIndexed { i, b ->
                val c = x.exitAt(b)
                if (c < cross) { cross = c; bi = i }
            }
            val ev = minOf(spawnAt, cross)
            if (ev > now) break
            x = if (spawnAt <= cross) spawn(x) else arrive(x, bi, cross)
        }
        val keep = x.arrivals.filter { now - it.at < SwitchesRules.SHOW_MS }
        return x.copy(round = x.round.at(until.now), arrivals = keep)
    }

    override fun input(s: SwitchesState, input: Int, t: Long): SwitchesState {
        val x = tick(s, t)
        if (x.round.isOver || input !in x.on.indices) return x
        return x.copy(
            on = x.on.toMutableList().also { it[input] = !it[input] },
            flippedAt = x.flippedAt.toMutableList().also { it[input] = x.round.now.toDouble() },
        )
    }

    private fun spawn(s: SwitchesState): SwitchesState {
        val n = s.net.gateCount
        val rnd = Random(s.seed * 7_919L + s.nextId)
        var target = rnd.nextInt(n)
        // hoechstens MAX_RUN gleiche Ziele hintereinander
        if (target == s.lastTarget && s.run >= SwitchesRules.MAX_RUN) target = (target + 1 + rnd.nextInt(n - 1)) % n
        val at = s.nextAt
        return s.copy(
            beads = s.beads + Bead(s.nextId, target, s.net.portalEdge, at),
            nextId = s.nextId + 1,
            nextAt = at + s.intervalMs,
            lastSpawnAt = at,
            lastTarget = target,
            run = if (target == s.lastTarget) s.run + 1 else 1,
        )
    }

    private fun arrive(s: SwitchesState, i: Int, at: Double): SwitchesState {
        val b = s.beads[i]
        val node = s.net.nodes[s.net.edges[b.edge].to]
        if (node.kind == Kind.SWITCH) {
            val next = s.net.next(s.net.edges[b.edge].to, s.on[node.slot])
            return s.copy(beads = s.beads.toMutableList().also { it[i] = b.copy(edge = next, enteredAt = at, prevEdge = b.edge) })
        }
        val correct = node.gate == b.target
        return s.copy(
            round = if (correct) s.round.hit(SwitchesRules.POINTS) else s.round.miss(),
            stair = if (correct) s.stair.hit() else s.stair.miss(),
            beads = s.beads.toMutableList().also { it.removeAt(i) },
            arrivals = s.arrivals + Arrival(b.id, b.target, node.gate, at, correct),
        )
    }

    const val LONG_AGO = -1e9
}
