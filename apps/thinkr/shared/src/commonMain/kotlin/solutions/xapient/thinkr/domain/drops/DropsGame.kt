package solutions.xapient.thinkr.domain.drops

import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

enum class Op(val sign: String) { PLUS("+"), MINUS("−"), TIMES("×"), DIVIDE("÷") }

// Rechnung im Tropfen; nur ganzzahlige, nicht negative Ergebnisse
data class Sum(val a: Int, val op: Op, val b: Int) {
    val answer: Int
        get() = when (op) {
            Op.PLUS -> a + b
            Op.MINUS -> a - b
            Op.TIMES -> a * b
            Op.DIVIDE -> a / b
        }

    // Grundpunkte nach Schwierigkeit
    val points: Int
        get() = when {
            op == Op.TIMES && a >= 10 -> 30
            op == Op.TIMES || op == Op.DIVIDE -> 20
            answer > 20 || a > 20 -> 15
            else -> 10
        }

    override fun toString(): String = "$a ${op.sign} $b"
}

// Was eine Stufe stellt: Rechenarten, Zahlenraum, zweistellig mal einstellig
data class Mix(val ops: List<Op>, val addMax: Int, val mulMax: Int, val wide: Int = 0)

// Stufen, Zeiten und Punkte an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object DropsRules {
    const val MAX_LEVEL = 14
    const val LIVES = 3
    const val MAX_DIGITS = 4
    const val STEP_AFTER = 4
    const val CHAIN_BONUS = 10
    const val GLOW_FROM = 3
    const val GLOW_SHARE = 0.06
    const val FIRST_MS = 500L
    const val RETRY_MS = 250L
    const val RELAXED = 1.5

    // Sicherheitsnetz: die Runde endet sonst ueber das volle Becken
    const val MAX_MS = 15 * 60_000L

    // auf der hoechsten Stufe faellt es nach jedem Schritt etwas schneller
    const val BOOST = 0.96
    const val MIN_BOOST = 0.5

    fun mix(level: Int): Mix = when (level.coerceIn(1, MAX_LEVEL)) {
        1 -> Mix(listOf(Op.PLUS), 10, 0)
        2 -> Mix(listOf(Op.PLUS), 20, 0)
        3 -> Mix(listOf(Op.PLUS, Op.MINUS), 20, 0)
        4 -> Mix(listOf(Op.PLUS, Op.MINUS), 50, 0)
        5 -> Mix(listOf(Op.PLUS, Op.MINUS, Op.TIMES), 50, 5)
        6 -> Mix(listOf(Op.PLUS, Op.MINUS, Op.TIMES), 50, 10)
        7 -> Mix(listOf(Op.PLUS, Op.MINUS, Op.TIMES, Op.DIVIDE), 50, 10)
        8, 9 -> Mix(listOf(Op.PLUS, Op.MINUS, Op.TIMES, Op.DIVIDE), 100, 10)
        10, 11 -> Mix(listOf(Op.PLUS, Op.MINUS, Op.TIMES, Op.DIVIDE), 100, 10, wide = 19)
        else -> Mix(listOf(Op.PLUS, Op.MINUS, Op.TIMES, Op.DIVIDE), 200, 12, wide = 25)
    }

    // Fallzeit vom Himmel bis ins Wasser: 9 s auf Stufe 1, je Stufe x0,94
    fun fallMs(level: Int): Long = (9_000.0 * 0.94.pow(level.coerceIn(1, MAX_LEVEL) - 1)).toLong()

    fun maxDrops(level: Int): Int = when {
        level >= 12 -> 5
        level >= 8 -> 4
        level >= 4 -> 3
        else -> 2
    }

    // Abstand zwischen zwei Tropfen, damit meist maxDrops gleichzeitig fallen
    fun spawnMs(level: Int): Long = fallMs(level) * 9 / (10 * maxDrops(level))
}

object Sums {
    fun make(rng: Random, level: Int): Sum {
        val m = DropsRules.mix(level)
        val op = m.ops.random(rng)
        return when (op) {
            Op.PLUS -> {
                val total = rng.nextInt(2, m.addMax + 1)
                val a = rng.nextInt(1, total)
                Sum(a, op, total - a)
            }
            Op.MINUS -> {
                val a = rng.nextInt(2, m.addMax + 1)
                Sum(a, op, rng.nextInt(1, a + 1))
            }
            Op.TIMES -> if (m.wide > 0 && rng.nextDouble() < 0.4) {
                Sum(rng.nextInt(11, m.wide + 1), op, rng.nextInt(2, 10))
            } else {
                // kein Mal-Eins mit 1: zu leicht
                Sum(rng.nextInt(2, m.mulMax + 1), op, rng.nextInt(2, m.mulMax + 1))
            }
            Op.DIVIDE -> {
                val b = rng.nextInt(2, m.mulMax + 1)
                val q = rng.nextInt(1, m.mulMax + 1)
                Sum(b * q, op, b)
            }
        }
    }
}

// Ein Tropfen; x = Lage 0..1, faellt von spawnAt bis spawnAt + fallMs ins Wasser
data class Drop(val id: Int, val sum: Sum, val x: Float, val spawnAt: Long, val fallMs: Long, val glow: Boolean = false) {
    val landAt: Long get() = spawnAt + fallMs
    fun progress(t: Long): Float = ((t - spawnAt).toFloat() / fallMs).coerceIn(0f, 1f)
}

// Fuer die Optik: geplatzt (good) oder aufgeschlagen, mit Lage im Moment
data class Burst(val drop: Drop, val at: Long, val good: Boolean, val y: Float, val chain: Int = 1)

object DropsKeys {
    const val DEL = -1
    const val OK = -2
}

data class DropsState(
    override val round: Round,
    override val startLevel: Int,
    val seed: Long,
    val relaxed: Boolean,
    val stage: Int,
    val run: Int,
    val boost: Double,
    val drops: List<Drop>,
    val nextId: Int,
    val nextSpawnAt: Long,
    val entry: String = "",
    val splashes: Int = 0,
    val bursts: List<Burst> = emptyList(),
    val wrongAt: Long = -1,
    val splashAt: Long = -1,
) : Play {
    override val level: Int get() = stage
    val full: Boolean get() = splashes >= DropsRules.LIVES
    private val scale: Double get() = if (relaxed) DropsRules.RELAXED else 1.0
    val fallMs: Long get() = (DropsRules.fallMs(stage) * boost * scale).toLong()
    val spawnMs: Long get() = (DropsRules.spawnMs(stage) * boost * scale).toLong()
}

// Tropfen: Rechnungen fallen ins Wasser, Ergebnis eintippen; drei Aufschlaege fuellen das Becken. Eingabe = Taste
object DropsGame : MiniGame<DropsState, Int> {
    private const val KEEP_BURST_MS = 900L

    override fun startLevel(saved: Int): Int = (saved.coerceIn(1, DropsRules.MAX_LEVEL) - 2).coerceAtLeast(1)

    override fun start(seed: Long, level: Int, relaxed: Boolean): DropsState {
        val l = level.coerceIn(1, DropsRules.MAX_LEVEL)
        return DropsState(Round(durationMs = DropsRules.MAX_MS), l, seed, relaxed, l, 0, 1.0, emptyList(), 0, DropsRules.FIRST_MS)
    }

    fun spawn(s: DropsState, at: Long): DropsState {
        val rng = Random(s.seed * 1_000_003L + s.nextId * 7_919L + 41)
        val sum = Sums.make(rng, s.stage)
        // nicht direkt neben einem Tropfen, der noch oben haengt
        var x = 0f
        for (i in 0 until 12) {
            x = 0.1f + 0.8f * rng.nextFloat()
            if (s.drops.none { it.progress(at) < 0.4f && abs(it.x - x) < 0.24f }) break
        }
        val glow = s.stage >= DropsRules.GLOW_FROM && rng.nextDouble() < DropsRules.GLOW_SHARE
        val d = Drop(s.nextId, sum, x, at, s.fallMs, glow)
        return s.copy(drops = s.drops + d, nextId = s.nextId + 1, nextSpawnAt = at + s.spawnMs)
    }

    override fun tick(s: DropsState, t: Long): DropsState {
        var x = s
        // Ereignisse streng nach Zeit: Ergebnis haengt nicht an der Bildrate
        while (!x.round.isOver) {
            val land = x.drops.minByOrNull { it.landAt }
            val landAt = land?.landAt ?: Long.MAX_VALUE
            val next = minOf(landAt, x.nextSpawnAt)
            if (next > t) break
            x = x.copy(round = x.round.at(next))
            if (landAt <= x.nextSpawnAt && land != null) {
                x = landed(x, land, next)
            } else if (x.drops.size < DropsRules.maxDrops(x.stage)) {
                x = spawn(x, next)
            } else {
                x = x.copy(nextSpawnAt = next + DropsRules.RETRY_MS)
            }
        }
        if (!x.round.isOver) x = x.copy(round = x.round.at(t))
        return x.copy(bursts = x.bursts.filter { x.round.now - it.at < KEEP_BURST_MS })
    }

    private fun landed(s: DropsState, d: Drop, at: Long): DropsState {
        val splashes = s.splashes + 1
        var round = s.round.miss()
        if (splashes >= DropsRules.LIVES) round = round.end()
        return s.copy(
            round = round,
            drops = s.drops - d,
            splashes = splashes,
            splashAt = at,
            stage = (s.stage - 1).coerceAtLeast(1),
            run = 0,
            bursts = s.bursts + Burst(d, at, good = false, y = 1f),
        )
    }

    override fun input(s: DropsState, input: Int, t: Long): DropsState {
        val x = tick(s, t)
        if (x.round.isOver) return x
        return when (input) {
            DropsKeys.DEL -> x.copy(entry = x.entry.dropLast(1))
            DropsKeys.OK -> if (x.entry.isEmpty()) x else submit(x, t, final = true)
            in 0..9 -> if (x.entry.length >= DropsRules.MAX_DIGITS) x else submit(x.copy(entry = x.entry + input), t, final = false)
            else -> x
        }
    }

    // final = Enter; sonst nur, wenn die Eingabe eindeutig ist (kein laengeres Ergebnis faengt so an)
    private fun submit(s: DropsState, t: Long, final: Boolean): DropsState {
        val value = s.entry.toIntOrNull() ?: return s
        val match = s.drops.filter { it.sum.answer == value && s.entry == value.toString() }
        if (match.isEmpty()) {
            return if (final) s.copy(round = s.round.miss(), entry = "", wrongAt = t) else s
        }
        val longer = s.drops.any { a -> a.sum.answer.toString().let { it.length > s.entry.length && it.startsWith(s.entry) } }
        if (!final && longer) return s
        return solve(s, match, t)
    }

    private fun solve(s: DropsState, match: List<Drop>, t: Long): DropsState {
        val glow = match.any { it.glow }
        val popped = (if (glow) s.drops else match).sortedBy { it.spawnAt }
        var round = s.round
        popped.forEachIndexed { i, d ->
            round = round.hit(d.sum.points)
            if (i > 0) round = round.bonus(DropsRules.CHAIN_BONUS)
        }
        var stage = s.stage
        var run = s.run + popped.size
        var boost = s.boost
        while (run >= DropsRules.STEP_AFTER) {
            run -= DropsRules.STEP_AFTER
            if (stage < DropsRules.MAX_LEVEL) stage++ else boost = (boost * DropsRules.BOOST).coerceAtLeast(DropsRules.MIN_BOOST)
        }
        val bursts = popped.map { Burst(it, t, good = true, y = it.progress(t), chain = popped.size) }
        return s.copy(round = round, drops = s.drops - popped.toSet(), entry = "", stage = stage, run = run, boost = boost, bursts = s.bursts + bursts)
    }
}
