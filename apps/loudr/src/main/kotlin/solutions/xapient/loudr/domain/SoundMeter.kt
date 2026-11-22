package solutions.xapient.loudr.domain

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

enum class TimeWeighting(val seconds: Double) { FAST(0.125), SLOW(1.0) }

// Momentaufnahme fuer die Anzeige; Werte in dB ohne Kalibrierung, NaN = noch kein Wert
class Reading(
    val fast: Float,
    val slow: Float,
    val leq: Float,
    val minFast: Float,
    val maxFast: Float,
    val minSlow: Float,
    val maxSlow: Float,
    val seconds: Float,
    // Leq je 100 ms, aeltester zuerst
    val history: FloatArray,
    // misst gerade und ist eingeschwungen
    val live: Boolean,
) {
    fun level(w: TimeWeighting) = if (w == TimeWeighting.FAST) fast else slow
    fun min(w: TimeWeighting) = if (w == TimeWeighting.FAST) minFast else minSlow
    fun max(w: TimeWeighting) = if (w == TimeWeighting.FAST) maxFast else maxSlow

    companion object {
        val EMPTY = Reading(Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0f, FloatArray(0), false)
    }
}

// Ringpuffer fester Groesse
class History(val capacity: Int) {
    private val buf = FloatArray(capacity)
    private var start = 0
    var size = 0
        private set

    fun push(v: Float) {
        if (size < capacity) {
            buf[(start + size) % capacity] = v
            size++
        } else {
            buf[start] = v
            start = (start + 1) % capacity
        }
    }

    fun toArray(): FloatArray = FloatArray(size) { buf[(start + it) % capacity] }

    fun clear() {
        start = 0
        size = 0
    }
}

// Schallpegel aus PCM: A-Filter, Zeitbewertung Fast/Slow, Leq, Min/Max, Verlauf. Nicht threadsicher, Aufrufer sperrt.
class SoundMeter {
    private var filter: AWeighting? = null
    private var rate = 0
    private var alphaFast = 0.0
    private var alphaSlow = 0.0
    private var msFast = 0.0
    private var msSlow = 0.0
    private var hasLevel = false
    private var primed = false
    private var settle = 0
    private var primeLen = 0
    private var primeE = 0.0
    private var primeN = 0

    private var energy = 0.0
    private var count = 0L
    private var seconds = 0.0
    private var minFast = Float.NaN
    private var maxFast = Float.NaN
    private var minSlow = Float.NaN
    private var maxSlow = Float.NaN

    private var sliceLen = 0
    private var sliceE = 0.0
    private var sliceN = 0
    private val history = History(HISTORY_SLICES)
    private var historyCopy: FloatArray? = null

    // Neuer Messlauf: Filter frisch, Einschwingen abwarten; Statistik bleibt
    fun restart(sampleRate: Int) {
        filter = AWeighting(sampleRate)
        rate = sampleRate
        alphaFast = 1 - exp(-1.0 / (sampleRate * TimeWeighting.FAST.seconds))
        alphaSlow = 1 - exp(-1.0 / (sampleRate * TimeWeighting.SLOW.seconds))
        settle = (sampleRate * SETTLE_S).toInt()
        primeLen = (sampleRate * PRIME_S).toInt()
        primeE = 0.0
        primeN = 0
        primed = false
        sliceLen = sampleRate / 10
        sliceE = 0.0
        sliceN = 0
    }

    // Messlauf beendet: weitere Bloecke werden ignoriert, letzter Pegel bleibt sichtbar
    fun pause() {
        filter = null
        primed = false
    }

    fun feed(pcm: ShortArray, n: Int) {
        val f = filter ?: return
        var used = 0
        for (i in 0 until n) {
            val w = f.process(pcm[i] / FULL_SCALE)
            val e = w * w
            if (settle > 0) {
                // Filter schwingt ein; das Ende setzt den Startwert der Zeitbewertung
                if (settle <= primeLen) {
                    primeE += e
                    primeN++
                }
                settle--
                if (settle == 0) {
                    msFast = if (primeN > 0) primeE / primeN else e
                    msSlow = msFast
                    primed = true
                    hasLevel = true
                }
                continue
            }
            msFast += alphaFast * (e - msFast)
            msSlow += alphaSlow * (e - msSlow)
            energy += e
            count++
            used++
            sliceE += e
            if (++sliceN == sliceLen) {
                history.push(db(sliceE / sliceN))
                historyCopy = null
                sliceE = 0.0
                sliceN = 0
            }
        }
        if (used > 0) {
            seconds += used.toDouble() / rate
            val lf = db(msFast)
            val ls = db(msSlow)
            minFast = minOf(minFast, lf)
            maxFast = maxOf(maxFast, lf)
            minSlow = minOf(minSlow, ls)
            maxSlow = maxOf(maxSlow, ls)
        }
    }

    // Min/Max/Leq/Verlauf leeren; laeuft die Messung, bleibt der aktuelle Pegel
    fun reset() {
        energy = 0.0
        count = 0
        seconds = 0.0
        minFast = Float.NaN
        maxFast = Float.NaN
        minSlow = Float.NaN
        maxSlow = Float.NaN
        history.clear()
        historyCopy = null
        sliceE = 0.0
        sliceN = 0
        if (!primed) hasLevel = false
    }

    fun reading(): Reading {
        val h = historyCopy ?: history.toArray().also { historyCopy = it }
        return Reading(
            fast = if (hasLevel) db(msFast) else Float.NaN,
            slow = if (hasLevel) db(msSlow) else Float.NaN,
            leq = if (count > 0) db(energy / count) else Float.NaN,
            minFast = minFast, maxFast = maxFast, minSlow = minSlow, maxSlow = maxSlow,
            seconds = seconds.toFloat(),
            history = h,
            live = primed,
        )
    }

    // Fuer SavedState: Statistik, letzter Pegel, Verlauf
    fun save(): DoubleArray {
        val h = history.toArray()
        val head = doubleArrayOf(
            energy, count.toDouble(), seconds,
            minFast.toDouble(), maxFast.toDouble(), minSlow.toDouble(), maxSlow.toDouble(),
            if (hasLevel) msFast else -1.0, if (hasLevel) msSlow else -1.0,
        )
        return head + DoubleArray(h.size) { h[it].toDouble() }
    }

    fun restore(d: DoubleArray) {
        if (d.size < HEAD) return
        energy = d[0]
        count = d[1].toLong()
        seconds = d[2]
        minFast = d[3].toFloat()
        maxFast = d[4].toFloat()
        minSlow = d[5].toFloat()
        maxSlow = d[6].toFloat()
        hasLevel = d[7] >= 0 && d[8] >= 0
        msFast = max(d[7], 0.0)
        msSlow = max(d[8], 0.0)
        history.clear()
        for (i in HEAD until d.size) history.push(d[i].toFloat())
        historyCopy = null
    }

    companion object {
        const val FULL_SCALE = 32768.0
        // CDD: 90 dB SPL bei 1 kHz ergeben RMS 2500 (16 bit)
        val REF_DB: Double = 90.0 + 20 * log10(FULL_SCALE / 2500.0)
        const val SETTLE_S = 0.3
        const val PRIME_S = 0.1
        const val HISTORY_SLICES = 600
        private const val HEAD = 9

        // mittleres Quadrat (Vollausschlag = 1) -> dB
        fun db(meanSquare: Double): Float = (10 * log10(max(meanSquare, 1e-12)) + REF_DB).toFloat()

        private fun minOf(a: Float, b: Float) = if (a.isNaN()) b else min(a, b)
        private fun maxOf(a: Float, b: Float) = if (a.isNaN()) b else max(a, b)
    }
}
