package solutions.xapient.thinkr.domain.swarm

import solutions.xapient.thinkr.domain.Bands
import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import solutions.xapient.thinkr.domain.Staircase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Bildschirm-Richtungen (y waechst nach unten)
enum class Dir(val dx: Int, val dy: Int) {
    UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0)
}

enum class Formation { LINE, CROSS, VEE, DIAMOND }

// Lage im Schwarm in Abstands-Einheiten; Index 0 ist immer der Mittlere bei (0, 0)
data class Spot(val x: Float, val y: Float)

object Formations {
    val SIZES = listOf(5, 7, 9)

    fun spots(f: Formation, n: Int): List<Spot> {
        require(n in SIZES)
        val k = (n - 1) / 2
        val rest: List<Spot> = when (f) {
            Formation.LINE -> (1..k).flatMap { listOf(Spot(-it.toFloat(), 0f), Spot(it.toFloat(), 0f)) }
            Formation.CROSS -> (1..3).flatMap { listOf(Spot(it.toFloat(), 0f), Spot(-it.toFloat(), 0f), Spot(0f, -it.toFloat()), Spot(0f, it.toFloat())) }.take(n - 1)
            // Spitze vorn, die Arme schraeg nach hinten
            Formation.VEE -> (1..k).flatMap { listOf(Spot(-it.toFloat(), it * 0.8f), Spot(it.toFloat(), it * 0.8f)) }
            Formation.DIAMOND -> when (n) {
                5 -> listOf(Spot(-1f, -1f), Spot(1f, -1f), Spot(1f, 1f), Spot(-1f, 1f))
                7 -> List(6) { i -> Spot(1.25f * cos(PI / 3 * i).toFloat(), 1.25f * sin(PI / 3 * i).toFloat()) }
                else -> listOf(Spot(-1f, -1f), Spot(1f, -1f), Spot(1f, 1f), Spot(-1f, 1f), Spot(-2f, 0f), Spot(2f, 0f), Spot(0f, -2f), Spot(0f, 2f))
            }
        }
        return listOf(Spot(0f, 0f)) + rest
    }
}

// Ein Schwarm: point = Zeigerichtung des Mittleren, flank = die der anderen; drift = Flugrichtung (ab Stufe 10)
data class Flock(
    val point: Dir,
    val flank: Dir,
    val size: Int,
    val formation: Formation,
    // Lage des Mittleren, normiert 0..1
    val x: Float,
    val y: Float,
    val hollow: Boolean = false,
    val drift: Dir? = null,
) {
    val congruent: Boolean get() = point == flank

    // gefuellt: wohin er zeigt; hohl: wohin der Schwarm fliegt
    val answer: Dir get() = if (hollow) drift ?: point else point
}

// Stufen und Fristen an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object SwarmRules {
    const val MAX_LEVEL = 16
    const val HOLLOW = 10
    const val POINTS = 40
    const val GAP_MS = 220L
    const val RELAXED = 1.5
    const val DECK = 400
    const val MAX_RUN = 3
    const val HOLLOW_SHARE = 0.4
    const val DRIFT_APART = 0.7

    // Flugtempo in Abstands-Einheiten je Sekunde
    const val DRIFT_SPEED = 0.55

    // Spielarten: Zeigerichtung (1–9), dazu die Hohl-Regel (10–16)
    val bands = Bands(listOf(1, HOLLOW), listOf(2600.0, 3200.0), MAX_LEVEL, 0.9)

    fun size(level: Int): Int = when {
        level >= 8 -> 9
        level >= 4 -> 7
        else -> 5
    }

    // Anteil gemischter Schwaerme: 30 % bis 70 %
    fun incongruent(level: Int): Double = (0.3 + 0.05 * (level - 1)).coerceAtMost(0.7)

    // Unruhe: Zittern in Abstands-Einheiten
    fun jitter(level: Int): Double = (0.03 + 0.012 * (level - 1)).coerceAtMost(0.2)
}

object SwarmDeck {
    fun deal(rng: Random, count: Int, level: Int, hollowRule: Boolean): List<Flock> {
        val out = ArrayList<Flock>(count)
        val n = SwarmRules.size(level)
        val mixed = SwarmRules.incongruent(level)
        var run = 0
        var lastAnswer: Dir? = null
        repeat(count) {
            // Antwort gleichverteilt, hoechstens MAX_RUN gleiche in Folge
            var answer = Dir.entries.random(rng)
            if (answer == lastAnswer && run >= SwarmRules.MAX_RUN) answer = Dir.entries.filter { it != answer }.random(rng)
            val hollow = hollowRule && rng.nextDouble() < SwarmRules.HOLLOW_SHARE
            val other = { d: Dir -> Dir.entries.filter { it != d }.random(rng) }
            val point: Dir
            val drift: Dir?
            if (hollow) {
                drift = answer
                point = if (rng.nextDouble() < SwarmRules.DRIFT_APART) other(answer) else answer
            } else {
                point = answer
                drift = if (!hollowRule) null else if (rng.nextDouble() < SwarmRules.DRIFT_APART) other(answer) else answer
            }
            val flank = if (rng.nextDouble() < mixed) other(point) else point
            val prev = out.lastOrNull()
            // neue Stelle und meist neue Formation, damit der Blick suchen muss
            var x: Float
            var y: Float
            do {
                x = 0.3f + 0.4f * rng.nextFloat()
                y = 0.3f + 0.4f * rng.nextFloat()
            } while (prev != null && kotlin.math.hypot(x - prev.x, y - prev.y) < 0.12f)
            val formation = Formation.entries.filter { it != prev?.formation }.random(rng)
            out += Flock(point, flank, n, formation, x, y, hollow, drift)
            run = if (answer == lastAnswer) run + 1 else 1
            lastAnswer = answer
        }
        return out
    }
}

// Letzte Auswertung fuer die Optik; said = null: Frist abgelaufen, shownAt = Erscheinen dieses Schwarms
data class SwarmAnswer(val index: Int, val said: Dir?, val correct: Boolean, val at: Long, val shownAt: Long)

data class SwarmState(
    override val round: Round,
    override val startLevel: Int,
    val modeStart: Int,
    val flocks: List<Flock>,
    val index: Int,
    val shownAt: Long,
    val stair: Staircase,
    val relaxed: Boolean,
    val jitter: Double,
    val last: SwarmAnswer? = null,
) : Play {
    override val level: Int get() = SwarmRules.bands.levelOf(modeStart, stair.value)
    val flock: Flock get() = flocks[index]
    val deadlineMs: Long get() = (stair.value * if (relaxed) SwarmRules.RELAXED else 1.0).toLong()
    val dueAt: Long get() = shownAt + deadlineMs
    val hollowRule: Boolean get() = modeStart >= SwarmRules.HOLLOW
}

// Schwarm: wohin zeigt der Mittlere (hohl: wohin fliegt der Schwarm)? Eingabe = Wischrichtung
object SwarmGame : MiniGame<SwarmState, Dir> {
    override fun startLevel(saved: Int): Int = SwarmRules.bands.warmUp(saved)

    override fun start(seed: Long, level: Int, relaxed: Boolean): SwarmState {
        val l = level.coerceIn(1, SwarmRules.MAX_LEVEL)
        val m = SwarmRules.bands.modeStart(l)
        val flocks = SwarmDeck.deal(Random(seed), SwarmRules.DECK, l, m >= SwarmRules.HOLLOW)
        return SwarmState(Round(), l, m, flocks, 0, 0, SwarmRules.bands.stairFor(m, l), relaxed, SwarmRules.jitter(l))
    }

    override fun tick(s: SwarmState, t: Long): SwarmState {
        var x = s.copy(round = s.round.at(t))
        // auch mehrere Ablaeufe nach einem langen Frame nachholen, aber nichts nach dem Rundenende
        while (x.dueAt <= t && x.dueAt < x.round.durationMs && x.index < x.flocks.lastIndex) {
            val due = x.dueAt
            x = x.copy(
                round = x.round.miss(),
                stair = x.stair.miss(),
                last = SwarmAnswer(x.index, null, false, due, x.shownAt),
                index = x.index + 1,
                shownAt = due + SwarmRules.GAP_MS,
            )
        }
        if (x.index >= x.flocks.lastIndex) x = x.copy(round = x.round.at(x.round.durationMs))
        return x
    }

    override fun input(s: SwarmState, input: Dir, t: Long): SwarmState {
        val x = tick(s, t)
        // Wechsel und Rundenende nehmen keine Antwort an
        if (x.round.isOver || t < x.shownAt) return x
        val correct = input == x.flock.answer
        return x.copy(
            round = if (correct) x.round.hit(SwarmRules.POINTS) else x.round.miss(),
            stair = if (correct) x.stair.hit() else x.stair.miss(),
            last = SwarmAnswer(x.index, input, correct, t, x.shownAt),
            index = x.index + 1,
            shownAt = t + SwarmRules.GAP_MS,
        )
    }
}
