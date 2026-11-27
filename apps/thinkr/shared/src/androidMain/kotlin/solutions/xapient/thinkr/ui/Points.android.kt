package solutions.xapient.thinkr.ui

import java.text.NumberFormat
import java.util.Locale

actual fun formatPoints(n: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(n)
