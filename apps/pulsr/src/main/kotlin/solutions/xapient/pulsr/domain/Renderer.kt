package solutions.xapient.pulsr.domain

// Fuellt Bloecke mit Klicks: Scheduler bestimmt die Samples, Stimmen klingen ueber Blockgrenzen aus.
// Keine Allokation pro Block, damit der Audio-Thread nicht auf den GC wartet.
// leadIn: Stille vor der ersten Eins, damit das Einschwingen der Ausgabe sie nicht verschluckt.
class Renderer(
    private val rate: Int,
    settings: Settings,
    private val clock: BeatClock? = null,
    private val leadIn: Int = 0,
    // jeder Klick mit absolutem Frame, Art und Schlag (Tests)
    private val events: ((Long, Kind, Int) -> Unit)? = null,
) {
    private val scheduler = Scheduler(rate, settings)
    private var clicks: ClickSet = Clicks.make(settings.sound, rate)
    private val voiceData = arrayOfNulls<FloatArray>(MAX_VOICES)
    // Index in die Stimme; negativ = beginnt erst spaeter im Block
    private val voicePos = IntArray(MAX_VOICES)
    private var quiet = leadIn
    private var shift = 0

    init {
        // alle Klaenge vorab (beim ersten Start je Rate), nicht im Audio-Thread
        Sound.entries.forEach { Clicks.make(it, rate) }
    }

    // waehrend advance zeigt frame auf den Anfang des hoerbaren Teils im Block
    private val listener = ClickListener { offset, kind, beat, _ ->
        startVoice(clicks.of(kind), offset + shift)
        events?.invoke(frame + offset, kind, beat)
    }

    // absoluter Frame am Anfang des naechsten Blocks, inkl. Vorlauf
    val frame: Long get() = leadIn - quiet + scheduler.frame

    fun apply(s: Settings) {
        scheduler.apply(s)
        // aus dem Zwischenspeicher: Umschalten erzeugt nichts neu
        clicks = Clicks.make(s.sound, rate)
    }

    fun render(out: FloatArray, n: Int) {
        out.fill(0f, 0, n)
        val q = minOf(quiet, n)
        quiet -= q
        shift = q
        if (q < n) {
            clock?.record(frame, scheduler.pos, scheduler.beatInBar, scheduler.beats, scheduler.sub, scheduler.step, scheduler.accent)
            scheduler.advance(n - q, listener)
        }
        mix(out, n, 1f, 0f)
    }

    // letzter Block beim Stopp: keine neuen Klicks, Klingendes linear auf 0
    fun fadeOut(out: FloatArray, n: Int) {
        out.fill(0f, 0, n)
        mix(out, n, 1f, -1f / n)
        voiceData.fill(null)
    }

    private fun mix(out: FloatArray, n: Int, gain: Float, slope: Float) {
        for (v in 0 until MAX_VOICES) {
            val data = voiceData[v] ?: continue
            var p = voicePos[v]
            val from = if (p < 0) -p else 0
            p += from
            var i = from
            while (i < n && p < data.size) {
                out[i] += data[p] * (gain + slope * i)
                i++
                p++
            }
            voicePos[v] += n
            if (voicePos[v] >= data.size) voiceData[v] = null
        }
    }

    private fun startVoice(data: FloatArray, offset: Int) {
        var slot = voiceData.indexOfFirst { it == null }
        if (slot < 0) {
            // alle belegt: die am weitesten ausgeklungene ersetzen
            slot = voicePos.indices.maxBy { voicePos[it] }
        }
        voiceData[slot] = data
        voicePos[slot] = -offset
    }

    companion object {
        const val MAX_VOICES = 8

        // Float -1..1 nach 16 bit, begrenzt
        fun toPcm(src: FloatArray, dst: ShortArray, n: Int) {
            for (i in 0 until n) {
                val v = (src[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
                dst[i] = v.toShort()
            }
        }
    }
}
