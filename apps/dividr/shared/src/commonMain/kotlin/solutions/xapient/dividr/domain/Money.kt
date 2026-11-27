package solutions.xapient.dividr.domain

import kotlin.math.abs

/** Sprache/Region fuer Zahlen und Waehrungen; Android java.util.Locale, iOS NSLocale */
expect class Locale

// Waehrungsdaten und Zahlenformat der Plattform (Android java.text/java.util.Currency, iOS NSNumberFormatter)
internal expect object MoneyFormat {
    fun known(code: String): Boolean
    fun digits(code: String): Int?
    fun symbol(code: String, locale: Locale): String
    fun localeCurrency(locale: Locale): String?
    fun rate(micros: Long, locale: Locale): String
    fun grouping(locale: Locale): Char

    // Betrag >= 0 in Minor Units mit d Nachkommastellen
    fun plain(minor: Long, d: Int, locale: Locale): String
    fun currency(minor: Long, code: String, d: Int, locale: Locale): String
}

// Betraege als Minor Units; Nachkommastellen je Waehrung (HUF/JPY = 0)
object Money {
    val COMMON = listOf("EUR", "USD", "CHF", "GBP", "HUF", "CZK", "PLN", "SEK", "NOK", "DKK", "HRK", "JPY", "TRY")

    fun valid(code: String): Boolean =
        code.length == 3 && code.all { it in 'A'..'Z' } && MoneyFormat.known(code)

    fun digits(code: String): Int = MoneyFormat.digits(code)?.takeIf { it >= 0 } ?: 2

    fun symbol(code: String, locale: Locale): String = MoneyFormat.symbol(code, locale)

    fun defaultCurrency(locale: Locale): String =
        MoneyFormat.localeCurrency(locale)?.takeIf { valid(it) } ?: "EUR"

    // Zuerst umrechnen, dann teilen: amount Minor(von) * rate -> Minor(nach), kaufmaennisch gerundet
    fun convert(amount: Long, from: String, rateMicros: Long, to: String): Long {
        val shift = digits(to) - digits(from) - 6
        val sign = if ((amount < 0) != (rateMicros < 0)) -1 else 1
        val a = abs(amount)
        val r = abs(rateMicros)
        val v = if (shift >= 0) mulDivHalfUp(a, r * pow10(shift), 1) else mulDivHalfUp(a, r, pow10(-shift))
        return sign * v
    }

    // „12,5" oder „12.50" -> 1250; null bei unlesbar oder zu vielen Stellen
    fun parse(text: String, code: String): Long? {
        val t = text.trim().replace(" ", "").replace(' '.toString(), "").replace(',', '.')
        if (t.isEmpty() || t.count { it == '.' } > 1 || !t.all { it.isDigit() || it == '.' } || t == ".") return null
        val d = digits(code)
        val frac = t.substringAfter('.', "")
        if (frac.length > d) return null
        val v = digitsValue(t.substringBefore('.') + frac.padEnd(d, '0'))
        return v?.takeIf { it in 0..Group.MAX_AMOUNT }
    }

    // Kurs „0,00253" -> Mikro-Einheiten (wie BigDecimal: Vorzeichen, Exponent, kaufmaennisch gerundet)
    fun parseRate(text: String): Long? {
        val t = text.trim().replace(',', '.')
        val m = Regex("([+-]?)(\\d*)(?:\\.(\\d*))?(?:[eE]([+-]?\\d{1,6}))?").matchEntire(t) ?: return null
        val (sign, int, frac, exp) = m.destructured
        if (int.isEmpty() && frac.isEmpty()) return null
        val all = (int + frac).trimStart('0')
        // Stellen nach dem Komma, bezogen auf Mikro-Einheiten
        val scale = frac.length - (exp.toIntOrNull() ?: 0) - 6
        val micros = when {
            all.isEmpty() -> 0L
            scale <= 0 -> if (all.length - scale > 19) null else digitsValue(all + "0".repeat(-scale))
            scale >= all.length -> if (scale == all.length && all[0] >= '5') 1L else 0L
            else -> digitsValue(all.dropLast(scale))?.let { if (all[all.length - scale] >= '5') it + 1 else it }
        } ?: return null
        val v = if (sign == "-") -micros else micros
        return v.takeIf { it in 1..MAX_RATE }
    }

    fun formatRate(micros: Long, locale: Locale): String = MoneyFormat.rate(micros, locale)

    // Tausendertrennzeichen der Sprache
    fun grouping(locale: Locale): Char = MoneyFormat.grouping(locale)

    // Zahl ohne Symbol, mit Tausendertrennung
    fun plain(minor: Long, code: String, locale: Locale): String = MoneyFormat.plain(abs(minor), digits(code), locale)

    // Fremdwaehrung kompakt: „12.000,00 HUF"
    fun withCode(minor: Long, code: String, locale: Locale): String = plain(minor, code, locale) + " " + code

    // „1.834,20 €" bzw. „€1,834.20"; signed = immer mit Vorzeichen (echtes Minus)
    fun format(minor: Long, code: String, locale: Locale, signed: Boolean = false): String {
        val s = MoneyFormat.currency(abs(minor), code, digits(code), locale)
        return when {
            minor < 0 -> "−$s"
            signed && minor > 0 -> "+$s"
            else -> s
        }
    }

    // Ziffernfolge (auch andere Schriften) -> Zahl; null bei Ueberlauf
    private fun digitsValue(s: String): Long? {
        var v = 0L
        for (c in s) {
            val d = c.digitToIntOrNull() ?: return null
            if (v > (Long.MAX_VALUE - d) / 10) return null
            v = v * 10 + d
        }
        return v
    }

    private fun pow10(n: Int): Long {
        var v = 1L
        repeat(n) { v *= 10 }
        return v
    }

    // round(a * b / div) kaufmaennisch, a, b >= 0, 1 <= div <= 1e9; ohne Ueberlauf im Zwischenergebnis
    internal fun mulDivHalfUp(a: Long, b: Long, div: Long): Long {
        val base = 1_000_000_000L
        val x = longArrayOf(a % base, a / base % base, a / base / base)
        val y = longArrayOf(b % base, b / base % base, b / base / base)
        val p = LongArray(7)
        for (i in 0..2) {
            var carry = 0L
            for (j in 0..2) {
                val cur = p[i + j] + x[i] * y[j] + carry
                p[i + j] = cur % base
                carry = cur / base
            }
            var k = i + 3
            while (carry > 0) {
                val cur = p[k] + carry
                p[k] = cur % base
                carry = cur / base
                k++
            }
        }
        var rem = 0L
        val q = LongArray(p.size)
        for (i in p.indices.reversed()) {
            val cur = rem * base + p[i]
            q[i] = cur / div
            rem = cur % div
        }
        val v = q[0] + q[1] * base + q[2] * base * base
        return if (rem * 2 >= div) v + 1 else v
    }
}
