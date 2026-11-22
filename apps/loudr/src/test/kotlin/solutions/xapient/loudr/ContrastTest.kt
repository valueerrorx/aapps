package solutions.xapient.loudr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.loudr.domain.Zone
import solutions.xapient.loudr.ui.GLOW_MAX
import solutions.xapient.loudr.ui.LoudrAccent
import solutions.xapient.loudr.ui.PAUSED_ALPHA
import solutions.xapient.loudr.ui.RIM_ALPHA
import solutions.xapient.loudr.ui.zoneColor

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = LoudrAccent)

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    // hellster Grund hinter Zahl und Vergleich: voller Lichthof der Zone
    @Test fun textOnGlow() = Zone.entries.forEach { z ->
        val glow = zoneColor(z).copy(alpha = GLOW_MAX).compositeOver(ui.bg)
        aa(ui.fg, glow, "Zahl auf Lichthof $z")
        aa(ui.fgMuted, glow, "Einheit auf Lichthof $z")
    }

    @Test fun mutedTextOnGround() {
        aa(ui.fgMuted, ui.bg, "Skala, Hinweis, Status")
        aa(ui.fgMuted, ui.surface, "Untertitel in Einstellungen")
        aa(ui.fg, ui.surface, "Werte in Einstellungen")
    }

    // Zonen: Pegelbogen aktiv und pausiert, Rand dauerhaft
    @Test fun zonesVisibleInEveryState() = Zone.entries.forEach { z ->
        val col = zoneColor(z)
        aa(col, ui.bg, "Bogen $z", 3.0)
        aa(col.copy(alpha = PAUSED_ALPHA).compositeOver(ui.bg), ui.bg, "Bogen pausiert $z", 3.0)
        aa(col.copy(alpha = RIM_ALPHA).compositeOver(ui.bg), ui.bg, "Zonen-Rand $z", 3.0)
        // Zonen-Punkt vor dem Vergleich, auch pausiert
        aa(col.copy(alpha = PAUSED_ALPHA).compositeOver(ui.bg), ui.surfaceHigh, "Bogen pausiert auf Spur $z", 3.0)
    }

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Tinte auf Akzent (Weiter, Mikrofon erlauben)")
        aa(ui.fg, ui.surface, "Pause")
        aa(ui.fgMuted, ui.bg, "Zuruecksetzen, Zahnrad", 3.0)
        aa(ui.fgMuted, ui.surface, "Kalibrier-Schritt am Anschlag", 3.0)
        aa(ui.accent, ui.bg, "Kalibrierwert", 4.5)
    }

    // Max-Marke und Spitze auf der Spur
    @Test fun markers() {
        aa(ui.fg.copy(alpha = PAUSED_ALPHA).compositeOver(ui.surfaceHigh), ui.surfaceHigh, "Max-Marke pausiert", 3.0)
        aa(ui.fg, ui.surfaceHigh, "Max-Marke", 3.0)
    }
}
