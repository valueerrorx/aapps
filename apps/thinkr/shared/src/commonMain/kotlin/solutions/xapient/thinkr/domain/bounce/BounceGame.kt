package solutions.xapient.thinkr.domain.bounce

import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import kotlin.math.abs
import kotlin.random.Random

// Rand des Felds; dx/dy = Richtung nach innen (y waechst nach unten)
enum class Edge(val dx: Int, val dy: Int) { TOP(0, 1), RIGHT(-1, 0), BOTTOM(0, -1), LEFT(1, 0) }

// Randfeld: Seite und Index entlang der Seite (links→rechts bzw. oben→unten)
data class Port(val edge: Edge, val i: Int)

// Prisma: SLASH = „/" (unten links nach oben rechts), BACKSLASH = „\"
enum class Prism { SLASH, BACKSLASH }

data class Cell(val x: Int, val y: Int)

// Weg des Lichts: Eckpunkte von aussen nach aussen (Zellkoordinaten), Prismen in Trefferfolge
data class Trace(val corners: List<Cell>, val hits: List<Int>, val exit: Port) {
    // Laenge in Zellen (nur waagrecht und senkrecht)
    val length: Int get() = corners.zipWithNext().sumOf { (a, b) -> abs(a.x - b.x) + abs(a.y - b.y) }

    // verschiedene getroffene Prismen (zaehlen fuer die Punkte)
    val distinct: Int get() = hits.toSet().size
}

object Optics {
    // Feld ausserhalb vor dem Randfeld
    fun outside(p: Port, side: Int): Cell = when (p.edge) {
        Edge.TOP -> Cell(p.i, -1)
        Edge.BOTTOM -> Cell(p.i, side)
        Edge.LEFT -> Cell(-1, p.i)
        Edge.RIGHT -> Cell(side, p.i)
    }

    fun ports(side: Int): List<Port> = Edge.entries.flatMap { e -> List(side) { Port(e, it) } }

    // 90°-Abprall; „/" tauscht und dreht das Vorzeichen, „\" tauscht nur
    fun reflect(p: Prism, dx: Int, dy: Int): Pair<Int, Int> = when (p) {
        Prism.SLASH -> -dy to -dx
        Prism.BACKSLASH -> dy to dx
    }

    // Strahl vom Randfeld durch das Feld; Spiegelwege sind umkehrbar, die Schranke schuetzt trotzdem
    fun trace(side: Int, prisms: Map<Int, Prism>, entry: Port): Trace {
        var c = outside(entry, side)
        var dx = entry.edge.dx
        var dy = entry.edge.dy
        val corners = arrayListOf(c)
        val hits = ArrayList<Int>()
        repeat(4 * side * side + 8) {
            c = Cell(c.x + dx, c.y + dy)
            if (c.x !in 0 until side || c.y !in 0 until side) {
                corners += c
                val exit = when {
                    c.x < 0 -> Port(Edge.LEFT, c.y)
                    c.x >= side -> Port(Edge.RIGHT, c.y)
                    c.y < 0 -> Port(Edge.TOP, c.x)
                    else -> Port(Edge.BOTTOM, c.x)
                }
                return Trace(corners, hits, exit)
            }
            val i = c.y * side + c.x
            val p = prisms[i] ?: return@repeat
            val (nx, ny) = reflect(p, dx, dy)
            dx = nx
            dy = ny
            hits += i
            corners += c
        }
        error("Strahl ohne Ausgang")
    }
}

// Stufen und Zeiten an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object BounceRules {
    const val MAX_LEVEL = 14
    const val POINTS = 20
    const val LEAD_MS = 450L
    const val SPEED = 9.0
    const val HOLD_OK_MS = 750L
    const val HOLD_BAD_MS = 1_300L
    const val RELAXED = 1.5

    fun side(level: Int): Int = when {
        level >= 12 -> 8
        level >= 8 -> 7
        level >= 4 -> 6
        else -> 5
    }

    // Prismen auf dem Feld: 2 bis 10
    fun prisms(level: Int): Int = 2 + (level - 1) * 2 / 3

    // so viele muss das Licht wirklich treffen, sonst waere die Aufgabe zu leicht
    fun minHits(level: Int): Int = minOf(prisms(level), 1 + level / 3)

    // Anzeigezeit sinkt leicht mit der Stufe
    fun showMs(level: Int): Long = (3_000L - 100L * (level - 1)).coerceAtLeast(1_800L)

    // richtig +1, falsch gleich, zweimal falsch in Folge -1
    fun next(level: Int, correct: Boolean, missRun: Int): Int = when {
        correct -> level + 1
        missRun >= 2 -> level - 1
        else -> level
    }.coerceIn(1, MAX_LEVEL)
}

// Eine Aufgabe; said = getipptes Randfeld, scored = schon gewertet (erst wenn das Licht draussen ist)
data class Shot(
    val no: Int,
    val level: Int,
    val side: Int,
    val prisms: Map<Int, Prism>,
    val entry: Port,
    val trace: Trace,
    val startAt: Long,
    val said: Port? = null,
    val saidAt: Long = -1,
    val scored: Boolean = false,
) {
    val answered: Boolean get() = saidAt >= 0
    val correct: Boolean get() = said == trace.exit

    // Zeitpunkt (ms ab Antwort), zu dem das Licht die Strecke bis Eckpunkt k erreicht
    fun cornerMs(k: Int): Double {
        var d = 0
        for (j in 1..k) d += abs(trace.corners[j].x - trace.corners[j - 1].x) + abs(trace.corners[j].y - trace.corners[j - 1].y)
        return d / BounceRules.SPEED * 1000.0
    }

    val runMs: Long get() = (trace.length / BounceRules.SPEED * 1000.0).toLong()
}

enum class BouncePhase { LEAD, SHOW, ANSWER, RUN, HOLD }

data class BounceState(
    override val round: Round,
    override val startLevel: Int,
    val seed: Long,
    val relaxed: Boolean,
    // Stufe nach den gewerteten Aufgaben
    val stage: Int,
    val missRun: Int,
    val shot: Shot,
    // Abpraller frueherer Aufgaben (fuer die Raster-Haptik)
    val bounced: Int = 0,
) : Play {
    override val level: Int get() = stage

    override val steps: Int get() = bounced + bouncesAt(round.now)

    private val scale: Double get() = if (relaxed) BounceRules.RELAXED else 1.0
    val hideAt: Long get() = shot.startAt + BounceRules.LEAD_MS + (BounceRules.showMs(shot.level) * scale).toLong()
    val doneAt: Long get() = if (shot.answered) shot.saidAt + shot.runMs else Long.MAX_VALUE
    val nextAt: Long get() = doneAt + if (shot.correct) BounceRules.HOLD_OK_MS else BounceRules.HOLD_BAD_MS

    // Abpraller der laufenden Aufgabe bis t
    fun bouncesAt(t: Long): Int {
        if (!shot.answered) return 0
        val dt = (t - shot.saidAt).toDouble()
        return (1..shot.trace.hits.size).count { shot.cornerMs(it) <= dt }
    }

    fun phase(t: Long = round.now): BouncePhase = when {
        t < shot.startAt + BounceRules.LEAD_MS -> BouncePhase.LEAD
        t < hideAt -> BouncePhase.SHOW
        !shot.answered -> BouncePhase.ANSWER
        t < doneAt -> BouncePhase.RUN
        else -> BouncePhase.HOLD
    }
}

// Bande: Prismen kurz zeigen, ausblenden, Austrittsstelle des Lichts antippen; Eingabe = Randfeld
object BounceGame : MiniGame<BounceState, Port> {
    override fun startLevel(saved: Int): Int = (saved.coerceIn(1, BounceRules.MAX_LEVEL) - 2).coerceAtLeast(1)

    override fun start(seed: Long, level: Int, relaxed: Boolean): BounceState {
        val l = level.coerceIn(1, BounceRules.MAX_LEVEL)
        return BounceState(Round(), l, seed, relaxed, l, 0, newShot(seed, 0, l, 0))
    }

    fun newShot(seed: Long, no: Int, level: Int, at: Long): Shot {
        val rng = Random(seed * 1_000_003L + no * 7_919L + 29)
        val side = BounceRules.side(level)
        val (prisms, entry) = layout(rng, side, BounceRules.prisms(level), BounceRules.minHits(level))
        return Shot(no, level, side, prisms, entry, Optics.trace(side, prisms, entry), at)
    }

    // Zufaellige Prismen und ein Eingang, dessen Weg mindestens need verschiedene Prismen trifft
    fun layout(rng: Random, side: Int, count: Int, need: Int): Pair<Map<Int, Prism>, Port> {
        var want = need
        while (true) {
            repeat(300) {
                val cells = (0 until side * side).shuffled(rng).take(count)
                val prisms = cells.associateWith { if (rng.nextBoolean()) Prism.SLASH else Prism.BACKSLASH }
                for (p in Optics.ports(side).shuffled(rng)) {
                    if (Optics.trace(side, prisms, p).distinct >= want) return prisms to p
                }
            }
            // praktisch nie: Anspruch senken statt haengen
            want = (want - 1).coerceAtLeast(1)
        }
    }

    override fun tick(s: BounceState, t: Long): BounceState {
        var x = s.copy(round = s.round.at(t))
        // gewertet wird, wenn das Licht draussen ist (oder die Zeit um)
        if (x.shot.answered && !x.shot.scored && (t >= x.doneAt || x.round.isOver)) x = score(x)
        // naechste Aufgabe nach der Aufloesung, auch mehrere nach einem langen Frame
        while (x.shot.scored && x.nextAt <= t && x.nextAt < x.round.durationMs) {
            val n = x.shot.trace.hits.size
            x = x.copy(bounced = x.bounced + n, shot = newShot(x.seed, x.shot.no + 1, x.stage, x.nextAt))
        }
        return x
    }

    private fun score(x: BounceState): BounceState {
        val sh = x.shot
        val ok = sh.correct
        val run = if (ok) 0 else x.missRun + 1
        val round = if (ok) x.round.hit(BounceRules.POINTS * sh.trace.distinct) else x.round.miss()
        val stage = BounceRules.next(sh.level, ok, run)
        return x.copy(round = round, stage = stage, missRun = if (run >= 2) 0 else run, shot = sh.copy(scored = true))
    }

    override fun input(s: BounceState, input: Port, t: Long): BounceState {
        val x = tick(s, t)
        val sh = x.shot
        val valid = input.i in 0 until sh.side && input != sh.entry
        if (x.round.isOver || x.phase(t) != BouncePhase.ANSWER || !valid) return x
        return x.copy(shot = sh.copy(said = input, saidAt = t))
    }
}
