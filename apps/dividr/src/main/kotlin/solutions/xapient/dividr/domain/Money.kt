package solutions.xapient.dividr.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

// Betraege als Minor Units; Nachkommastellen je Waehrung (HUF/JPY = 0)
object Money {
    val COMMON = listOf("EUR", "USD", "CHF", "GBP", "HUF", "CZK", "PLN", "SEK", "NOK", "DKK", "HRK", "JPY", "TRY")

    fun valid(code: String): Boolean =
        code.length == 3 && code.all { it in 'A'..'Z' } && runCatching { Currency.getInstance(code) }.isSuccess

    fun digits(code: String): Int =
        runCatching { Currency.getInstance(code).defaultFractionDigits }.getOrNull()?.takeIf { it >= 0 } ?: 2

    fun symbol(code: String, locale: Locale): String =
        runCatching { Currency.getInstance(code).getSymbol(locale) }.getOrDefault(code)

    fun defaultCurrency(locale: Locale): String =
        runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()?.takeIf { valid(it) } ?: "EUR"

    // Zuerst umrechnen, dann teilen: amount Minor(von) * rate -> Minor(nach)
    fun convert(amount: Long, from: String, rateMicros: Long, to: String): Long {
        val shift = digits(to) - digits(from)
        var v = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(rateMicros)).movePointLeft(6)
        v = v.movePointRight(shift)
        return v.setScale(0, RoundingMode.HALF_UP).toLong()
    }

    // „12,5" oder „12.50" -> 1250; null bei unlesbar oder zu vielen Stellen
    fun parse(text: String, code: String): Long? {
        val t = text.trim().replace(" ", "").replace('\u00A0'.toString(), "").replace(',', '.')
        if (t.isEmpty() || t.count { it == '.' } > 1 || !t.all { it.isDigit() || it == '.' } || t == ".") return null
        val d = digits(code)
        val frac = t.substringAfter('.', "")
        if (frac.length > d) return null
        val v = runCatching { BigDecimal(if (t.endsWith('.')) t.dropLast(1) else t).movePointRight(d).longValueExact() }.getOrNull()
        return v?.takeIf { it in 0..Group.MAX_AMOUNT }
    }

    // Kurs „0,00253" -> Mikro-Einheiten
    fun parseRate(text: String): Long? {
        val t = text.trim().replace(',', '.')
        val v = runCatching { BigDecimal(t) }.getOrNull() ?: return null
        val m = v.movePointRight(6).setScale(0, RoundingMode.HALF_UP)
        return runCatching { m.longValueExact() }.getOrNull()?.takeIf { it in 1..MAX_RATE }
    }

    fun formatRate(micros: Long, locale: Locale): String {
        val f = DecimalFormat("0.######", DecimalFormatSymbols.getInstance(locale))
        return f.format(BigDecimal.valueOf(micros).movePointLeft(6))
    }

    // Zahl ohne Symbol, mit Tausendertrennung
    fun plain(minor: Long, code: String, locale: Locale): String {
        val d = digits(code)
        val f = DecimalFormat.getNumberInstance(locale) as DecimalFormat
        f.minimumFractionDigits = d
        f.maximumFractionDigits = d
        f.isGroupingUsed = true
        return f.format(BigDecimal.valueOf(kotlin.math.abs(minor)).movePointLeft(d))
    }

    // „1.834,20 €" bzw. „€1,834.20"; signed = immer mit Vorzeichen (echtes Minus)
    fun format(minor: Long, code: String, locale: Locale, signed: Boolean = false): String {
        val f = DecimalFormat.getCurrencyInstance(locale) as DecimalFormat
        val cur = runCatching { Currency.getInstance(code) }.getOrNull()
        if (cur != null) {
            f.currency = cur
            val sym = f.decimalFormatSymbols
            sym.currencySymbol = cur.getSymbol(locale)
            f.decimalFormatSymbols = sym
        }
        val d = digits(code)
        f.minimumFractionDigits = d
        f.maximumFractionDigits = d
        val s = f.format(BigDecimal.valueOf(kotlin.math.abs(minor)).movePointLeft(d))
        return when {
            minor < 0 -> "−$s"
            signed && minor > 0 -> "+$s"
            else -> s
        }
    }
}
