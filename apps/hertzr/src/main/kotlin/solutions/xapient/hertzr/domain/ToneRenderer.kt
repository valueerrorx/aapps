package solutions.xapient.hertzr.domain

import kotlin.math.exp
import kotlin.math.ln

// Was der Audio-Thread spielen soll; als Ganzes uebergeben (atomar)
data class Params(
    val hz: Double,
    val wave: Wave,
    val amp: Float,
    val output: Output,
    // null = Dauerton
    val sweep: SweepSpec? = null,
)

fun Settings.params(): Params = Params(
    hz = hz,
    wave = wave,
    amp = Level.amp(levelDb),
    output = output,
    sweep = if (mode == Mode.SWEEP) SweepSpec(sweepFrom, sweepTo, sweepSec) else null,
)

// Erzeugt Stereo-Bloecke (interleaved L,R) ohne Allokation. Phase laeuft durch (Double), Frequenz gleitet logarithmisch,
// Pegel und Kanaele gleiten, Wellenwechsel blendet ueber, Start/Stopp blenden linear. Der Sweep wird beim Start festgelegt.
class ToneRenderer(private val rate: Int, params: Params) {
    private var p = params
    private var phase = 0.0
    private var logHz = ln(params.sweep?.from ?: params.hz)
    private var logTarget = logHz
    private var hz = exp(logHz)
    private var wave = params.wave
    // Anteil je Wellenform; Wechsel blendet vom aktuellen Gemisch linear zur neuen Form (auch mitten in einer Blende)
    private val weight = DoubleArray(Wave.entries.size).also { it[params.wave.ordinal] = 1.0 }
    private val from = DoubleArray(Wave.entries.size)
    // 1 = Ueberblendung fertig
    private var mix = 1.0
    private var gainL = 0f
    private var gainR = 0f
    private var env = 0f
    private var envStep = 1f / (rate * FADE_MS / 1_000)
    private var sweepFrame = 0L

    private val glide = 1 - exp(-1.0 / (rate * GLIDE_MS / 1_000.0))
    private val smooth = (1 - exp(-1.0 / (rate * LEVEL_MS / 1_000.0))).toFloat()
    private val xfadeStep = 1.0 / (rate * XFADE_MS / 1_000.0)
    private val sweepFrames get() = p.sweep?.let { it.seconds.toLong() * rate } ?: 0L

    // aktuelle Frequenz am Ende des letzten Blocks (Anzeige)
    val currentHz: Double get() = hz

    // Sweep-Fortschritt 0..1, null beim Dauerton
    val progress: Float? get() = p.sweep?.let { (sweepFrame.toDouble() / sweepFrames).toFloat().coerceIn(0f, 1f) }

    // Ausgeblendet und fertig: der Track kann stoppen
    var done = false
        private set

    val releasing: Boolean get() = envStep < 0

    fun apply(next: Params) {
        if (next.wave != wave) {
            weight.copyInto(from)
            wave = next.wave
            mix = 0.0
        }
        logTarget = ln(next.hz)
        // laufender Sweep bleibt, wie er gestartet wurde
        p = next.copy(sweep = p.sweep)
    }

    // Ausblenden beginnen; render liefert danach Stille und setzt done
    fun release() {
        if (envStep > 0) envStep = -1f / (rate * FADE_MS / 1_000)
    }

    fun render(out: FloatArray, frames: Int) {
        val sweep = p.sweep
        val total = sweepFrames
        val tl = p.amp * p.output.left
        val tr = p.amp * p.output.right
        for (i in 0 until frames) {
            if (sweep != null) {
                hz = sweep.hzAt(sweepFrame.toDouble() / total)
                if (sweepFrame < total) sweepFrame++ else release()
            } else if (logHz != logTarget) {
                val d = logTarget - logHz
                logHz = if (kotlin.math.abs(d) < 1e-7) logTarget else logHz + d * glide
                hz = exp(logHz)
            }
            val dt = hz / rate
            var s: Double
            if (mix < 1.0) {
                mix = (mix + xfadeStep).coerceAtMost(1.0)
                s = 0.0
                for (k in weight.indices) {
                    weight[k] = from[k] * (1 - mix) + (if (k == wave.ordinal) mix else 0.0)
                    if (weight[k] > 0.0) s += weight[k] * Oscillator.sample(Wave.entries[k], phase, dt)
                }
            } else {
                s = Oscillator.sample(wave, phase, dt)
            }
            phase += dt
            if (phase >= 1.0) phase -= 1.0
            gainL += (tl - gainL) * smooth
            gainR += (tr - gainR) * smooth
            env = (env + envStep).coerceIn(0f, 1f)
            val v = s.toFloat() * env
            out[2 * i] = v * gainL
            out[2 * i + 1] = v * gainR
        }
        if (envStep < 0 && env <= 0f) done = true
    }

    companion object {
        const val FADE_MS = 40
        const val GLIDE_MS = 8
        const val LEVEL_MS = 10
        const val XFADE_MS = 20
    }
}
