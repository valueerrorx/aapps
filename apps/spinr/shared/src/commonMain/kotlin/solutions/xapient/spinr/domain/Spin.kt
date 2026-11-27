package solutions.xapient.spinr.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

// Eine Drehung: Start, Richtung (+1 = im Uhrzeigersinn), Anfangsschwung; Endstellung steht von Anfang an fest
class Spin(val start: Double, val direction: Int, val omega0: Double, val friction: Friction = Friction()) {
    val duration: Double = friction.stopTime(omega0)
    val distance: Double = friction.distance(omega0)
    val end: Double = start + direction * distance

    fun angleAt(t: Double): Double = if (t >= duration) end else start + direction * friction.travelled(omega0, t)

    fun velocityAt(t: Double): Double = direction * friction.velocity(omega0, t)

    fun isDone(t: Double): Boolean = t >= duration
}

// Plant Drehungen so, dass die Endstellung gleichverteilt ist
object Spinner {
    const val TAP_TURNS = 4
    const val MIN_FLING = 500.0 // Grad/s, darunter nur Drehen von Hand
    const val MIN_DISTANCE = 2.5 * 360.0
    const val MAX_DISTANCE = 10.0 * 360.0
    private const val KNEE = 6.0 * 360.0 // ab hier wird der Weg sanft gebremst

    // Tippen: 4-5 volle Umdrehungen plus gleichverteilter Rest
    fun tap(start: Double, random: Random, friction: Friction = Friction()): Spin {
        val distance = 360.0 * (TAP_TURNS + random.nextInt(2)) + random.nextDouble() * 360.0
        return Spin(start, 1, friction.velocityFor(distance), friction)
    }

    // Schwung: natuerlicher Weg, auf +-180 Grad so verschoben, dass der Rest gleichverteilt ist
    fun fling(start: Double, velocity: Double, random: Random, friction: Friction = Friction()): Spin? {
        if (abs(velocity) < MIN_FLING || velocity.isNaN()) return null
        val base = naturalDistance(abs(velocity), friction) - 180.0
        val distance = base + (random.nextDouble() * 360.0 - base).mod(360.0)
        return Spin(start, if (velocity > 0) 1 else -1, friction.velocityFor(distance), friction)
    }

    // Mitte des Wegs: +-180 Grad bleiben innerhalb 2,5-10 Umdrehungen, oben weich statt hart begrenzt
    fun naturalDistance(speed: Double, friction: Friction = Friction()): Double {
        val lo = MIN_DISTANCE + 180.0
        val hi = MAX_DISTANCE - 180.0
        val d = friction.distance(speed)
        if (d <= KNEE) return d.coerceAtLeast(lo)
        return KNEE + (hi - KNEE) * (1 - exp(-(d - KNEE) / (hi - KNEE)))
    }
}
