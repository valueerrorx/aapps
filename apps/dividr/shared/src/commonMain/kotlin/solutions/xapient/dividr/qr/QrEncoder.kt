package solutions.xapient.dividr.qr

// QR-Code-Erzeugung nach ISO/IEC 18004, nur ECC M, ein Segment (alphanumerisch, sonst Bytes).
// Fuer iOS (Android nutzt ZXing); Aufbau wie ZXing, Maskenwahl nach Norm-Strafpunkten.
internal object QrEncoder {
    private const val ALNUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"

    // ECC-Codewoerter je Block und Anzahl Bloecke fuer Stufe M, Index = Version
    private val ECC_PER_BLOCK = intArrayOf(
        -1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26,
        26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
    )
    private val BLOCKS = intArrayOf(
        -1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16,
        17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49,
    )

    class Result(val size: Int, val version: Int, val dark: BooleanArray)

    fun encode(text: String): Result {
        val alnum = text.all { it in ALNUM }
        val bytes = if (alnum) null else text.encodeToByteArray()
        val count = bytes?.size ?: text.length
        var version = 1
        var bits: Bits
        while (true) {
            require(version <= 40) { "Daten zu gross fuer QR" }
            bits = Bits()
            if (bytes == null) {
                bits.add(0b0010, 4)
                bits.add(count, if (version <= 9) 9 else if (version <= 26) 11 else 13)
                var i = 0
                while (i + 1 < text.length) {
                    bits.add(ALNUM.indexOf(text[i]) * 45 + ALNUM.indexOf(text[i + 1]), 11)
                    i += 2
                }
                if (i < text.length) bits.add(ALNUM.indexOf(text[i]), 6)
            } else {
                bits.add(0b0100, 4)
                bits.add(count, if (version <= 9) 8 else 16)
                bytes.forEach { bits.add(it.toInt() and 0xFF, 8) }
            }
            if (bits.size <= dataCodewords(version) * 8) break
            version++
        }
        val capacity = dataCodewords(version) * 8
        bits.add(0, minOf(4, capacity - bits.size))
        bits.add(0, (8 - bits.size % 8) % 8)
        var pad = 0xEC
        while (bits.size < capacity) {
            bits.add(pad, 8)
            pad = pad xor 0xEC xor 0x11
        }
        val codewords = interleave(bits.toBytes(), version)
        return draw(codewords, version)
    }

    private fun rawModules(ver: Int): Int {
        var r = (16 * ver + 128) * ver + 64
        if (ver >= 2) {
            val n = ver / 7 + 2
            r -= (25 * n - 10) * n - 55
            if (ver >= 7) r -= 36
        }
        return r
    }

    fun totalCodewords(ver: Int) = rawModules(ver) / 8
    fun eccPerBlock(ver: Int) = ECC_PER_BLOCK[ver]
    fun blocks(ver: Int) = BLOCKS[ver]
    fun dataCodewords(ver: Int) = totalCodewords(ver) - ECC_PER_BLOCK[ver] * BLOCKS[ver]

    // Daten in Bloecke teilen, je Block Reed-Solomon, dann spaltenweise verschraenken
    private fun interleave(data: IntArray, ver: Int): IntArray {
        val nBlocks = BLOCKS[ver]
        val ecc = ECC_PER_BLOCK[ver]
        val total = totalCodewords(ver)
        val shortBlocks = nBlocks - total % nBlocks
        val shortLen = total / nBlocks
        val divisor = rsDivisor(ecc)
        val dataBlocks = ArrayList<IntArray>()
        val eccBlocks = ArrayList<IntArray>()
        var k = 0
        for (i in 0 until nBlocks) {
            val len = shortLen - ecc + if (i < shortBlocks) 0 else 1
            val d = data.copyOfRange(k, k + len)
            k += len
            dataBlocks += d
            eccBlocks += rsRemainder(d, divisor)
        }
        val out = IntArray(total)
        var o = 0
        for (i in 0..shortLen - ecc) for (b in dataBlocks) if (i < b.size) out[o++] = b[i]
        for (i in 0 until ecc) for (b in eccBlocks) out[o++] = b[i]
        return out
    }

    private fun gfMul(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }

    private fun rsDivisor(degree: Int): IntArray {
        val r = IntArray(degree)
        r[degree - 1] = 1
        var root = 1
        repeat(degree) {
            for (j in r.indices) {
                r[j] = gfMul(r[j], root)
                if (j + 1 < r.size) r[j] = r[j] xor r[j + 1]
            }
            root = gfMul(root, 0x02)
        }
        return r
    }

    private fun rsRemainder(data: IntArray, divisor: IntArray): IntArray {
        val r = IntArray(divisor.size)
        for (b in data) {
            val factor = b xor r[0]
            for (i in 0 until r.size - 1) r[i] = r[i + 1]
            r[r.size - 1] = 0
            for (i in r.indices) r[i] = r[i] xor gfMul(divisor[i], factor)
        }
        return r
    }

    private fun draw(codewords: IntArray, ver: Int): Result {
        val size = ver * 4 + 17
        val base = Grid(size)
        base.functionPatterns(ver)
        // Datenbits einmal platzieren, Masken darauf anwenden
        base.placeData(codewords)
        var best: Grid? = null
        var bestPenalty = Int.MAX_VALUE
        for (mask in 0..7) {
            val g = base.copy()
            g.applyMask(mask)
            g.formatBits(mask)
            val p = g.penalty()
            if (p < bestPenalty) {
                bestPenalty = p
                best = g
            }
        }
        return Result(size, ver, best!!.dark)
    }

    private class Bits {
        private val list = ArrayList<Boolean>()
        val size get() = list.size

        fun add(value: Int, n: Int) {
            for (i in n - 1 downTo 0) list += (value ushr i) and 1 != 0
        }

        fun toBytes(): IntArray = IntArray(list.size / 8) { i ->
            var v = 0
            for (j in 0 until 8) v = (v shl 1) or if (list[i * 8 + j]) 1 else 0
            v
        }
    }

    private class Grid(val size: Int, val dark: BooleanArray = BooleanArray(size * size), val fixed: BooleanArray = BooleanArray(size * size)) {
        fun copy() = Grid(size, dark.copyOf(), fixed)

        operator fun get(x: Int, y: Int) = dark[y * size + x]

        fun set(x: Int, y: Int, v: Boolean) {
            dark[y * size + x] = v
            fixed[y * size + x] = true
        }

        fun functionPatterns(ver: Int) {
            for (i in 0 until size) {
                set(6, i, i % 2 == 0)
                set(i, 6, i % 2 == 0)
            }
            finder(3, 3)
            finder(size - 4, 3)
            finder(3, size - 4)
            val pos = alignmentPositions(ver)
            val n = pos.size
            for (i in 0 until n) for (j in 0 until n) {
                if (i == 0 && j == 0 || i == 0 && j == n - 1 || i == n - 1 && j == 0) continue
                for (dy in -2..2) for (dx in -2..2) set(pos[i] + dx, pos[j] + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1)
            }
            // Formatbereich reservieren (Werte folgen je Maske), dunkles Modul
            formatBits(0)
            if (ver >= 7) {
                var rem = ver
                repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
                val bits = (ver shl 12) or rem
                for (i in 0 until 18) {
                    val b = (bits ushr i) and 1 != 0
                    val a = size - 11 + i % 3
                    val c = i / 3
                    set(a, c, b)
                    set(c, a, b)
                }
            }
        }

        private fun finder(cx: Int, cy: Int) {
            for (dy in -4..4) for (dx in -4..4) {
                val x = cx + dx
                val y = cy + dy
                if (x !in 0 until size || y !in 0 until size) continue
                val d = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                set(x, y, d != 2 && d != 4)
            }
        }

        private fun alignmentPositions(ver: Int): IntArray {
            if (ver == 1) return IntArray(0)
            val n = ver / 7 + 2
            val step = if (ver == 32) 26 else (ver * 4 + n * 2 + 1) / (n * 2 - 2) * 2
            val r = IntArray(n)
            r[0] = 6
            var p = size - 7
            for (i in n - 1 downTo 1) {
                r[i] = p
                p -= step
            }
            return r
        }

        fun formatBits(mask: Int) {
            val data = (0 shl 3) or mask // Stufe M = 00
            var rem = data
            repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
            val bits = ((data shl 10) or rem) xor 0x5412
            fun bit(i: Int) = (bits ushr i) and 1 != 0
            for (i in 0..5) set(8, i, bit(i))
            set(8, 7, bit(6))
            set(8, 8, bit(7))
            set(7, 8, bit(8))
            for (i in 9..14) set(14 - i, 8, bit(i))
            for (i in 0..7) set(size - 1 - i, 8, bit(i))
            for (i in 8..14) set(8, size - 15 + i, bit(i))
            set(8, size - 8, true)
        }

        // Zickzack von unten rechts, Spalte 6 (Timing) ausgelassen
        fun placeData(codewords: IntArray) {
            var i = 0
            val total = codewords.size * 8
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right = 5
                for (vert in 0 until size) {
                    for (j in 0..1) {
                        val x = right - j
                        val upward = ((right + 1) and 2) == 0
                        val y = if (upward) size - 1 - vert else vert
                        if (!fixed[y * size + x] && i < total) {
                            dark[y * size + x] = (codewords[i ushr 3] ushr (7 - (i and 7))) and 1 != 0
                            i++
                        }
                    }
                }
                right -= 2
            }
        }

        fun applyMask(mask: Int) {
            for (y in 0 until size) for (x in 0 until size) {
                if (fixed[y * size + x]) continue
                val invert = when (mask) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> (x / 3 + y / 2) % 2 == 0
                    5 -> x * y % 2 + x * y % 3 == 0
                    6 -> (x * y % 2 + x * y % 3) % 2 == 0
                    else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                }
                if (invert) dark[y * size + x] = !dark[y * size + x]
            }
        }

        // Strafpunkte wie ZXing MaskUtil (N1 3, N2 3, N3 40, N4 10)
        fun penalty(): Int = rule1(true) + rule1(false) + rule2() + rule3() + rule4()

        private fun rule1(horizontal: Boolean): Int {
            var p = 0
            for (i in 0 until size) {
                var run = 0
                var prev: Boolean? = null
                for (j in 0 until size) {
                    val b = if (horizontal) this[j, i] else this[i, j]
                    if (b == prev) {
                        run++
                    } else {
                        if (run >= 5) p += 3 + (run - 5)
                        run = 1
                        prev = b
                    }
                }
                if (run >= 5) p += 3 + (run - 5)
            }
            return p
        }

        private fun rule2(): Int {
            var p = 0
            for (y in 0 until size - 1) for (x in 0 until size - 1) {
                val v = this[x, y]
                if (v == this[x + 1, y] && v == this[x, y + 1] && v == this[x + 1, y + 1]) p++
            }
            return 3 * p
        }

        private fun rule3(): Int {
            var n = 0
            fun h(y: Int, x: Int) = this[x, y]
            // ausserhalb des Codes zaehlt als hell
            fun lightH(y: Int, from: Int, to: Int): Boolean {
                for (i in maxOf(from, 0) until minOf(to, size)) if (h(y, i)) return false
                return true
            }
            fun lightV(x: Int, from: Int, to: Int): Boolean {
                for (i in maxOf(from, 0) until minOf(to, size)) if (this[x, i]) return false
                return true
            }
            for (y in 0 until size) for (x in 0 until size) {
                if (x + 6 < size && h(y, x) && !h(y, x + 1) && h(y, x + 2) && h(y, x + 3) && h(y, x + 4) && !h(y, x + 5) && h(y, x + 6) &&
                    (lightH(y, x - 4, x) || lightH(y, x + 7, x + 11))
                ) n++
                if (y + 6 < size && this[x, y] && !this[x, y + 1] && this[x, y + 2] && this[x, y + 3] && this[x, y + 4] && !this[x, y + 5] && this[x, y + 6] &&
                    (lightV(x, y - 4, y) || lightV(x, y + 7, y + 11))
                ) n++
            }
            return n * 40
        }

        private fun rule4(): Int {
            val darkCount = dark.count { it }
            val total = size * size
            return kotlin.math.abs(darkCount * 2 - total) * 10 / total * 10
        }
    }
}
