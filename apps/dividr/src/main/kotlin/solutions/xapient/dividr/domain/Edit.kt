package solutions.xapient.dividr.domain

// Lokale Aenderungen: jede setzt den Stempel (neu: v = 1, sonst v + 1, by = eigenes Geraet)

fun newGroup(groupId: Long, name: String, currency: String, device: Int): Group =
    Group(setOf(groupId), Meta(Group.cleanTitle(name).trim(), currency, Stamp(1, device)))

private fun Stamp.next(device: Int) = Stamp(v + 1, device)

// Hoechste bekannte Nummer je Geraet ueber alle Datensaetze
fun Group.knowledge(): Map<Int, Int> {
    val out = HashMap<Int, Int>()
    (members.keys + entries.keys).forEach { id ->
        val d = Ids.device(id)
        val s = Ids.seq(id)
        if ((out[d] ?: Int.MIN_VALUE) < s) out[d] = s
    }
    return out
}

fun Group.setMeta(name: String, currency: String, device: Int): Group {
    val n = Group.cleanTitle(name).trim().ifEmpty { meta.name }
    if (n == meta.name && currency == meta.currency) return this
    return copy(meta = Meta(n, currency, meta.stamp.next(device)))
}

// Waehrung nur aenderbar, solange es keine Eintraege gibt
fun Group.canChangeCurrency() = entries.values.none { !it.deleted }

fun Group.addMember(id: Long, name: String, device: Int, fromStart: Boolean = true): Group {
    val m = Member(id, Group.cleanName(name).trim(), Stamp(1, device), fromStart = fromStart, known = knowledge())
    return copy(members = members + (id to m))
}

private fun Group.editMember(id: Long, device: Int, f: (Member) -> Member): Group {
    val m = members[id] ?: return this
    val n = f(m)
    if (n == m) return this
    return copy(members = members + (id to n.copy(stamp = m.stamp.next(device))))
}

fun Group.rename(id: Long, name: String, device: Int): Group {
    val n = Group.cleanName(name).trim()
    if (n.isEmpty()) return this
    return editMember(id, device) { it.copy(name = n) }
}

fun Group.setFromStart(id: Long, fromStart: Boolean, device: Int) = editMember(id, device) { it.copy(fromStart = fromStart) }

// Bezuege: zahlte etwas, steht in einer festen Auswahl oder ist Ziel eines Alias
fun Group.references(id: Long): Int {
    val entryRefs = entries.values.count { e ->
        !e.deleted && (resolve(e.payer) == id || (!e.forAll && e.forIds.any { resolve(it) == id }))
    }
    val aliasRefs = members.values.count { it.id != id && !it.removed && it.alias == id }
    return entryRefs + aliasRefs
}

fun Group.removeMember(id: Long, device: Int): Group {
    if (references(id) > 0) return this
    return editMember(id, device) { it.copy(removed = true) }
}

// Zusammenlegen: immer die groessere ID wird Alias der kleineren, damit keine Zyklen entstehen
fun Group.mergeMembers(a: Long, b: Long, device: Int): Group {
    val x = resolve(a)
    val y = resolve(b)
    if (x == y) return this
    val (from, to) = if (x > y) x to y else y to x
    return editMember(from, device) { it.copy(alias = to) }
}

fun Group.unmerge(id: Long, device: Int) = editMember(id, device) { it.copy(alias = null) }

const val MAX_RATE = 1_000_000_000_000L

// Neuer oder geaenderter Eintrag; Felder werden auf Grenzen gebracht
fun Group.putEntry(e: Entry, device: Int): Group {
    val old = entries[e.id]
    val clean = e.copy(
        title = Group.cleanTitle(e.title).trim(),
        amount = e.amount.coerceIn(1, Group.MAX_AMOUNT),
        currency = e.currency.ifEmpty { currency },
        rateMicros = if (e.currency.isEmpty() || e.currency == currency) 0 else e.rateMicros.coerceIn(1, MAX_RATE),
        forIds = if (e.forAll) emptySet() else e.forIds,
        deleted = false,
    )
    if (old != null && old.copy(stamp = clean.stamp) == clean) return this
    val stamp = old?.stamp?.next(device) ?: Stamp(1, device)
    return copy(entries = entries + (e.id to clean.copy(stamp = stamp)))
}

fun Group.deleteEntry(id: Long, device: Int): Group {
    val old = entries[id] ?: return this
    if (old.deleted) return this
    return copy(entries = entries + (id to old.tombstone(old.stamp.next(device))))
}

// Zwei getrennt angelegte Gruppen: IDs vereinigen, Inhalte kommen ueber den normalen Import
fun Group.adopt(other: Group): Group = other.copy(ids = other.ids + ids)
