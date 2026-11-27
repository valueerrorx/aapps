package solutions.xapient.thinkr.domain.glance

import solutions.xapient.thinkr.domain.Bands
import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

// Was in der Mitte aufblitzt: eine von zwei Formen
enum class Mark { ORB, DIAMOND }

sealed interface GlanceInput {
    data class Center(val mark: Mark) : GlanceInput
    data class Spot(val index: Int) : GlanceInput
}

// Stellen am Rand: je Ring 8, Ring 1 um eine halbe Teilung versetzt; Ring = index / 8
object Spots {
    const val PER_RING = 8

    // Radius als Anteil des halben kuerzeren Bildschirmmasses
    val RADII = listOf(0.42f, 0.66f, 0.9f)

    fun ring(index: Int): Int = index / PER_RING

    fun angle(index: Int): Double {
        val r = ring(index)
        val k = index % PER_RING
        return -PI / 2 + 2 * PI * k / PER_RING + (if (r == 1) PI / PER_RING else 0.0)
    }

    fun count(rings: Int): Int = rings * PER_RING
}

// Stufen und Zeiten an einer Stelle, damit das Tuning nach dem Geraetetest leicht faellt
object GlanceRules {
    const val MAX_LEVEL = 18
    const val POINTS = 60
    const val RING_BONUS = 15
    const val FIX_MIN_MS = 600L
    const val FIX_MAX_MS = 950L
    const val MASK_MS = 300L
    const val FEEDBACK_OK_MS = 750L
    const val FEEDBACK_BAD_MS = 1_150L
    const val HZ = 60.0
    const val FRAME_MS = 1000.0 / HZ

    // Gemaechlich: mindestens 150 ms
    const val RELAXED = 1.5
    const val RELAXED_MIN_FRAMES = 9

    // Anzeigezeit in Frames (60 Hz): 500 ms bis 50 ms
    val FRAMES = listOf(30, 26, 22, 19, 16, 14, 12, 10, 9, 8, 7, 6, 5, 4, 3)

    // Spielarten: ein Ring (1–6), zwei Ringe (7–12), drei Ringe mit Koedern (13–18); je Stufe Zeit x0,72
    val bands = Bands(listOf(1, 7, 13), listOf(500.0, 500.0, 500.0), MAX_LEVEL, 0.72)

    fun rings(modeStart: Int): Int = when {
        modeStart >= 13 -> 3
        modeStart >= 7 -> 2
        else -> 1
    }

    fun decoys(level: Int): Int = if (level < 13) 0 else (1 + (level - 13) / 2).coerceAtMost(3)

    fun ms(frames: Int): Double = frames * FRAME_MS

    // Sprosse mit der laengsten Zeit, die noch in die Stufe passt
    fun rungFor(modeStart: Int, level: Int): Int {
        val limit = bands.deadline(modeStart, level) + 1e-6
        return FRAMES.indices.firstOrNull { ms(FRAMES[it]) <= limit } ?: FRAMES.lastIndex
    }
}

// Eine Aufgabe; flashAt/maskAt = Frame-Zeitpunkte des Aufblitzens bzw. der Maske (-1 = noch nicht)
data class Trial(
    val no: Int,
    val mark: Mark,
    val spot: Int,
    val decoys: List<Int>,
    val fixMs: Long,
    val startAt: Long,
    val frames: Int,
    val flashAt: Long = -1,
    val maskAt: Long = -1,
    val saidMark: Mark? = null,
    val saidSpot: Int? = null,
    val doneAt: Long = -1,
    // Anzeigedauer wich zu sehr ab: nicht gewertet
    val void: Boolean = false,
) {
    val done: Boolean get() = doneAt >= 0
    val correct: Boolean get() = saidMark == mark && saidSpot == spot
    val shownMs: Long get() = if (flashAt >= 0 && maskAt >= 0) maskAt - flashAt else 0
}

enum class GlancePhase { FIX, FLASH, MASK, ANSWER, FEEDBACK }

data class GlanceState(
    override val round: Round,
    override val startLevel: Int,
    val seed: Long,
    val relaxed: Boolean,
    val modeStart: Int,
    // Sprosse auf der Frame-Leiter (0 = laengste Zeit)
    val rung: Int,
    val run: Int,
    val trial: Trial,
    // geschaetzte Frame-Dauer aus den letzten Ticks
    val frameMs: Double = GlanceRules.FRAME_MS,
    val lastTick: Long = -1,
    val shown: Int = 0,
    val voids: Int = 0,
) : Play {
    override val level: Int get() = GlanceRules.bands.levelOf(modeStart, GlanceRules.ms(GlanceRules.FRAMES[rung]))
    val rings: Int get() = GlanceRules.rings(modeStart)
    val nextAt: Long get() = trial.doneAt + if (trial.correct || trial.void) GlanceRules.FEEDBACK_OK_MS else GlanceRules.FEEDBACK_BAD_MS

    fun phase(t: Long = round.now): GlancePhase = when {
        trial.flashAt < 0 -> GlancePhase.FIX
        trial.maskAt < 0 -> GlancePhase.FLASH
        t < trial.maskAt + GlanceRules.MASK_MS -> GlancePhase.MASK
        !trial.done -> GlancePhase.ANSWER
        else -> GlancePhase.FEEDBACK
    }
}

// Rundblick: Mitte fixieren, kurz blitzen Mitte und Stern am Rand auf; Mitte waehlen und Stelle antippen
object GlanceGame : MiniGame<GlanceState, GlanceInput> {
    override fun startLevel(saved: Int): Int = GlanceRules.bands.warmUp(saved)

    override fun start(seed: Long, level: Int, relaxed: Boolean): GlanceState {
        val l = level.coerceIn(1, GlanceRules.MAX_LEVEL)
        val m = GlanceRules.bands.modeStart(l)
        val rung = GlanceRules.rungFor(m, l)
        val s = GlanceState(Round(), l, seed, relaxed, m, rung, 0, Trial(0, Mark.ORB, 0, emptyList(), 0, 0, 1))
        return s.copy(trial = newTrial(s, 0, 0))
    }

    // Frames, die tatsaechlich gezeigt werden sollen (Gemaechlich laenger, mindestens 150 ms)
    fun framesFor(s: GlanceState): Int {
        val f = GlanceRules.FRAMES[s.rung]
        return if (s.relaxed) maxOf(ceil(f * GlanceRules.RELAXED).toInt(), GlanceRules.RELAXED_MIN_FRAMES) else f
    }

    fun newTrial(s: GlanceState, no: Int, at: Long): Trial {
        val rng = Random(s.seed * 1_000_003L + no * 7_919L + 53)
        val count = Spots.count(s.rings)
        // Ziel gleich oft auf jedem Ring
        val ring = rng.nextInt(s.rings)
        val spot = ring * Spots.PER_RING + rng.nextInt(Spots.PER_RING)
        val decoys = (0 until count).filter { it != spot }.shuffled(rng).take(GlanceRules.decoys(s.level))
        val mark = if (rng.nextBoolean()) Mark.ORB else Mark.DIAMOND
        val fix = GlanceRules.FIX_MIN_MS + rng.nextLong(GlanceRules.FIX_MAX_MS - GlanceRules.FIX_MIN_MS + 1)
        return Trial(no, mark, spot, decoys.sorted(), fix, at, framesFor(s))
    }

    override fun tick(s: GlanceState, t: Long): GlanceState {
        var x = s.copy(round = s.round.at(t))
        // Frame-Dauer schaetzen (Ausreisser zaehlen nicht)
        val dt = if (x.lastTick >= 0) t - x.lastTick else 0
        if (dt in 4..50) x = x.copy(frameMs = x.frameMs * 0.9 + dt * 0.1)
        if (t > x.lastTick) x = x.copy(lastTick = t)
        val tr = x.trial
        val target = GlanceRules.ms(tr.frames)
        when {
            // Aufblitzen beginnt im ersten Frame nach dem Fixieren, nicht kurz vor Rundenende
            tr.flashAt < 0 -> if (t >= tr.startAt + tr.fixMs && t < x.round.durationMs - 1_000) {
                x = x.copy(trial = tr.copy(flashAt = t), shown = x.shown + 1)
            }
            // Frame ohne Zeitfortschritt mitten im Blitz = Pause und Weiter: der Blitz stand real zu lang
            tr.maskAt < 0 && dt == 0L && t > tr.flashAt && !tr.void -> x = x.copy(trial = tr.copy(void = true), voids = x.voids + 1)
            // Maske im ersten Frame, in dem die Zielzeit (bis auf einen halben Frame) erreicht ist
            tr.maskAt < 0 -> if (t - tr.flashAt >= target - x.frameMs / 2) {
                val shownMs = t - tr.flashAt
                val off = abs(shownMs - target) > maxOf(x.frameMs * 0.75, 6.0)
                x = x.copy(trial = tr.copy(maskAt = t, void = tr.void || off), voids = x.voids + if (off && !tr.void) 1 else 0)
            }
            tr.done && x.nextAt <= t && x.nextAt < x.round.durationMs ->
                x = x.copy(trial = newTrial(x, tr.no + 1, x.nextAt))
        }
        return x
    }

    override fun input(s: GlanceState, input: GlanceInput, t: Long): GlanceState {
        // Tipps waehrend des Blitzes zaehlen nicht und ruehren die Frame-Messung nicht an
        if (s.phase() == GlancePhase.FLASH) return s
        val x = tick(s, t)
        val tr = x.trial
        if (x.round.isOver || x.phase(t) != GlancePhase.ANSWER) return x
        val next = when (input) {
            is GlanceInput.Center -> if (tr.saidMark != null) return x else tr.copy(saidMark = input.mark)
            is GlanceInput.Spot -> if (tr.saidSpot != null || input.index !in 0 until Spots.count(x.rings)) return x else tr.copy(saidSpot = input.index)
        }
        if (next.saidMark == null || next.saidSpot == null) return x.copy(trial = next)
        return score(x, next.copy(doneAt = t))
    }

    // beides richtig zaehlt; nicht gewertete Aufgaben aendern nichts
    private fun score(x: GlanceState, tr: Trial): GlanceState {
        if (tr.void) return x.copy(trial = tr)
        return if (tr.correct) {
            val round = x.round.hit(GlanceRules.POINTS).bonus(GlanceRules.RING_BONUS * Spots.ring(tr.spot))
            val up = x.run + 1 >= 2
            x.copy(
                round = round, trial = tr, run = if (up) 0 else x.run + 1,
                // die Stufe deckelt Bands.levelOf auf eine Spielart hinauf
                rung = if (up) (x.rung + 1).coerceAtMost(GlanceRules.FRAMES.lastIndex) else x.rung,
            )
        } else {
            x.copy(round = x.round.miss(), trial = tr, run = 0, rung = (x.rung - 1).coerceAtLeast(0))
        }
    }
}
