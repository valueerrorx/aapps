package solutions.xapient.thinkr.domain

import kotlin.math.max

// Punktekette: nach je STEP Richtigen in Folge +1 bis MAX, ein Fehler setzt zurueck
data class Combo(val mult: Int = 1, val run: Int = 0) {
    fun hit(): Combo = when {
        mult >= MAX -> Combo(MAX, 0)
        run + 1 >= STEP -> Combo(mult + 1, 0)
        else -> Combo(mult, run + 1)
    }

    fun miss(): Combo = Combo()

    companion object {
        const val STEP = 4
        const val MAX = 5
    }
}

// Treppe auf einen Wert (z. B. Frist): nach hitsToStep Richtigen in Folge mal hitFactor, nach jedem Fehler mal missFactor
data class Staircase(
    val value: Double,
    val min: Double,
    val max: Double,
    val hitsToStep: Int = 3,
    val hitFactor: Double = 0.92,
    val missFactor: Double = 1.12,
    val run: Int = 0,
) {
    fun hit(): Staircase =
        if (run + 1 >= hitsToStep) copy(value = (value * hitFactor).coerceIn(min, max), run = 0) else copy(run = run + 1)

    fun miss(): Staircase = copy(value = (value * missFactor).coerceIn(min, max), run = 0)
}

// Gemeinsamer Rundenstand: Spielzeit in ms (steht in der Pause), Punkte, Kette, Treffer
data class Round(
    val durationMs: Long = DURATION_MS,
    val now: Long = 0,
    val score: Int = 0,
    val combo: Combo = Combo(),
    val correct: Int = 0,
    val wrong: Int = 0,
) {
    val isOver: Boolean get() = now >= durationMs
    val remainingMs: Long get() = (durationMs - now).coerceAtLeast(0)
    val answered: Int get() = correct + wrong

    // Trefferquote in ganzen Prozent, abgerundet: ein Fehler ergibt nie 100 %
    val accuracy: Int get() = if (answered == 0) 0 else correct * 100 / answered

    // Zeit laeuft nur vorwaerts und nie ueber das Rundenende
    fun at(t: Long): Round = copy(now = max(now, t).coerceAtMost(durationMs))

    // Punkte mit dem Multiplikator vor diesem Treffer
    fun hit(points: Int): Round = copy(score = score + points * combo.mult, combo = combo.hit(), correct = correct + 1)

    fun miss(): Round = copy(combo = combo.miss(), wrong = wrong + 1)

    companion object {
        const val DURATION_MS = 60_000L
    }
}

// Was der Rahmen von jedem Spielstand braucht
interface Play {
    val round: Round
    val startLevel: Int

    // Stufe nach dem aktuellen Stand (am Rundenende = neue Stufe)
    val level: Int
}

// Ein Spiel: rein, deterministisch per Seed, Zeit nur ueber t (Spielzeit in ms)
interface MiniGame<S : Play, I> {
    fun startLevel(saved: Int): Int = (saved - 2).coerceAtLeast(1)

    fun start(seed: Long, level: Int, relaxed: Boolean): S

    fun tick(s: S, t: Long): S

    fun input(s: S, input: I, t: Long): S
}
