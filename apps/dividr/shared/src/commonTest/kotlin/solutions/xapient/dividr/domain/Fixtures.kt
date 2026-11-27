package solutions.xapient.dividr.domain

// Ein simuliertes Handy: eigene Geraete-ID, laufende Nummern
class Dev(val device: Int) {
    private var seq = 0
    fun id(): Long = Ids.of(device, ++seq)
}

object Fx {
    const val GID = 0x1234_5678_9ABCL

    fun group(dev: Dev, currency: String = "EUR", vararg names: String): Pair<Group, List<Long>> {
        var g = newGroup(GID, "Kornaten", currency, dev.device)
        val ids = names.map { n -> dev.id().also { g = g.addMember(it, n, dev.device) } }
        return g to ids
    }

    fun expense(
        dev: Dev, payer: Long, amount: Long, forIds: Set<Long>? = null, title: String = "x", day: Int = 20000,
        currency: String = "", rate: Long = 0,
    ) = Entry(
        dev.id(), Stamp(1, dev.device), title = title, amount = amount, payer = payer,
        forAll = forIds == null, forIds = forIds ?: emptySet(), day = day, currency = currency, rateMicros = rate,
    )

    fun payment(dev: Dev, from: Long, to: Long, amount: Long) =
        Entry(dev.id(), Stamp(1, dev.device), kind = Kind.PAYMENT, amount = amount, payer = from, forAll = false, forIds = setOf(to), day = 20000)
}
