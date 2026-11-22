package solutions.xapient.pulsr.domain

// Fuellt Bloecke mit Klicks: Scheduler bestimmt die Samples, Stimmen klingen ueber Blockgrenzen aus.
// Keine Allokation pro Block, damit der Audio-Thread nicht auf den GC wartet.
class Renderer(private val rate: Int, settings: Settings, private val clock: BeatClock? = null) {
    private val scheduler = Scheduler(rate, settings)
    private val sets = HashMap<Sound, ClickSet>()
    private var clicks: ClickSet
    private val voiceData = arrayOfNulls<FloatArray>(MAX_VOICES)
    // Index in die Stimme; negativ = beginnt erst spaeter im Block
    private val voicePos = IntArray(MAX_VOICES)

    private val listener = ClickListener { offset, kind, _, _ -> startVoice(clicks.of(kind), offset) }

    init {
        // alle Klaenge vorab: Umschalten im Lauf erzeugt nichts neu
        Sound.entries.forEach { sets[it] = Clicks.make(it, rate) }
        clicks = sets.getValue(settings.sound)
    }

    val frame: Long get() = scheduler.frame

    fun apply(s: Settings) {
        scheduler.apply(s)
        clicks = sets.getValue(s.sound)
    }

    fun render(out: FloatArray, n: Int) {
        clock?.record(scheduler.frame, scheduler.pos, scheduler.beatInBar, scheduler.beats, scheduler.sub, scheduler.step, scheduler.accent)
        scheduler.advance(n, listener)
        out.fill(0f, 0, n)
        for (v in 0 until MAX_VOICES) {
            val data = voiceData[v] ?: continue
            var p = voicePos[v]
            val from = if (p < 0) -p else 0
            p += from
            var i = from
            while (i < n && p < data.size) {
                out[i] += data[p]
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
