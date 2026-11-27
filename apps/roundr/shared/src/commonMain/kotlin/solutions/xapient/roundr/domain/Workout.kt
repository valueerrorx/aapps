package solutions.xapient.roundr.domain

// Ein Training; Zeiten in Sekunden. name: leer = eigenes ohne Namen, "@key" = eingebaute Vorlage
data class Workout(
    val name: String = "",
    val prepSec: Int = 10,
    val workSec: Int = 20,
    val restSec: Int = 10,
    val rounds: Int = 8,
    val sets: Int = 1,
    val setRestSec: Int = 60,
) {
    val isPreset: Boolean get() = name.startsWith("@")

    fun get(f: Field): Int = when (f) {
        Field.PREP -> prepSec
        Field.WORK -> workSec
        Field.REST -> restSec
        Field.ROUNDS -> rounds
        Field.SETS -> sets
        Field.SET_REST -> setRestSec
    }

    fun withValue(f: Field, v: Int): Workout {
        val c = v.coerceIn(f.min, f.max)
        return when (f) {
            Field.PREP -> copy(prepSec = c)
            Field.WORK -> copy(workSec = c)
            Field.REST -> copy(restSec = c)
            Field.ROUNDS -> copy(rounds = c)
            Field.SETS -> copy(sets = c)
            Field.SET_REST -> copy(setRestSec = c)
        }
    }

    // Einen Schritt rauf/runter; bearbeitete Vorlagen verlieren ihren Namen
    fun step(f: Field, dir: Int): Workout {
        val next = withValue(f, f.step(get(f), dir))
        return if (next == this) this else next.copy(name = if (isPreset) "" else name)
    }

    fun clamped(): Workout = Field.entries.fold(this) { w, f -> w.withValue(f, w.get(f)) }

    // Gleiche Werte, Name egal
    fun sameTiming(o: Workout): Boolean = copy(name = "") == o.copy(name = "")

    companion object {
        internal const val NAME_MAX = 24

        fun cleanName(s: String): String =
            s.replace('|', ' ').replace('\n', ' ').replace('\r', ' ').trim().trimStart('@').trim().take(NAME_MAX)
    }
}

enum class Field(val min: Int, val max: Int, val isTime: Boolean) {
    PREP(0, 60, true),
    WORK(5, 3600, true),
    REST(0, 3600, true),
    ROUNDS(1, 99, false),
    SETS(1, 10, false),
    SET_REST(0, 600, true);

    // Zeit: 5 s unter 1 min, 15 s unter 5 min, sonst 30 s; immer auf das Raster
    fun step(v: Int, dir: Int): Int {
        if (dir == 0) return v
        val next = if (!isTime) v + dir.coerceIn(-1, 1) else if (dir > 0) {
            val s = timeStep(v)
            (v / s + 1) * s
        } else {
            val s = timeStep(v - 1)
            (v - 1).floorDiv(s) * s
        }
        return next.coerceIn(min, max)
    }

    private fun timeStep(v: Int): Int = when {
        v < 60 -> 5
        v < 300 -> 15
        else -> 30
    }
}

object Presets {
    val all: List<Workout> = listOf(
        Workout("@tabata", prepSec = 10, workSec = 20, restSec = 10, rounds = 8),
        Workout("@tabata3", prepSec = 10, workSec = 20, restSec = 10, rounds = 8, sets = 3, setRestSec = 60),
        Workout("@hiit", prepSec = 10, workSec = 40, restSec = 20, rounds = 10),
        Workout("@emom", prepSec = 10, workSec = 60, restSec = 0, rounds = 10),
        Workout("@boxing", prepSec = 10, workSec = 180, restSec = 60, rounds = 12),
        Workout("@game", prepSec = 10, workSec = 120, restSec = 30, rounds = 5),
        Workout("@study", prepSec = 10, workSec = 1500, restSec = 300, rounds = 4),
    )

    val default: Workout get() = all.first()
}
