package solutions.xapient.dividr.domain

import platform.Foundation.*

actual typealias Locale = NSLocale

// Zahlen und Waehrungen ueber NSNumberFormatter (CLDR, wie Android ICU)
internal actual object MoneyFormat {
    private val codes: Set<String> by lazy { NSLocale.ISOCurrencyCodes.filterIsInstance<String>().toSet() }

    actual fun known(code: String): Boolean = code in codes

    // Nachkommastellen nach ISO 4217 wie java.util.Currency (Apples Formatter weicht teils ab)
    private val zero = setOf("ADP", "BEF", "BIF", "BYR", "CLP", "DJF", "ESP", "GNF", "GRD", "ISK", "ITL", "JPY", "KMF", "KRW", "LUF", "MGF", "PTE", "PYG", "RWF", "TPE", "TRL", "UGX", "UYI", "VND", "VUV", "XAF", "XOF", "XPF")
    private val three = setOf("BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND")
    private val four = setOf("CLF", "UYW")
    private val none = setOf("XAG", "XAU", "XBA", "XBB", "XBC", "XBD", "XDR", "XPD", "XPT", "XSU", "XTS", "XUA", "XXX")

    actual fun digits(code: String): Int? = when {
        !known(code) -> null
        code in zero -> 0
        code in three -> 3
        code in four -> 4
        code in none -> -1
        else -> 2
    }

    actual fun symbol(code: String, locale: Locale): String =
        locale.displayNameForKey(NSLocaleCurrencySymbol, code)?.takeIf { it.isNotEmpty() } ?: code

    actual fun localeCurrency(locale: Locale): String? = locale.objectForKey(NSLocaleCurrencyCode) as? String

    // Dezimalzahl exakt aus Mantisse und Exponent, nie ueber Double
    private fun decimal(minor: Long, d: Int): NSDecimalNumber =
        NSDecimalNumber(mantissa = minor.toULong(), exponent = (-d).toShort(), isNegative = false)

    actual fun rate(micros: Long, locale: Locale): String {
        val f = NSNumberFormatter()
        f.locale = locale
        f.numberStyle = NSNumberFormatterDecimalStyle
        f.usesGroupingSeparator = false
        f.minimumFractionDigits = 0.toULong()
        f.maximumFractionDigits = 6.toULong()
        f.minimumIntegerDigits = 1.toULong()
        return f.stringFromNumber(decimal(micros, 6)) ?: ""
    }

    actual fun grouping(locale: Locale): Char {
        val f = NSNumberFormatter()
        f.locale = locale
        f.numberStyle = NSNumberFormatterDecimalStyle
        return f.groupingSeparator?.firstOrNull() ?: ','
    }

    actual fun plain(minor: Long, d: Int, locale: Locale): String {
        val f = NSNumberFormatter()
        f.locale = locale
        f.numberStyle = NSNumberFormatterDecimalStyle
        f.usesGroupingSeparator = true
        f.minimumFractionDigits = d.toULong()
        f.maximumFractionDigits = d.toULong()
        return f.stringFromNumber(decimal(minor, d)) ?: ""
    }

    actual fun currency(minor: Long, code: String, d: Int, locale: Locale): String {
        val f = NSNumberFormatter()
        f.locale = locale
        f.numberStyle = NSNumberFormatterCurrencyStyle
        if (known(code)) {
            f.currencyCode = code
            f.currencySymbol = symbol(code, locale)
        }
        f.minimumFractionDigits = d.toULong()
        f.maximumFractionDigits = d.toULong()
        return f.stringFromNumber(decimal(minor, d)) ?: ""
    }
}
