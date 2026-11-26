package solutions.xapient.aapps.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class AappsColors(
    val bg: Color = Color(0xFF10161B),
    val surface: Color = Color(0xFF172028),
    val surfaceHigh: Color = Color(0xFF1E2932),
    val line: Color = Color(0xFF26323C),
    val fg: Color = Color(0xFFDCE3E8),
    val fgMuted: Color = Color(0xFF8A97A2),
    val fgFaint: Color = Color(0xFF55626D),
    val accent: Color,
    val onAccent: Color = Color(0xFF0B1014),
)

// Schriften je Plattform geladen (Android: R.font, iOS: Bytes aus den Compose-Ressourcen)
expect val SpaceGrotesk: FontFamily

expect val Inter: FontFamily

@Immutable
data class AappsType(
    val display: TextStyle = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 120.sp,
        letterSpacing = (-0.04).em, fontFeatureSettings = "tnum",
    ),
    val number: TextStyle = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 20.sp, fontFeatureSettings = "tnum",
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.02).em,
    ),
    val title: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    val body: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    val label: TextStyle = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.14.em,
    ),
)

private val LocalColors = staticCompositionLocalOf { AappsColors(accent = Color(0xFFC6F24E)) }
private val LocalType = staticCompositionLocalOf { AappsType() }

object Aapps {
    val colors: AappsColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: AappsType
        @Composable @ReadOnlyComposable get() = LocalType.current
}

// Dunkle Basis, eine Akzentfarbe pro App
@Composable
fun AappsTheme(accent: Color, content: @Composable () -> Unit) {
    val colors = remember(accent) { AappsColors(accent = accent) }
    CompositionLocalProvider(LocalColors provides colors, LocalType provides AappsType(), content = content)
}
