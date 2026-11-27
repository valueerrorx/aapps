package solutions.xapient.spinr.domain

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSString
import platform.Foundation.create

// Grapheme ueber NSString (UTF-16 wie Kotlin-Strings): Anfang der Zeichenfolge, in der index liegt
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal actual fun charBoundaryAtOrBefore(s: String, index: Int): Int {
    if (index >= s.length) return s.length
    val ns = NSString.create(string = s)
    return ns.rangeOfComposedCharacterSequenceAtIndex(index.toULong()).useContents { location.toInt() }
}
