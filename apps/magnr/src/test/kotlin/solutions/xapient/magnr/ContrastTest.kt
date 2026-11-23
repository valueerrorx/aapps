package solutions.xapient.magnr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import androidx.compose.ui.graphics.lerp
import solutions.xapient.magnr.ui.ARCS
import solutions.xapient.magnr.ui.AccentLight
import solutions.xapient.magnr.ui.GLOW_MAX
import solutions.xapient.magnr.ui.MagnrAccent
import solutions.xapient.magnr.ui.PAUSED_ALPHA
import solutions.xapient.magnr.ui.PEAK_ALPHA
import solutions.xapient.magnr.ui.Warn

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = MagnrAccent)

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    // hellster Grund hinter Zahl, Einheit und Stufe: voller Lichthof des Sensorpunkts
    @Test fun textOnGlow() {
        val glow = MagnrAccent.copy(alpha = GLOW_MAX).compositeOver(ui.bg)
        aa(ui.fg, glow, "Zahl, Stufe auf Lichthof")
        aa(ui.fgMuted, glow, "Einheit, Skala auf Lichthof")
    }

    @Test fun mutedTextOnGround() {
        aa(ui.fgMuted, ui.bg, "Skala, Hinweis, Status")
        aa(ui.fgMuted, ui.surface, "Untertitel, Einfuehrung")
        aa(ui.fg, ui.surface, "Text auf Karte")
    }

    // jeder Faecherbogen aktiv und pausiert, gegen Grund und leeren Bogen; Spitzen-Bogen
    @Test fun arcsVisibleInEveryState() {
        for (i in 0 until ARCS) {
            val col = lerp(MagnrAccent, AccentLight, i / (ARCS - 1f))
            aa(col, ui.bg, "Bogen $i", 3.0)
            aa(col, ui.surfaceHigh, "Bogen $i auf leerem Bogen", 3.0)
            val dim = col.copy(alpha = PAUSED_ALPHA)
            aa(dim.compositeOver(ui.bg), ui.bg, "Bogen $i pausiert", 3.0)
            aa(dim.compositeOver(ui.surfaceHigh), ui.surfaceHigh, "Bogen $i pausiert auf leerem Bogen", 3.0)
        }
        aa(AccentLight.copy(alpha = PEAK_ALPHA * PAUSED_ALPHA).compositeOver(ui.bg), ui.bg, "Spitze pausiert", 3.0)
        aa(AccentLight.copy(alpha = PEAK_ALPHA * PAUSED_ALPHA).compositeOver(ui.surfaceHigh), ui.surfaceHigh, "Spitze auf leerem Bogen", 3.0)
    }

    // Verlaufslinie pausiert, Stufenpunkt, Spitzen-Marke
    @Test fun markers() {
        aa(MagnrAccent.copy(alpha = 0.8f * PAUSED_ALPHA).compositeOver(ui.bg), ui.bg, "Verlauf pausiert", 3.0)
        aa(MagnrAccent.copy(alpha = PAUSED_ALPHA).compositeOver(ui.bg), ui.bg, "Stufenpunkt pausiert", 3.0)
        aa(Warn, ui.bg, "Anschlag-Punkt", 3.0)
    }

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Tinte auf Akzent (Nullen)")
        aa(ui.fg, ui.surface, "Nullen pausiert")
        aa(ui.fgMuted, ui.bg, "Pause, Zahnrad, Ton aus", 3.0)
        aa(ui.accent, MagnrAccent.copy(alpha = 0.14f).compositeOver(ui.bg), "Ton an", 3.0)
        aa(ui.accent, ui.bg, "Status-Punkt", 3.0)
    }

    @Test fun cardTitles() {
        aa(ui.accent, ui.surface, "Titel Einfuehrung")
        aa(Warn, ui.surface, "Titel Kalibrieren")
    }
}
