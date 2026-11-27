package solutions.xapient.thinkr.domain.stars

import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import kotlin.random.Random

// Stufen und Zeiten an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object StarsRules {
    const val MAX_LEVEL = 14
    const val LIVES = 2
    const val POINTS = 30
    const val PERFECT_BONUS = 10
    const val LEAD_MS = 500L
    const val REVEAL_OK_MS = 1_400L
    const val REVEAL_BAD_MS = 1_100L
    const val RELAXED = 1.5

    // Stufe 1 = 3 Sterne, je Stufe einer mehr
    fun stars(level: Int): Int = level + 2

    // Raster waechst mit der Sternzahl: 4×4 bis 7×7
    fun side(level: Int): Int = when (stars(level)) {
        in 0..4 -> 4
        in 5..7 -> 5
        in 8..11 -> 6
        else -> 7
    }

    // Anzeigezeit sinkt leicht mit der Stufe
    fun showMs(level: Int): Long = (1_200L - 20L * (level - 1)).coerceAtLeast(950L)

    // perfekt +1, ein Fehler gleich, zwei Fehler -1
    fun next(level: Int, errors: Int): Int = when (errors) {
        0 -> level + 1
        1 -> level
        else -> level - 1
    }.coerceIn(1, MAX_LEVEL)
}

data class Tap(val cell: Int, val at: Long, val hit: Boolean)

// Ein Durchgang: Sterne zeigen, ausblenden, aus dem Gedaechtnis antippen; endAt < 0 = laeuft
data class Pass(
    val no: Int,
    val level: Int,
    val side: Int,
    val stars: List<Int>,
    val startAt: Long,
    val taps: List<Tap> = emptyList(),
    val endAt: Long = -1,
) {
    val found: Int get() = taps.count { it.hit }
    val errors: Int get() = taps.count { !it.hit }
    val done: Boolean get() = endAt >= 0
    val perfect: Boolean get() = done && errors == 0 && found == stars.size
    fun tapped(cell: Int): Tap? = taps.firstOrNull { it.cell == cell }
}

enum class StarsPhase { LEAD, SHOW, RECALL, REVEAL }

data class StarsState(
    override val round: Round,
    override val startLevel: Int,
    val seed: Long,
    val relaxed: Boolean,
    // Stufe nach den abgeschlossenen Durchgaengen
    val stage: Int,
    val pass: Pass,
    val perfects: Int = 0,
) : Play {
    override val level: Int get() = stage
    override val cheers: Int get() = perfects
    private val scale: Double get() = if (relaxed) StarsRules.RELAXED else 1.0
    val showAt: Long get() = pass.startAt + StarsRules.LEAD_MS
    val recallAt: Long get() = showAt + (StarsRules.showMs(pass.level) * scale).toLong()
    val nextAt: Long get() = pass.endAt + if (pass.perfect) StarsRules.REVEAL_OK_MS else StarsRules.REVEAL_BAD_MS

    fun phase(t: Long = round.now): StarsPhase = when {
        pass.done -> StarsPhase.REVEAL
        t < showAt -> StarsPhase.LEAD
        t < recallAt -> StarsPhase.SHOW
        else -> StarsPhase.RECALL
    }
}

// Sternbild: Sterne leuchten kurz auf und werden aus dem Gedaechtnis angetippt; Eingabe = Zelle
object StarsGame : MiniGame<StarsState, Int> {
    override fun startLevel(saved: Int): Int = (saved.coerceIn(1, StarsRules.MAX_LEVEL) - 2).coerceAtLeast(1)

    override fun start(seed: Long, level: Int, relaxed: Boolean): StarsState {
        val l = level.coerceIn(1, StarsRules.MAX_LEVEL)
        return StarsState(Round(), l, seed, relaxed, l, newPass(seed, 0, l, 0))
    }

    fun newPass(seed: Long, no: Int, level: Int, at: Long): Pass {
        val side = StarsRules.side(level)
        val rng = Random(seed * 1_000_003L + no * 7_919L + 13)
        return Pass(no, level, side, Sky.pattern(rng, side, StarsRules.stars(level)), at)
    }

    override fun tick(s: StarsState, t: Long): StarsState {
        var x = s.copy(round = s.round.at(t))
        // nach der Aufloesung folgt der naechste Durchgang, auch mehrere nach einem langen Frame
        while (x.pass.done && x.nextAt <= t && x.nextAt < x.round.durationMs) {
            x = x.copy(pass = newPass(x.seed, x.pass.no + 1, x.stage, x.nextAt))
        }
        return x
    }

    override fun input(s: StarsState, input: Int, t: Long): StarsState {
        val x = tick(s, t)
        val p = x.pass
        // nur im Abfragen; schon getippte Zellen zaehlen nicht doppelt
        if (x.round.isOver || x.phase(t) != StarsPhase.RECALL || input !in 0 until p.side * p.side || p.tapped(input) != null) return x
        val hit = input in p.stars
        val taps = p.taps + Tap(input, t, hit)
        var round = if (hit) x.round.hit(StarsRules.POINTS) else x.round.miss()
        var pass = p.copy(taps = taps)
        var stage = x.stage
        var perfects = x.perfects
        if (pass.found == p.stars.size || pass.errors >= StarsRules.LIVES) {
            pass = pass.copy(endAt = t)
            stage = StarsRules.next(p.level, pass.errors)
            if (pass.perfect) {
                round = round.bonus(StarsRules.PERFECT_BONUS * p.stars.size)
                perfects++
            }
        }
        return x.copy(round = round, pass = pass, stage = stage, perfects = perfects)
    }
}
