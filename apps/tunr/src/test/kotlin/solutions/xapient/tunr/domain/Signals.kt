package solutions.xapient.tunr.domain

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Synthetische Testsignale
object Signals {
    fun harmonics(hz: Double, rate: Int, n: Int, amps: List<Double>, gain: Double = 0.5, phase: Double = 0.3): FloatArray =
        FloatArray(n) { j ->
            var v = 0.0
            amps.forEachIndexed { i, a ->
                val k = i + 1
                if (k * hz < rate / 2.0) v += a * sin(2 * PI * k * hz * j / rate + phase * k)
            }
            (gain * v).toFloat()
        }

    fun sine(hz: Double, rate: Int, n: Int, gain: Double = 0.5) = harmonics(hz, rate, n, listOf(1.0), gain)

    // Saegezahn bandbegrenzt: Obertoene mit 1/k
    fun saw(hz: Double, rate: Int, n: Int) = harmonics(hz, rate, n, List(40) { 1.0 / (it + 1) }, 0.3)

    fun noise(n: Int, amp: Double, seed: Int = 7): FloatArray {
        val r = Random(seed)
        return FloatArray(n) { ((r.nextDouble() * 2 - 1) * amp).toFloat() }
    }

    fun add(a: FloatArray, b: FloatArray) = FloatArray(a.size) { a[it] + b[it] }
}
