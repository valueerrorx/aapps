package solutions.xapient.dividr.domain

// Pro Datensatz gewinnt der groessere Stempel: zuerst v, dann „nicht geloescht", dann das groessere Geraet
object Merge {
    private fun rank(s: Stamp, deleted: Boolean, o: Stamp, oDeleted: Boolean): Int = when {
        s.v != o.v -> s.v.compareTo(o.v)
        deleted != oDeleted -> if (deleted) -1 else 1
        else -> s.by.compareTo(o.by)
    }

    fun pick(a: Entry, b: Entry) = if (rank(a.stamp, a.deleted, b.stamp, b.deleted) >= 0) a else b
    fun pick(a: Member, b: Member) = if (rank(a.stamp, a.removed, b.stamp, b.removed) >= 0) a else b
    fun pick(a: Meta, b: Meta) = if (rank(a.stamp, false, b.stamp, false) >= 0) a else b

    private fun <T> union(a: Map<Long, T>, b: Map<Long, T>, pick: (T, T) -> T): Map<Long, T> {
        val out = HashMap(a)
        b.forEach { (k, v) -> out[k] = out[k]?.let { pick(it, v) } ?: v }
        return out
    }

    fun merge(a: Group, b: Group) = Group(
        ids = a.ids + b.ids,
        meta = pick(a.meta, b.meta),
        members = union(a.members, b.members, ::pick),
        entries = union(a.entries, b.entries, ::pick),
    )
}

data class NewPerson(
    val member: Member,
    val forAllFromStart: Int, // „alle"-Ausgaben, bei denen die Person von Anfang an mitzaehlt
    val forAllFromNow: Int,
    val sameAs: Member?, // vorhandene Person mit gleichem Namen
)

data class Changed(val before: Entry, val after: Entry)

// Probeweiser Import: was sich gegenueber dem lokalen Stand aendert
data class ImportSummary(
    val local: Group,
    val merged: Group,
    val newPeople: List<NewPerson>,
    val added: List<Entry>,
    val changed: List<Changed>,
    val deleted: List<Entry>,
) {
    val upToDate get() = merged == local

    // Uebernehmen mit den Antworten aus dem Dialog
    fun apply(fromStart: Map<Long, Boolean>, same: Set<Long>, device: Int): Group {
        var g = merged
        newPeople.forEach { p ->
            val want = fromStart[p.member.id] ?: p.member.fromStart
            if (want != g.members[p.member.id]?.fromStart) g = g.setFromStart(p.member.id, want, device)
            if (p.sameAs != null && p.member.id in same) g = g.mergeMembers(p.member.id, p.sameAs.id, device)
        }
        return g
    }

    companion object {
        fun of(local: Group, incoming: Group): ImportSummary {
            val merged = Merge.merge(local, incoming)
            val added = ArrayList<Entry>()
            val changed = ArrayList<Changed>()
            val deleted = ArrayList<Entry>()
            merged.entries.values.sortedWith(compareBy({ it.day }, { it.id })).forEach { e ->
                val before = local.entries[e.id]
                when {
                    before == null -> if (!e.deleted) added += e
                    before == e -> Unit
                    e.deleted && !before.deleted -> deleted += before
                    !e.deleted && before.deleted -> added += e
                    !e.deleted -> changed += Changed(before, e)
                }
            }
            val activeLocal = local.activeMembers()
            val newPeople = merged.members.values
                .filter { it.id !in local.members && !it.removed && it.alias == null }
                .sortedBy { it.id }
                .map { m ->
                    val forAll = merged.entries.values.filter { !it.deleted && it.kind == Kind.EXPENSE && it.forAll }
                    NewPerson(
                        member = m,
                        forAllFromStart = forAll.size,
                        forAllFromNow = forAll.count { !covers(m.known, it.id) },
                        sameAs = activeLocal.firstOrNull { Group.nameKey(it.name) == Group.nameKey(m.name) },
                    )
                }
            return ImportSummary(local, merged, newPeople, added, changed, deleted)
        }
    }
}

// Ist die ID im Wissensstand enthalten (also vorher erstellt)?
fun covers(known: Map<Int, Int>, id: Long): Boolean {
    val max = known[Ids.device(id)] ?: return false
    return Ids.seq(id) <= max
}
