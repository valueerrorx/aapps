package solutions.xapient.dozr.domain

import kotlin.math.max
import kotlin.math.min

enum class Mode { IDLE, RUNNING, DONE }

// Abgeleiteter Zustand fuer Anzeige und Dienst
enum class Phase { IDLE, RUNNING, FADING, ENDED, DONE }

// Ein Einschlaf-Timer auf der Laufzeituhr (elapsedRealtime)
data class Sleep(
    val setMin: Int = DEFAULT_MIN,
    val mode: Mode = Mode.IDLE,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
) {
    val totalMs: Long get() = endAt - startAt

    fun remaining(now: Long): Long = when (mode) {
        Mode.IDLE -> setMin * MINUTE
        Mode.RUNNING -> max(0L, endAt - now)
        Mode.DONE -> 0L
    }

    // Ausblenden beginnt fadeMs vor dem Ende, nie vor dem Start
    fun fadeStart(fadeMs: Long): Long = max(startAt, endAt - fadeMs)

    fun phase(now: Long, fadeMs: Long): Phase = when (mode) {
        Mode.IDLE -> Phase.IDLE
        Mode.DONE -> Phase.DONE
        Mode.RUNNING -> when {
            now >= endAt -> Phase.ENDED
            now >= fadeStart(fadeMs) -> Phase.FADING
            else -> Phase.RUNNING
        }
    }

    // 0 = Ausblenden beginnt, 1 = Ende
    fun fadeProgress(now: Long, fadeMs: Long): Double {
        if (mode != Mode.RUNNING) return 0.0
        val s = fadeStart(fadeMs)
        val len = endAt - s
        if (len <= 0L) return 1.0
        return ((now - s).toDouble() / len).coerceIn(0.0, 1.0)
    }

    fun start(now: Long): Sleep = copy(mode = Mode.RUNNING, startAt = now, endAt = now + setMin * MINUTE)

    // Verlaengern ab jetzt bzw. ab dem Ende; Restzeit hoechstens MAX_MIN
    fun extend(now: Long, ms: Long = EXTEND_MS): Sleep {
        if (mode != Mode.RUNNING) return this
        return copy(endAt = min(max(endAt, now) + ms, now + MAX_MIN * MINUTE))
    }

    fun stop(): Sleep = copy(mode = Mode.IDLE, startAt = 0L, endAt = 0L)

    fun finish(): Sleep = copy(mode = Mode.DONE)

    // Einstellen nur ausserhalb des Laufs; nach dem Ende zurueck in den Leerlauf
    fun set(minutes: Int): Sleep = if (mode == Mode.RUNNING) this else copy(setMin = clampMin(minutes), mode = Mode.IDLE)

    // Ende zu lange her (Prozess war tot): nicht mehr pausieren, nur aufraeumen
    fun overdue(now: Long): Boolean = mode == Mode.RUNNING && now - endAt > LATE_MS

    companion object {
        const val MINUTE = 60_000L
        const val MIN_MIN = 5
        const val MAX_MIN = 120
        const val STEP_MIN = 5
        const val DEFAULT_MIN = 30
        const val EXTEND_MS = 10 * MINUTE
        const val LATE_MS = 2 * MINUTE
        val QUICK = listOf(15, 30, 45, 60)

        fun clampMin(m: Int): Int = m.coerceIn(MIN_MIN, MAX_MIN)

        // Nach Laden: Neustart des Geraets = Laufzeituhr neu, alte Zeitpunkte wertlos -> Leerlauf
        fun restored(s: Sleep, sameBoot: Boolean, now: Long): Sleep {
            val clean = s.copy(setMin = clampMin(s.setMin))
            if (clean.mode == Mode.IDLE) return clean
            if (!sameBoot || clean.endAt <= clean.startAt || clean.startAt > now) return clean.stop()
            return clean
        }
    }
}

// Ausblend-Dauer in den Einstellungen
enum class FadeLength(val ms: Long) {
    S30(30_000L), M1(60_000L), M3(180_000L), M5(300_000L);

    companion object {
        val DEFAULT = M1
        fun of(name: String?): FadeLength = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
