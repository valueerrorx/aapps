package solutions.xapient.hudr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.ui.Amber
import solutions.xapient.hudr.ui.FAINT_ALPHA
import solutions.xapient.hudr.ui.HudFont
import solutions.xapient.hudr.ui.HudPalette
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.LOST_ALPHA
import solutions.xapient.hudr.ui.Palette
import solutions.xapient.hudr.ui.Red

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = HudrAccent)
    // wie mainPalette(), ohne Compose
    private val main = Palette(ui.bg, ui.accent, ui.fgMuted, ui.fgMuted.copy(alpha = FAINT_ALPHA), Amber, Red, glow = true)

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg.compositeOver(bg), bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    // Ziffern auf ihrem eigenen Lichthof, in jedem Warnzustand
    @Test fun digitsOnGlow() = Alert.entries.forEach { a ->
        val col = main.alert(a)
        val glow = col.copy(alpha = HudFont.GLOW_ALPHA).compositeOver(main.bg)
        aa(col, glow, "Ziffern auf Lichthof $a")
        aa(main.muted, glow, "Einheit neben Lichthof $a")
    }

    // Band: Zahlen und Hauptstriche lesbar, Zeiger und Schwellen-Baender als Grafik
    @Test fun tape() = listOf(main, HudPalette).forEach { p ->
        aa(p.muted, p.bg, "Band-Zahlen")
        aa(p.faint, p.bg, "Nebenstriche, Grundlinie", 3.0)
        Alert.entries.forEach { a -> aa(p.alert(a), p.bg, "Zeiger $a", 3.0) }
        aa(p.amber.copy(alpha = LOST_ALPHA), p.bg, "Bernstein-Band ohne Signal", 3.0)
        aa(p.red.copy(alpha = LOST_ALPHA), p.bg, "Rot-Band ohne Signal", 3.0)
    }

    @Test fun mutedText() {
        aa(ui.fgMuted, ui.bg, "Status, Hinweis")
        aa(ui.fg, ui.bg, "Werte")
        aa(ui.accent, ui.bg, "Status-Punkt mit Fix", 3.0)
        aa(Amber, ui.bg, "Schwelle in den Einstellungen")
    }

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Tinte auf Akzent (HUD, Standort erlauben)")
        aa(ui.fgMuted, ui.bg, "Zuruecksetzen, Zahnrad", 3.0)
        aa(ui.fgMuted, ui.surface, "Schwellen-Schritt am Anschlag", 3.0)
    }

    // HUD: extrem kontrastreich auf Schwarz
    @Test fun hud() = Alert.entries.forEach { aa(HudPalette.alert(it), HudPalette.bg, "HUD-Ziffern $it", 7.0) }
}
