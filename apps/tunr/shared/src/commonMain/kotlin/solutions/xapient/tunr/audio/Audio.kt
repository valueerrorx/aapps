package solutions.xapient.tunr.audio

import solutions.xapient.tunr.domain.Frame

// Mikrofon; im Test ersetzt. start/stop nur vom Main-Thread, Rueckrufe kommen auf dem Main-Thread
interface Mic {
    fun start(sink: (Frame) -> Unit, onError: () -> Unit): Boolean
    fun stop()
}

// Referenzton; im Test ersetzt. Aufrufe vom Main-Thread
interface TonePlayer {
    fun play(hz: Float)
    fun stop()
}
