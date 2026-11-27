package solutions.xapient.spinr.domain

import java.text.BreakIterator

internal actual fun charBoundaryAtOrBefore(s: String, index: Int): Int {
    val bi = BreakIterator.getCharacterInstance().apply { setText(s) }
    return if (bi.isBoundary(index)) index else bi.preceding(index)
}
