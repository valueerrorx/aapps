package solutions.xapient.pulsr.domain

import kotlin.math.ceil

fun interface ClickListener {
    // Versatz im Block, Art, Schlag im Takt, Unterteilung im Schlag
    fun onClick(offset: Int, kind: Kind, beat: Int, subIndex: Int)
}

// Zaehlt Frames und legt jeden Klick auf das erste Sample, an dem die Beat-Position sein Ziel erreicht.
// Die Position laeuft kontinuierlich weiter: kein Aufsummieren von Rundungsfehlern, Tempowechsel dehnt nur den laufenden Schlag.
class Scheduler(private val sampleRate: Int, settings: Settings = Settings()) {
    // Position im aktuellen Schlag, 0 <= pos < 1
    var pos = 0.0
        private set
    // naechstes Ereignis im Schlag: 0 = Schlag, 1..sub-1 = Unterteilung, sub = Schlaggrenze
    private var next = 0
    var beatInBar = 0
        private set
    // absoluter Frame am Anfang des naechsten Blocks
    var frame = 0L
        private set

    var bpm = 0; private set
    var beats = 1; private set
    var sub = 1; private set
    var accent = true; private set

    init {
        apply(settings)
    }

    // Schlaege pro Frame
    val step: Double get() = bpm / 60.0 / sampleRate

    fun apply(s: Settings) {
        val ok = s.sane()
        bpm = ok.bpm
        accent = ok.accent
        if (ok.beats != beats) {
            beats = ok.beats
            // weniger Schlaege: der naechste Schlag wird die Eins
            if (beatInBar >= beats) beatInBar = beats - 1
        }
        if (ok.sub.count != sub) {
            sub = ok.sub.count
            // nur noch nicht gespielte Unterteilungen; Ziel k/sub muss echt groesser als pos sein
            if (next != 0 || pos > 0.0) next = minOf(sub, (pos * sub).toInt() + 1)
        }
    }

    // Position im Block wie in BeatClock.at gerechnet (start + f * step): Bild und Klick kippen am selben Frame
    fun advance(n: Int, listener: ClickListener) {
        val start = pos
        val d = step
        // im Block ueberschrittene Schlaggrenzen
        var wraps = 0
        var f = 0L
        while (true) {
            val target = next.toDouble() / sub + wraps
            // erster Frame, an dem die Position das Ziel erreicht; Rundung exakt nachziehen
            var g = ceil((target - start) / d).toLong().coerceAtLeast(f)
            while (g > f && start + (g - 1) * d >= target) g--
            while (start + g * d < target) g++
            if (g >= n) break
            f = g
            if (next >= sub) {
                // Schlaggrenze: in den neuen Schlag wechseln
                wraps++
                next = 0
                beatInBar = (beatInBar + 1) % beats
            }
            val i = next++
            val kind = when {
                i != 0 -> Kind.SUB
                beatInBar == 0 && accent -> Kind.ACCENT
                else -> Kind.BEAT
            }
            listener.onClick(f.toInt(), kind, beatInBar, i)
        }
        pos = start + n * d - wraps
        frame += n
    }
}
