package solutions.xapient.sleepr

import solutions.xapient.sleepr.domain.Biquad
import solutions.xapient.sleepr.domain.FS
import solutions.xapient.sleepr.domain.FSpec
import solutions.xapient.sleepr.domain.FType
import solutions.xapient.sleepr.domain.NoiseChannel
import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/** Zufaellige Meereswellen: langsames Anschwellen, Brechen, Auslaufen; Faktor auf den Reglerwert (wie Android) */
internal class Wave(private val rnd: Random, period: Double) {
    private var t = 0.0; private var d = 1.0; private var peak = 0.8; private var rise = 0.5

    init { next(period); t = rnd.nextDouble() * d } // zufaelliger Startpunkt, Farben laufen nicht synchron

    private fun next(period: Double) {
        d = period * (0.65 + 0.7 * rnd.nextDouble())  // Laenge ±35 %
        peak = 0.55 + 0.45 * rnd.nextDouble()          // Hoehe der Welle
        rise = 0.35 + 0.25 * rnd.nextDouble()          // Anteil Anschwellen
        t = 0.0
    }

    fun step(dt: Double, period: Double): Double {
        t += dt
        if (t >= d) next(period)
        val u = t / d
        val env = if (u < rise) sin(PI / 2 * u / rise).pow(2) else ((1 - u) / (1 - rise)).pow(1.6)
        return 0.4 + 0.9 * peak * env // Tal 40 %, Kamm bis 130 % des Reglerwerts
    }
}

/**
 * Rauschen blockweise berechnen, Ablauf wie NoiseEngine auf Android: Pegel 60 ms geglaettet, Einblenden 1,5 s,
 * Soft-Clip ab 0,7, Timer mit sanftem Ausblenden, Stopp in 350 ms. Plattformfrei, die Ausgabe macht der Player.
 */
class NoiseRenderer(private val mix: Mix, seed: Long, random: Random = Random(seed)) {
    @Volatile var live = DoubleArray(5); private set // aktuelle Pegel inkl. Wellen, fuer die Oberflaeche
    @Volatile private var stopReq = false
    @Volatile private var timerReset = true
    @Volatile var frames = 0L; private set
    @Volatile private var endFrame = 0L      // 0 = kein Timer
    @Volatile private var fadeStart = 0L

    private val k = NoiseChannel.K
    private val gen = arrayOf(NoiseChannel(seed), NoiseChannel(seed * 31 + 7))
    private val tone = arrayOf(Biquad(), Biquad())
    private val v = DoubleArray(5)
    private val g = DoubleArray(5)
    private val tg = DoubleArray(5)
    private var m = 0.0
    private val smooth = 1.0 - exp(-1.0 / (0.06 * FS))
    private var lastTone = -1.0
    private val fadeIn = 1.5 * FS
    private val fadeOutLen = (0.35 * FS).toLong()
    private var fadeOutFrom = -1L
    private val waves = Array(5) { Wave(random, mix.waveSec[it].toDouble()) }
    private val wmix = DoubleArray(5) { if (mix.waves[it]) 1.0 else 0.0 }

    fun stop() { stopReq = true }
    fun restartTimer() { timerReset = true }

    /** Restzeit in ms, -1 ohne Timer */
    fun remainingMs(): Long = if (endFrame == 0L) -1 else max(0L, (endFrame - frames) * 1000 / FS)
    fun fading() = endFrame != 0L && frames >= fadeStart

    private fun softClip(x: Double): Double { // schuetzt vor Uebersteuern, linear bis 0.7
        val a = abs(x)
        return if (a <= 0.7) x else sign(x) * (0.7 + 0.3 * tanh((a - 0.7) / 0.3))
    }

    /** Fuellt n Frames (links/rechts getrennt); true = fertig (gestoppt oder Timer abgelaufen), Rest ist Stille */
    fun render(left: FloatArray, right: FloatArray, n: Int): Boolean {
        val f0 = frames
        if (timerReset) {
            timerReset = false
            val t = mix.timer
            if (t > 0) {
                val total = t * 60L * FS
                val fd = min(120L, t * 12L) * FS
                endFrame = f0 + total; fadeStart = endFrame - fd
            } else endFrame = 0L
        }
        val hz = min(mix.toneHz(), 0.45 * FS)
        if (hz != lastTone) { for (b in tone) b.set(FSpec(FType.LOWPASS, hz)); lastTone = hz }
        val dt = n.toDouble() / FS
        val lv = DoubleArray(5)
        for (i in 0..4) {
            val base = mix.levels[i].toDouble()
            val w = waves[i].step(dt, mix.waveSec[i].toDouble()) // laeuft immer weiter, damit Ein-/Ausschalten nahtlos ist
            wmix[i] = (wmix[i] + (if (mix.waves[i]) dt else -dt) / 1.5).coerceIn(0.0, 1.0) // Wellen weich ein-/ausblenden
            lv[i] = min(100.0, base * (1 + wmix[i] * (w - 1)))
            tg[i] = Mix.gain(lv[i]) * k[i]
        }
        live = lv
        val tm = Mix.gain(mix.master)
        if (stopReq && fadeOutFrom < 0) fadeOutFrom = f0
        val end = endFrame; val fs0 = fadeStart

        for (j in 0 until n) {
            for (i in 0..4) g[i] += (tg[i] - g[i]) * smooth
            m += (tm - m) * smooth
            val f = f0 + j
            var env = min(1.0, f / fadeIn)
            if (end > 0) env *= when {
                f >= end -> 0.0
                f >= fs0 -> (end - f).toDouble() / (end - fs0)
                else -> 1.0
            }
            if (fadeOutFrom >= 0) env *= max(0.0, 1.0 - (f - fadeOutFrom).toDouble() / fadeOutLen)
            for (c in 0..1) {
                gen[c].next(v)
                var s = 0.0
                for (i in 0..4) s += g[i] * v[i]
                s = tone[c].process(s)
                val o = (softClip(s * m) * env).toFloat()
                if (c == 0) left[j] = o else right[j] = o
            }
        }
        frames = f0 + n
        return (fadeOutFrom >= 0 && frames - fadeOutFrom >= fadeOutLen) || (end > 0 && frames >= end)
    }
}

/** Wiedergabe je Plattform (iOS: AVAudioEngine); Zustand fuer die Oberflaeche */
interface NoisePlayer {
    val playing: Boolean
    val renderer: NoiseRenderer?
    fun play()
    fun stop()
    /** Mischung geaendert */
    fun update() {}
    /** Zusammensetzung fuer den Sperrbildschirm */
    fun describe(text: String) {}
    fun release() {}
}
