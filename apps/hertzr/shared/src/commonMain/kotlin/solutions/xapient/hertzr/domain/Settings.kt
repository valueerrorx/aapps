package solutions.xapient.hertzr.domain

enum class Wave { SINE, SQUARE, TRIANGLE, SAW }

enum class Mode { TONE, SWEEP }

// Ausgang: beide Kanaele, einer allein oder rechts gegenphasig (Polaritaetstest)
enum class Output(val left: Float, val right: Float) {
    BOTH(1f, 1f),
    LEFT(1f, 0f),
    RIGHT(0f, 1f),
    INVERT(1f, -1f),
}

// Alles, was gespeichert wird; sane() haelt jeden Wert im gueltigen Bereich
data class Settings(
    val hz: Double = 440.0,
    val wave: Wave = Wave.SINE,
    val levelDb: Int = Level.DEFAULT_DB,
    val mode: Mode = Mode.TONE,
    val sweepFrom: Double = 20.0,
    val sweepTo: Double = 20_000.0,
    val sweepSec: Int = 10,
    val output: Output = Output.BOTH,
    val keepOn: Boolean = true,
) {
    fun sane(maxHz: Double = Freq.MAX): Settings = copy(
        hz = Freq.quantize(Freq.clamp(hz, maxHz)),
        levelDb = levelDb.coerceIn(Level.MIN_DB, Level.MAX_DB),
        sweepFrom = Freq.quantize(Freq.clamp(sweepFrom, maxHz)),
        sweepTo = Freq.quantize(Freq.clamp(sweepTo, maxHz)),
        sweepSec = Sweep.nearestDuration(sweepSec),
    )
}
