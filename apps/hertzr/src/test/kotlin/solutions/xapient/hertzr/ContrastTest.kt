package solutions.xapient.hertzr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.hertzr.ui.Amber
import solutions.xapient.hertzr.ui.GLOW_IDLE
import solutions.xapient.hertzr.ui.GLOW_PLAY
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.WAVE_IDLE_ALPHA

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = HertzrAccent)
    // hellster Grund hinter der Zahl: Mitte des Lichthofs beim Spielen
    private val glowPlay = ui.accent.copy(alpha = GLOW_PLAY).compositeOver(ui.bg)
    private val glowIdle = ui.accent.copy(alpha = GLOW_IDLE).compositeOver(ui.bg)

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    @Test fun numberAndLabelsOnGlow() {
        aa(ui.fg, glowPlay, "Zahl auf Lichthof")
        aa(ui.fgMuted, glowPlay, "Notenname und Einheit auf Lichthof")
        aa(ui.fgMuted, glowIdle, "Skalen-Beschriftung")
    }

    // Welle, Fuellung und Marke sind Grafik: >= 3:1, auch gedaempft im Leerlauf
    @Test fun graphics() {
        aa(ui.accent, glowPlay, "Welle und Fuellung", 3.0)
        aa(ui.accent.copy(alpha = WAVE_IDLE_ALPHA).compositeOver(glowIdle), glowIdle, "Welle im Leerlauf", 3.0)
        aa(ui.accent.copy(alpha = 0.32f).compositeOver(ui.bg), ui.bg, "Sweep-Bereich", 1.5)
        aa(ui.fgMuted, ui.bg, "Dekaden-Striche", 3.0)
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
