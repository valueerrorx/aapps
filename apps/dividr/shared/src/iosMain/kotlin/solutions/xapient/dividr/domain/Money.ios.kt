package solutions.xapient.dividr.domain

import platform.Foundation.*

actual typealias Locale = NSLocale

// Zahlen und Waehrungen ueber NSNumberFormatter (CLDR, wie Android ICU)
internal actual object MoneyFormat {
    private val codes: Set<String> by lazy { NSLocale.ISOCurrencyCodes.filterIsInstance<String>().toSet() }

    actual fun known(code: String): Boolean = code in codes

    actual fun digits(code: String): Int? {
        if (!known(code)) return null
        val f = NSNumberFormatter()
        f.locale = NSLocale(localeIdentifier = "en_US_POSIX")
        f.numberStyle = NSNumberFormatterCurrencyStyle
        f.currencyCode = code
        return f.maximumFractionDigits.toInt()
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
