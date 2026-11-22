package solutions.xapient.tunr.domain

// Analysefenster: je Hop rutscht es weiter, die juengsten Samples stehen am Ende
class SlidingWindow(val size: Int, val hop: Int) {
    init {
        require(hop in 1..size)
    }

    val data = FloatArray(size)

    // so viele Samples in Folge waren exakt 0 (gesperrtes oder belegtes Mikrofon liefert nur Nullen)
    var zeroRun = 0L
        private set

    // pcm: genau hop Samples, 16 bit
    fun push(pcm: ShortArray) {
        require(pcm.size >= hop)
        System.arraycopy(data, hop, data, 0, size - hop)
        val base = size - hop
        var zeros = true
        for (i in 0 until hop) {
            val s = pcm[i]
            if (s.toInt() != 0) zeros = false
            data[base + i] = s / 32768f
        }
        zeroRun = if (zeros) zeroRun + hop else 0L
    }

    // Pegel ueber die juengsten count Samples, wie sie der Detektor auswertet
    fun levelDb(count: Int): Float = PitchDetector.rmsDb(data, size - count, count)
}
