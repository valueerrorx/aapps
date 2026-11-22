package solutions.xapient.roundr.domain

// Speicherformat: eine Zeile je Training, Felder mit „|"; kaputte Daten werden still verworfen
object Codec {
    private const val V = "w1"
    const val MAX_SAVED = 20

    fun encode(w: Workout): String = listOf(
        V, Workout.cleanName(w.name).let { if (w.isPreset) w.name else it },
        w.prepSec, w.workSec, w.restSec, w.rounds, w.sets, w.setRestSec,
    ).joinToString("|")

    fun decode(s: String?): Workout? {
        val p = s?.split('|') ?: return null
        if (p.size != 8 || p[0] != V) return null
        val n = p.drop(2).map { it.toIntOrNull() ?: return null }
        return Workout(p[1], n[0], n[1], n[2], n[3], n[4], n[5]).clamped()
    }

    fun encodeList(list: List<Workout>): String = list.joinToString("\n") { encode(it) }

    fun decodeList(s: String?): List<Workout> =
        s?.lineSequence()?.mapNotNull { decode(it) }?.filter { it.name.isNotBlank() && !it.isPreset }
            ?.distinctBy { it.name }?.take(MAX_SAVED)?.toList() ?: emptyList()

    // Speichern unter Namen: gleicher Name ersetzt, neu vorne; null = Name leer oder Liste voll
    fun save(list: List<Workout>, w: Workout, name: String): List<Workout>? {
        val n = Workout.cleanName(name)
        if (n.isEmpty()) return null
        val rest = list.filter { it.name != n }
        if (rest.size >= MAX_SAVED) return null
        return listOf(w.copy(name = n).clamped()) + rest
    }

    // Lauf: id|offset|laeuftSeit(-1 = angehalten)|Training
    fun encodeRun(r: Run): String =
        "r1|${r.id}|${r.session.offsetMs}|${r.session.runningSince ?: -1}|${encode(r.workout)}"

    fun decodeRun(s: String?): Run? {
        val p = s?.split('|', limit = 5) ?: return null
        if (p.size != 5 || p[0] != "r1") return null
        val id = p[1].toLongOrNull() ?: return null
        val off = p[2].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val since = p[3].toLongOrNull() ?: return null
        val w = decode(p[4]) ?: return null
        return Run(w, Session(off, since.takeIf { it >= 0 }), id)
    }
}
