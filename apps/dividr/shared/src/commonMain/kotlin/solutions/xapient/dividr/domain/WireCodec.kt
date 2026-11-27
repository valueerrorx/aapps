package solutions.xapient.dividr.domain

class CodecException(msg: String) : Exception(msg)

// Code einer neueren App-Version: nichts uebernehmen, Hinweis zum Aktualisieren
class NewerFormatException : Exception("newer format")

// Binaerformat 1: Gruppe <-> Bytes, fuer QR und Speicherung
object WireCodec {
    const val FORMAT = 1
    const val MAX_MEMBERS = 100
    const val MAX_ENTRIES = 5_000
    const val MAX_IDS = 32
    const val MAX_DEVICES = 1_000
    const val MAX_VERSION = 1_000_000

    private const val M_REMOVED = 1
    private const val M_ALIAS = 2
    private const val M_FROM_START = 4
    private const val E_PAYMENT = 1
    private const val E_DELETED = 2
    private const val E_FOR_ALL = 4
    private const val E_FOREIGN = 8

    fun encode(g: Group): ByteArray {
        val seen = HashSet<Int>()
        fun dev(id: Long) { seen += Ids.device(id) }
        seen += g.meta.stamp.by
        g.members.values.forEach { m ->
            dev(m.id); seen += m.stamp.by; m.alias?.let(::dev); seen += m.known.keys
        }
        g.entries.values.forEach { e ->
            dev(e.id); seen += e.stamp.by
            if (!e.deleted) { dev(e.payer); e.forIds.forEach(::dev) }
        }
        val devices = seen.sorted()
        val index = devices.withIndex().associate { it.value to it.index }
        val w = Writer()
        fun id(x: Long) { w.varint(index.getValue(Ids.device(x)).toLong()); w.varint(Ids.seq(x).toLong() and 0xFFFFFFFFL) }
        fun stamp(s: Stamp) { w.varint(s.v.toLong()); w.varint(index.getValue(s.by).toLong()) }

        w.byte(FORMAT)
        w.varint(g.ids.size.toLong())
        g.ids.sorted().forEach { w.long(it) }
        w.varint(devices.size.toLong())
        devices.forEach { w.int(it) }
        w.str(g.meta.name); w.str(g.meta.currency); stamp(g.meta.stamp)

        val live = g.entries.values.filter { !it.deleted }
        val base = live.minOfOrNull { it.day } ?: 0
        w.varint(zig(base.toLong()))

        w.varint(g.members.size.toLong())
        g.members.values.sortedBy { it.id }.forEach { m ->
            id(m.id); stamp(m.stamp)
            var f = 0
            if (m.removed) f = f or M_REMOVED
            if (m.alias != null) f = f or M_ALIAS
            if (m.fromStart) f = f or M_FROM_START
            w.byte(f)
            w.str(m.name)
            m.alias?.let { id(it) }
            w.varint(m.known.size.toLong())
            m.known.entries.sortedBy { it.key }.forEach { (d, s) -> w.varint(index.getValue(d).toLong()); w.varint(s.toLong() and 0xFFFFFFFFL) }
        }

        w.varint(g.entries.size.toLong())
        g.entries.values.sortedBy { it.id }.forEach { e ->
            id(e.id); stamp(e.stamp)
            val foreign = !e.deleted && e.currency.isNotEmpty() && e.currency != g.currency
            var f = 0
            if (e.kind == Kind.PAYMENT) f = f or E_PAYMENT
            if (e.deleted) f = f or E_DELETED
            if (e.forAll) f = f or E_FOR_ALL
            if (foreign) f = f or E_FOREIGN
            w.byte(f)
            if (e.deleted) return@forEach
            w.varint(e.amount)
            id(e.payer)
            w.varint(zig((e.day - base).toLong()))
            if (foreign) { w.str(e.currency); w.varint(e.rateMicros) }
            if (!e.forAll) { w.varint(e.forIds.size.toLong()); e.forIds.sorted().forEach { id(it) } }
            w.str(e.title)
        }
        return w.bytes()
    }

    // Wirft nur CodecException oder NewerFormatException
    fun decode(bytes: ByteArray): Group = try {
        read(Reader(bytes))
    } catch (e: CodecException) {
        throw e
    } catch (e: NewerFormatException) {
        throw e
    } catch (e: RuntimeException) {
        throw CodecException(e::class.simpleName ?: "error")
    }

    private fun read(r: Reader): Group {
        val format = r.byte()
        if (format > FORMAT) throw NewerFormatException()
        if (format != FORMAT) bad("format")
        val ids = List(r.count(MAX_IDS, min = 1)) { r.long() }.toSet()
        val devices = List(r.count(MAX_DEVICES, min = 1)) { r.int() }
        fun dev() = devices.getOrNull(r.varint().toIntExact()) ?: bad("device")
        fun id(): Long { val d = dev(); return Ids.of(d, r.uint()) }
        fun stamp(): Stamp {
            val v = r.varint()
            if (v !in 1..MAX_VERSION.toLong()) bad("version")
            return Stamp(v.toInt(), dev())
        }
        val name = Group.cleanTitle(r.str()).trim()
        val currency = r.str()
        if (!Money.valid(currency)) bad("currency")
        val meta = Meta(name, currency, stamp())
        val base = unzig(r.varint()).toIntExact()

        val members = LinkedHashMap<Long, Member>()
        repeat(r.count(MAX_MEMBERS)) {
            val mid = id()
            val st = stamp()
            val f = r.byte()
            val mname = Group.cleanName(r.str()).trim()
            val alias = if (f and M_ALIAS != 0) id() else null
            val known = HashMap<Int, Int>()
            repeat(r.count(devices.size)) { known[dev()] = r.uint() }
            if (mid in members) bad("duplicate member")
            members[mid] = Member(mid, mname, st, alias, f and M_REMOVED != 0, f and M_FROM_START != 0, known)
        }

        val entries = LinkedHashMap<Long, Entry>()
        repeat(r.count(MAX_ENTRIES)) {
            val eid = id()
            val st = stamp()
            val f = r.byte()
            if (eid in entries) bad("duplicate entry")
            if (f and E_DELETED != 0) {
                entries[eid] = Entry(eid, st, deleted = true)
                return@repeat
            }
            val amount = r.varint()
            if (amount !in 1..Group.MAX_AMOUNT) bad("amount")
            val payer = id()
            val day = (base + unzig(r.varint())).toIntExact()
            var cur = currency
            var rate = 0L
            if (f and E_FOREIGN != 0) {
                cur = r.str()
                if (!Money.valid(cur) || cur == currency) bad("currency")
                rate = r.varint()
                if (rate !in 0..MAX_RATE) bad("rate") // 0 = Kurs fehlt (Waehrung gleichzeitig geaendert)
            }
            val forAll = f and E_FOR_ALL != 0
            val forIds = if (forAll) emptySet() else List(r.count(MAX_MEMBERS)) { id() }.toSet()
            val kind = if (f and E_PAYMENT != 0) Kind.PAYMENT else Kind.EXPENSE
            if (kind == Kind.PAYMENT && (forAll || forIds.size != 1)) bad("payment")
            val title = Group.cleanTitle(r.str()).trim()
            entries[eid] = Entry(eid, st, kind, title, amount, cur, rate, payer, forAll, forIds, day)
        }
        if (!r.done()) bad("trailing")
        return Group(ids, meta, members, entries)
    }

    private fun bad(msg: String): Nothing = throw CodecException(msg)
    private fun zig(v: Long) = (v shl 1) xor (v shr 63)
    private fun unzig(v: Long) = (v ushr 1) xor -(v and 1)
    private fun Long.toIntExact(): Int = if (this in Int.MIN_VALUE..Int.MAX_VALUE) toInt() else bad("range")

    private class Writer {
        private val out = ByteSink()
        fun byte(v: Int) = out.write(v)
        fun varint(v: Long) {
            var x = v
            while (x and 0x7FL.inv() != 0L) { out.write(((x and 0x7F) or 0x80).toInt()); x = x ushr 7 }
            out.write(x.toInt())
        }
        fun int(v: Int) { for (s in 24 downTo 0 step 8) out.write(v ushr s and 0xFF) }
        fun long(v: Long) { for (s in 56 downTo 0 step 8) out.write((v ushr s and 0xFF).toInt()) }
        fun str(s: String) { val b = s.encodeToByteArray(); varint(b.size.toLong()); out.write(b) }
        fun bytes(): ByteArray = out.toByteArray()
    }

    private class Reader(private val b: ByteArray) {
        private var p = 0
        fun done() = p == b.size
        fun byte(): Int { if (p >= b.size) bad("eof"); return b[p++].toInt() and 0xFF }
        fun varint(): Long {
            var v = 0L
            var shift = 0
            while (true) {
                val x = byte()
                v = v or ((x and 0x7F).toLong() shl shift)
                if (x and 0x80 == 0) return v
                shift += 7
                if (shift > 63) bad("varint")
            }
        }
        fun uint(): Int { val v = varint(); if (v !in 0..0xFFFFFFFFL) bad("uint"); return v.toInt() }
        fun count(max: Int, min: Int = 0): Int { val v = varint(); if (v !in min..max.toLong()) bad("count"); return v.toInt() }
        fun int(): Int { var v = 0; repeat(4) { v = (v shl 8) or byte() }; return v }
        fun long(): Long { var v = 0L; repeat(8) { v = (v shl 8) or byte().toLong() }; return v }
        fun str(): String {
            val n = count(MAX_STR_BYTES)
            if (p + n > b.size) bad("eof")
            val s = b.decodeToString(p, p + n)
            p += n
            return s
        }
    }

    private const val MAX_STR_BYTES = 256
}
