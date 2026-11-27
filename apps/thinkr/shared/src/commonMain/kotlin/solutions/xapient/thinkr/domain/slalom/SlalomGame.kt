package solutions.xapient.thinkr.domain.slalom

import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

enum class Kind { PILLAR, BOULDER }

// Hindernis; from != lane: rutscht zwischen slideAt und slideAt - slideLen (Abstand vor dem Gleiter) hinueber
data class Obstacle(val lane: Int, val kind: Kind, val from: Int = lane, val slideAt: Double = 0.0, val slideLen: Double = 0.0) {
    val slides: Boolean get() = from != lane

    // Lage in Spuren bei Abstand d vor dem Gleiter
    fun laneAt(d: Double): Double {
        if (!slides) return lane.toDouble()
        val f = ((slideAt - d) / slideLen).coerceIn(0.0, 1.0)
        val e = f * f * (3 - 2 * f)
        return from + (lane - from) * e
    }
}

// Reihe von Hindernissen auf Hoehe z (Weltmass); safe = garantiert freie Spur
data class Row(val no: Int, val z: Double, val obstacles: List<Obstacle>, val safe: Int) {
    val blocked: Set<Int> get() = obstacles.map { it.lane }.toSet()
}

// Stufen und Tempo an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object SlalomRules {
    const val MAX_LEVEL = 14
    const val LIVES = 3
    const val BASE_SPEED = 14.0
    const val LEVEL_STEP = 1.06
    const val SPEED_UP = 1.06
    const val SPEED_DOWN = 0.9
    const val CLEAN_MS = 10_000L
    const val LANE_MS = 160L
    const val REACT_S = 0.62
    const val SHIFT_S = 0.34
    const val VIEW = 70.0
    const val ROW_POINTS = 10
    const val NEAR_BONUS = 15
    const val NEAR_MS = 700L
    const val SAFE_MS = 900L
    const val START_S = 2.2
    const val RELAXED = 1.5

    // je Runde hoechstens so viele Stufen hinauf bzw. hinunter
    const val MAX_UP = 3
    const val MAX_DOWN = 2

    fun lanes(level: Int): Int = when {
        level >= 10 -> 5
        level >= 5 -> 4
        else -> 3
    }

    fun speed(level: Int): Double = BASE_SPEED * LEVEL_STEP.pow(level.coerceIn(1, MAX_LEVEL) - 1)

    // Stufe zum Tempo: hoechste, deren Starttempo erreicht ist
    fun levelOf(speed: Double): Int =
        (1 + (ln(speed / BASE_SPEED) / ln(LEVEL_STEP) + 1e-6).toInt()).coerceIn(1, MAX_LEVEL)

    fun slideShare(level: Int): Double = if (level < 6) 0.0 else (0.05 * (level - 5)).coerceAtMost(0.35)

    fun gateShare(level: Int): Double = if (level < 10) 0.0 else 0.25
}

object Rows {
    // naechste Reihe hinter prev; der Abstand reicht immer, um von der freien Spur zur naechsten zu wechseln
    fun make(seed: Long, no: Int, prevZ: Double, prevSafe: Int, lanes: Int, level: Int, speed: Double): Row {
        val rng = Random(seed * 1_000_003L + no * 7_919L + 67)
        val shift = if (lanes > 3 && rng.nextDouble() < 0.25) 2 else rng.nextInt(0, 2)
        val dir = if (rng.nextBoolean()) 1 else -1
        var safe = prevSafe + dir * shift
        if (safe !in 0 until lanes) safe = prevSafe - dir * shift
        safe = safe.coerceIn(0, lanes - 1)
        val moved = abs(safe - prevSafe)
        val gap = speed * (SlalomRules.REACT_S + SlalomRules.SHIFT_S * moved + rng.nextDouble() * 0.55)
        val z = prevZ + gap
        // Zahl der gesperrten Spuren: mit der Stufe mehr, enge Tore ab Stufe 10
        val maxBlock = lanes - 1
        val count = when {
            rng.nextDouble() < SlalomRules.gateShare(level) -> maxBlock
            level >= 4 && rng.nextDouble() < 0.45 -> minOf(2, maxBlock)
            level >= 8 && rng.nextDouble() < 0.3 -> minOf(3, maxBlock)
            else -> 1
        }
        val lanesBlocked = (0 until lanes).filter { it != safe }.shuffled(rng).take(count)
        var obstacles = lanesBlocked.map { Obstacle(it, if (rng.nextDouble() < 0.6) Kind.PILLAR else Kind.BOULDER) }
        // rutschender Brocken: kommt aus einer Nachbarspur (auch der freien) und ist frueh genug am Ziel
        if (rng.nextDouble() < SlalomRules.slideShare(level)) {
            val i = obstacles.indices.random(rng)
            val o = obstacles[i]
            val from = listOf(o.lane - 1, o.lane + 1).filter { it in 0 until lanes && it !in lanesBlocked }.randomOrNull(rng)
            if (from != null) {
                val end = speed * (SlalomRules.REACT_S + SlalomRules.SHIFT_S + 0.2)
                obstacles = obstacles.toMutableList().also { it[i] = o.copy(kind = Kind.BOULDER, from = from, slideAt = end + speed * 0.45, slideLen = speed * 0.45) }
            }
        }
        return Row(no, z, obstacles, safe)
    }
}

data class SlalomState(
    override val round: Round,
    override val startLevel: Int,
    val seed: Long,
    val relaxed: Boolean,
    val lanes: Int,
    val lane: Int,
    val laneFrom: Int,
    val laneAt: Long,
    // zurueckgelegte Strecke (Weltmass) und Tempo je Sekunde
    val z: Double,
    val speed: Double,
    val cleanSince: Long,
    val lives: Int,
    val safeUntil: Long,
    val rows: List<Row>,
    val nextNo: Int,
    val passed: Int = 0,
    val nears: Int = 0,
    val crashes: Int = 0,
    val crashAt: Long = -1,
    val nearAt: Long = -1,
) : Play {
    private val scale: Double get() = if (relaxed) SlalomRules.RELAXED else 1.0

    // Stufe nach dem Tempo (Gemaechlich gleich bewertet), je Runde begrenzt
    override val level: Int
        get() = SlalomRules.levelOf(speed * scale)
            .coerceIn(startLevel - SlalomRules.MAX_DOWN, startLevel + SlalomRules.MAX_UP)
            .coerceIn(1, SlalomRules.MAX_LEVEL)

    override val cheers: Int get() = nears
    override val jolts: Int get() = crashes

    // Spur, die bei einer Durchfahrt um t zaehlt: waehrend des Wechsels erst ab der Haelfte die neue
    fun laneAtTime(t: Long): Int = if (t - laneAt < SlalomRules.LANE_MS / 2) laneFrom else lane
}

// Slalom: Gleiter in der Schlucht, Hindernissen per Spurwechsel ausweichen. Eingabe = -1 links, +1 rechts
object SlalomGame : MiniGame<SlalomState, Int> {
    override fun startLevel(saved: Int): Int = (saved.coerceIn(1, SlalomRules.MAX_LEVEL) - 2).coerceAtLeast(1)

    override fun start(seed: Long, level: Int, relaxed: Boolean): SlalomState {
        val l = level.coerceIn(1, SlalomRules.MAX_LEVEL)
        val lanes = SlalomRules.lanes(l)
        val v = SlalomRules.speed(l) / if (relaxed) SlalomRules.RELAXED else 1.0
        val mid = lanes / 2
        val first = Rows.make(seed, 0, v * SlalomRules.START_S - v * SlalomRules.REACT_S, mid, lanes, l, v)
        val s = SlalomState(Round(), l, seed, relaxed, lanes, mid, mid, -10_000, 0.0, v, 0, SlalomRules.LIVES, -1, listOf(first), 1)
        return fill(s)
    }

    // Reihen bis zur Sichtweite nachlegen, vorbeigezogene verwerfen
    private fun fill(s: SlalomState): SlalomState {
        val rows = s.rows.filter { it.z > s.z - 4 }.toMutableList()
        var no = s.nextNo
        var last = s.rows.last()
        while (last.z < s.z + SlalomRules.VIEW) {
            last = Rows.make(s.seed, no++, last.z, last.safe, s.lanes, s.level, s.speed)
            rows += last
        }
        return s.copy(rows = rows, nextNo = no)
    }

    override fun tick(s: SlalomState, t: Long): SlalomState {
        var x = s
        // Ereignisse streng nach Zeit: Durchfahrten, Tempostufen und neue Reihen (Ergebnis haengt nicht an der Bildrate)
        while (!x.round.isOver && x.round.now < t) {
            val now = x.round.now
            val row = x.rows.firstOrNull { it.z > x.z }
            val rowAt = if (row == null) Long.MAX_VALUE else now + ceil((row.z - x.z) / x.speed * 1000).toLong().coerceAtLeast(1)
            val upAt = x.cleanSince + SlalomRules.CLEAN_MS
            val fillAt = now + ceil((x.rows.last().z - SlalomRules.VIEW - x.z) / x.speed * 1000).toLong().coerceAtLeast(1)
            val next = minOf(t, rowAt, upAt, fillAt, x.round.durationMs)
            val dz = x.speed * (next - now) / 1000.0
            // nie ueber die naechste Reihe hinaus, ohne sie zu werten
            val z = if (next == rowAt && row != null) row.z + 1e-9 else minOf(x.z + dz, (row?.z ?: Double.MAX_VALUE) - 1e-9)
            x = x.copy(round = x.round.at(next), z = z)
            if (next == upAt && next < x.round.durationMs) x = x.copy(speed = x.speed * SlalomRules.SPEED_UP, cleanSince = upAt)
            if (next == rowAt && row != null) x = pass(x, row, next)
            x = fill(x)
        }
        return x
    }

    private fun pass(s: SlalomState, row: Row, t: Long): SlalomState {
        val lane = s.laneAtTime(t)
        if (lane in row.blocked && t >= s.safeUntil) {
            val lives = s.lives - 1
            var round = s.round.miss()
            if (lives <= 0) round = round.end()
            return s.copy(
                round = round, lives = lives, crashes = s.crashes + 1, crashAt = t,
                speed = s.speed * SlalomRules.SPEED_DOWN, cleanSince = t, safeUntil = t + SlalomRules.SAFE_MS,
            )
        }
        // Punkte nach Tempo; knapp: gesperrte Nachbarspur und eben erst gewechselt
        val points = (SlalomRules.ROW_POINTS * s.speed / SlalomRules.BASE_SPEED).roundToInt()
        var x = s.copy(round = s.round.hit(points), passed = s.passed + 1)
        val near = (lane - 1 in row.blocked || lane + 1 in row.blocked) && t - s.laneAt <= SlalomRules.NEAR_MS
        if (near) x = x.copy(round = x.round.bonus(SlalomRules.NEAR_BONUS), nears = x.nears + 1, nearAt = t)
        return x
    }

    override fun input(s: SlalomState, input: Int, t: Long): SlalomState {
        val x = tick(s, t)
        if (x.round.isOver || input == 0) return x
        val target = (x.lane + input.coerceIn(-1, 1)).coerceIn(0, x.lanes - 1)
        if (target == x.lane) return x
        // Wechsel im Wechsel: von der Spur aus, die gerade zaehlt
        return x.copy(laneFrom = x.laneAtTime(t), lane = target, laneAt = t)
    }
}
