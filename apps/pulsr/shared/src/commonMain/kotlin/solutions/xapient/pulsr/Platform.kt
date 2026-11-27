package solutions.xapient.pulsr

import androidx.compose.runtime.Composable

// Monotone Zeit in ms (Android uptimeMillis), Basis fuer Tap-Tempo
expect fun uptimeMillis(): Long

// Bildzeit -> Zeitbasis von Player.pulse (Android: Bildzeit ist schon System.nanoTime)
expect fun presentNanos(frameNanos: Long): Long

// Dauer eines Bilds in ns (aus der Bildwiederholrate)
@Composable
expect fun frameNanos(): Long

// Haptik „geht nicht" (Rad am Rand des Tempobereichs)
@Composable
expect fun rememberReject(): () -> Unit
