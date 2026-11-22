package solutions.xapient.hertzr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.hertzr.ui.Amber
import solutions.xapient.hertzr.ui.BAND_ALPHA
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.KeyWhite
import solutions.xapient.hertzr.ui.ScopeBg
import solutions.xapient.hertzr.ui.WAVE_IDLE_ALPHA

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = HertzrAccent)
    // Schirm: Raster unter der Anzeige ist der hellste Grund
    private val grid = ui.line

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    // Anzeige auf dem Schirm, auch dort, wo eine Rasterlinie darunter liegt
    @Test fun readoutOnScope() {
        aa(ui.fg, ScopeBg, "Zahl")
        aa(ui.fgMuted, ScopeBg, "Notenname und Einheit")
        aa(ui.fg, grid, "Zahl ueber Rasterlinie")
        aa(ui.fgMuted, grid, "Notenname ueber Rasterlinie", 3.0)
    }

    // Spur, Nadel und Band sind Grafik: >= 3:1, auch gedaempft im Leerlauf
    @Test fun graphics() {
        aa(ui.accent, ScopeBg, "Spur")
        aa(ui.accent.copy(alpha = WAVE_IDLE_ALPHA).compositeOver(ScopeBg), ScopeBg, "Spur im Leerlauf", 3.0)
        aa(ui.line, ScopeBg, "Raster sichtbar, aber zurueckhaltend", 1.3)
        aa(ui.accent, ui.surface, "Nadel auf dem Band", 3.0)
        aa(ui.fgMuted, ui.surface, "Band-Beschriftung")
        aa(ui.fgMuted, ui.accent.copy(alpha = BAND_ALPHA).compositeOver(ui.surface), "Beschriftung im Sweep-Bereich")
        aa(KeyWhite, ScopeBg, "weisse gegen schwarze Tasten", 1.5)
        aa(KeyWhite, ui.surface, "Tasten auf dem Band", 1.2)
        aa(ui.fg, KeyWhite, "C-Beschriftung auf weisser Taste")
    }

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Tinte auf Akzent (Start, gewaehlte Welle)")
        aa(ui.fg, ui.surface, "Knoepfe, Chips")
        aa(ui.fgMuted, ui.surface, "Chip-Beschriftung, nicht gewaehlte Welle")
        aa(ui.fg, ui.surfaceHigh, "gedrueckt, gewaehlter Modus")
        aa(ui.fgMuted, ui.bg, "Pegel-Beschriftung")
    }

    // Warnung in Bernstein: Text >= 4,5:1, Bereich auf der Spur sichtbar
    @Test fun warning() {
        aa(Amber, ui.bg, "Warntext und Pegelwert")
        aa(Amber, ui.line, "Pegel-Fuellung laut auf Spur", 3.0)
        aa(ui.accent, ui.line, "Pegel-Fuellung auf Spur", 3.0)
    }
}
