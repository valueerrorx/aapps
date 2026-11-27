package solutions.xapient.dividr.domain

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency

actual typealias Locale = java.util.Locale

internal actual object MoneyFormat {
    private fun cur(code: String): Currency? = runCatching { Currency.getInstance(code) }.getOrNull()

    actual fun known(code: String): Boolean = cur(code) != null

    actual fun digits(code: String): Int? = cur(code)?.defaultFractionDigits

    actual fun symbol(code: String, locale: Locale): String = cur(code)?.getSymbol(locale) ?: code

    actual fun localeCurrency(locale: Locale): String? = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()

    actual fun rate(micros: Long, locale: Locale): String {
        val f = DecimalFormat("0.######", DecimalFormatSymbols.getInstance(locale))
        return f.format(BigDecimal.valueOf(micros).movePointLeft(6))
    }

    actual fun grouping(locale: Locale): Char = DecimalFormatSymbols.getInstance(locale).groupingSeparator

    actual fun plain(minor: Long, d: Int, locale: Locale): String {
        val f = DecimalFormat.getNumberInstance(locale) as DecimalFormat
        f.minimumFractionDigits = d
        f.maximumFractionDigits = d
        f.isGroupingUsed = true
        return f.format(BigDecimal.valueOf(minor).movePointLeft(d))
    }

    actual fun currency(minor: Long, code: String, d: Int, locale: Locale): String {
        val f = DecimalFormat.getCurrencyInstance(locale) as DecimalFormat
        val cur = cur(code)
        if (cur != null) {
            f.currency = cur
            val sym = f.decimalFormatSymbols
            sym.currencySymbol = cur.getSymbol(locale)
            f.decimalFormatSymbols = sym
        }
        f.minimumFractionDigits = d
        f.maximumFractionDigits = d
        return f.format(BigDecimal.valueOf(minor).movePointLeft(d))
    }
}
