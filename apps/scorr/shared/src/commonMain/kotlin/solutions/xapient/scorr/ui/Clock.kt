package solutions.xapient.scorr.ui

import kotlin.time.TimeSource

// Zeitbasis fuer Zug-Zusammenfassen und Zug-Anzeige: nur Abstaende zaehlen, wird nicht gespeichert
private val start = TimeSource.Monotonic.markNow()

fun nowMs(): Long = start.elapsedNow().inWholeMilliseconds
