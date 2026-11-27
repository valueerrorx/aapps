package solutions.xapient.dividr.domain

// Folgt dem Alias (max. 8 Spruenge); Zyklus -> kleinste ID im Zyklus
fun Group.resolve(id: Long): Long {
    val path = ArrayList<Long>()
    var cur = id
    repeat(MAX_HOPS + 1) {
        val i = path.indexOf(cur)
        if (i >= 0) return path.subList(i, path.size).min()
        path += cur
        val next = members[cur]?.alias ?: return cur
        if (next !in members) return cur
        cur = next
    }
    return cur
}

private const val MAX_HOPS = 8

// Aktive Personen: nicht entfernt, kein Alias; aufsteigend nach ID
fun Group.activeMembers(): List<Member> =
    members.values.filter { !it.removed && resolve(it.id) == it.id }.sortedBy { it.id }

// Feste Auswahl, die nach Aufloesung leer ist, wird wie „alle" gerechnet
fun Group.isFallback(e: Entry) = e.kind == Kind.EXPENSE && !e.forAll && fixed(e).isEmpty()

private fun Group.fixed(e: Entry): List<Long> {
    val active = activeMembers().map { it.id }.toSet()
    return e.forIds.map { resolve(it) }.filter { it in active }.distinct().sorted()
}

// Beteiligte einer Ausgabe bzw. Empfaenger einer Zahlung, aufsteigend
fun Group.participants(e: Entry): List<Long> {
    if (e.kind == Kind.PAYMENT) return e.forIds.map { resolve(it) }.distinct().sorted()
    val fixed = if (e.forAll) emptyList() else fixed(e)
    if (fixed.isNotEmpty()) return fixed
    val all = activeMembers().filter { it.fromStart || !covers(it.known, e.id) }.map { it.id }
    // niemand uebrig: der Zahler traegt es selbst, Summe bleibt 0
    return all.ifEmpty { listOf(resolve(e.payer)) }
}

// Betrag in Minor Units der Gruppenwaehrung
fun Group.inGroup(e: Entry): Long =
    if (e.currency == currency || e.currency.isEmpty()) e.amount
    else Money.convert(e.amount, e.currency, e.rateMicros, currency)

// Gleich teilen; Rest-Cent einzeln an die ersten IDs (aufsteigend)
fun shares(total: Long, ids: List<Long>): Map<Long, Long> {
    if (ids.isEmpty()) return emptyMap()
    val sorted = ids.distinct().sorted()
    val base = total / sorted.size
    val rest = total - base * sorted.size
    return sorted.mapIndexed { i, id -> id to base + if (i < rest) 1 else 0 }.toMap()
}

// Salden in Gruppen-Cent: Zahler +, Beteiligte -; Summe immer 0
fun Group.balances(): Map<Long, Long> {
    val out = LinkedHashMap<Long, Long>()
    activeMembers().forEach { out[it.id] = 0L }
    entries.values.sortedBy { it.id }.forEach { e ->
        if (e.deleted) return@forEach
        val total = inGroup(e)
        val payer = resolve(e.payer)
        val to = participants(e)
        if (to.isEmpty()) return@forEach
        out[payer] = (out[payer] ?: 0L) + total
        shares(total, to).forEach { (id, s) -> out[id] = (out[id] ?: 0L) - s }
    }
    return out
}

fun Group.totalSpent(): Long = entries.values.filter { !it.deleted && it.kind == Kind.EXPENSE }.sumOf { inGroup(it) }

fun Group.visibleEntries(): List<Entry> =
    entries.values.filter { !it.deleted }.sortedWith(compareByDescending<Entry> { it.day }.thenByDescending { it.id })

// Anzeigename mit Unterscheidung bei gleichem Namen: „Anna · 2"
fun Group.displayName(id: Long): String {
    val r = resolve(id)
    val m = members[r] ?: return "?"
    val same = members.values.filter { !it.removed && resolve(it.id) == it.id && Group.nameKey(it.name) == Group.nameKey(m.name) }
        .map { it.id }.sorted()
    val i = same.indexOf(r)
    return if (same.size > 1 && i > 0) "${m.name} · ${i + 1}" else m.name
}
