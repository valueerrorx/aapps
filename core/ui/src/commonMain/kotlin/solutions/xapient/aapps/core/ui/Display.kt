package solutions.xapient.aapps.core.ui

import androidx.compose.runtime.Composable

/** Liefert die Display-Rotation 0..3 (Vierteldrehungen gegen den Uhrzeigersinn, wie Android Surface.ROTATION_*) */
@Composable
expect fun DisplayRotationEffect(onRotation: (Int) -> Unit)

/** System-Zurueck (Android); iOS hat keine Zurueck-Taste, dort fuehrt der Pfeil oben zurueck */
@Composable
expect fun BackEffect(onBack: () -> Unit)

/** Dezimaltrenner der aktuellen Sprache */
@Composable
expect fun decimalSeparator(): Char
