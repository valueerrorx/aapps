package solutions.xapient.thinkr.ui

import platform.Foundation.*

actual fun formatPoints(n: Int): String {
    val f = NSNumberFormatter()
    f.locale = NSLocale.currentLocale
    f.numberStyle = NSNumberFormatterDecimalStyle
    f.maximumFractionDigits = 0uL
    return f.stringFromNumber(NSNumber(int = n)) ?: n.toString()
}
