package solutions.xapient.magnr.domain

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// Ausschlag -> Piepton: hoeher und dichter bei Annaeherung, unter "schwach" still
object ToneMap {
    const val LOW_HZ = 520f
    const val MAX_INTERVAL_MS = 600f
    const val MIN_INTERVAL_MS = 60f
    const val CONTINUOUS = 0.95f
    private const val BEEP_MS = 45f

    private fun t(strength: Float) = ((strength - Level.WEAK.from) / (1f - Level.WEAK.from)).coerceIn(0f, 1f)

    fun silent(strength: Float) = strength < Level.WEAK.from

    // zwei Oktaven: 520 -> 2080 Hz
    fun pitchHz(strength: Float): Float = LOW_HZ * 2f.pow(2f * t(strength))

    // Abstand zwischen Pieptoenen, exponentiell; 0 = Dauerton, unendlich = still
    fun intervalMs(strength: Float): Float = when {
        silent(strength) -> Float.POSITIVE_INFINITY
        strength >= CONTINUOUS -> 0f
        else -> MAX_INTERVAL_MS * (MIN_INTERVAL_MS / MAX_INTERVAL_MS).pow(t(strength))
    }

    fun beepMs(intervalMs: Float): Float = min(BEEP_MS, intervalMs * 0.5f)
}

// Erzeugt die Pieptoene als 16-bit-PCM; phasenstetig, 3 ms Rampen gegen Knacksen
class BeepSynth(private val sampleRate: Int, private val amplitude: Float = 0.3f) {
    private var phase = 0.0
    private var env = 0f
    private var sinceStart = Long.MAX_VALUE / 2
    private val rampStep = 1f / (sampleRate * RAMP_S)

    fun render(out: ShortArray, count: Int = out.size, strength: Float) {
        val interval = ToneMap.intervalMs(strength)
        val freq = ToneMap.pitchHz(strength)
        val intervalSamples = if (interval.isInfinite()) Long.MAX_VALUE else (interval * sampleRate / 1000f).toLong()
        val beepSamples = (ToneMap.beepMs(interval) * sampleRate / 1000f).toLong()
        val inc = 2.0 * PI * freq / sampleRate
        for (i in 0 until count) {
            val gate = when {
                interval == 0f -> true
                interval.isInfinite() -> false
                else -> {
                    if (sinceStart >= intervalSamples) sinceStart = 0
                    sinceStart < beepSamples
                }
            }
            sinceStart++
            env = if (gate) min(1f, env + rampStep) else maxOf(0f, env - rampStep)
            if (env == 0f) {
                phase = 0.0
                out[i] = 0
            } else {
                out[i] = (sin(phase) * env * amplitude * Short.MAX_VALUE).toInt().toShort()
                phase += inc
                if (phase > 2 * PI) phase -= 2 * PI
            }
        }
    }

    private companion object {
        const val RAMP_S = 0.003f
    }
}
