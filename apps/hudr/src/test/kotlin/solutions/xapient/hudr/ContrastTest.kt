package solutions.xapient.hudr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.ui.Amber
import solutions.xapient.hudr.ui.GLOW_MAX
import solutions.xapient.hudr.ui.HUD_MUTED_ALPHA
import solutions.xapient.hudr.ui.HudBg
import solutions.xapient.hudr.ui.HudGreen
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.LOST_ALPHA
import solutions.xapient.hudr.ui.alertColor
import solutions.xapient.hudr.ui.hudColor

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = HudrAccent)

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    // hellster Grund hinter der Zahl: voller Lichthof der Warnfarbe
    @Test fun numberOnGlow() = Alert.entries.forEach { a ->
        val glow = alertColor(a, ui.accent).copy(alpha = GLOW_MAX).compositeOver(ui.bg)
        aa(ui.fg, glow, "Zahl auf Lichthof $a")
        aa(alertColor(a, ui.accent), glow, "Warnzahl auf Lichthof $a")
        aa(ui.fgMuted, glow, "Einheit/Skala auf Lichthof $a")
    }

    // Bogen und Spitze, auch bei verlorenem Signal gedimmt
    @Test fun arcInEveryState() = Alert.entries.forEach { a ->
        val col = alertColor(a, ui.accent)
        aa(col, ui.bg, "Bogen $a", 3.0)
        aa(col, ui.surfaceHigh, "Bogen auf Spur $a", 3.0)
        aa(col.copy(alpha = LOST_ALPHA).compositeOver(ui.bg), ui.bg, "Bogen ohne Signal $a", 3.0)    }

    @Test fun limitMark() {
        aa(Amber, ui.surfaceHigh, "Schwellenmarke auf Spur", 3.0)
        aa(Amber, ui.bg, "Schwelle in den Einstellungen", 4.5)
    }

    @Test fun mutedText() {
        aa(ui.fgMuted, ui.bg, "Status, Skala, Hinweis")
        aa(ui.fgMuted, ui.surface, "Werte-Beschriftung im Glas")
        aa(ui.fg, ui.surface, "Werte im Glas")
        aa(ui.accent, ui.bg, "Status-Punkt mit Fix", 3.0)
    }

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Tinte auf Akzent (HUD, Standort erlauben)")
        aa(ui.fgMuted, ui.bg, "Zuruecksetzen, Zahnrad", 3.0)
        aa(ui.fgMuted, ui.surface, "Schwellen-Schritt am Anschlag", 3.0)
    }

    // HUD: extrem kontrastreich auf Schwarz, Nebentext noch AA
    @Test fun hud() {
        Alert.entries.forEach { aa(hudColor(it), HudBg, "HUD-Zahl $it", 7.0) }
        aa(HudGreen.copy(alpha = HUD_MUTED_ALPHA).compositeOver(HudBg), HudBg, "HUD-Einheit")
    }
}
