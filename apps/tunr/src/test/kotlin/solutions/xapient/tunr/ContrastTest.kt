package solutions.xapient.tunr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.tunr.ui.GLOW_BASE
import solutions.xapient.tunr.ui.GLOW_LOCK
import solutions.xapient.tunr.ui.GLOW_TONE
import solutions.xapient.tunr.ui.TunrAccent
import solutions.xapient.tunr.ui.TunrGreen
import solutions.xapient.tunr.ui.idleLine
import solutions.xapient.tunr.ui.stateColor
import solutions.xapient.tunr.ui.zoneDim

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = TunrAccent)
    // hellster Grund hinter der Mitte: eingerastet, Lichthof voll; fuer jede Zustandsfarbe
    private fun brightest(c: Color) = c.copy(alpha = GLOW_BASE + GLOW_TONE + GLOW_LOCK).compositeOver(ui.bg)
    private val states = (0..60).map { stateColor(it.toFloat()) }

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    @Test fun textOnGlow() {
        for (s in states) {
            val g = brightest(s)
            aa(ui.fg, g, "Note auf Lichthof")
            aa(ui.fgMuted, g, "Hz und Hinweis auf Lichthof")
            aa(s, g, "Cent-Zahl in Zustandsfarbe")
        }
        aa(ui.fgMuted, ui.bg, "Note ohne Ton (gedimmt)")
    }

    // ohne Ton: Strobe, Teilstriche und Zone bleiben sichtbar
    @Test fun idleGraphicsVisible() {
        aa(idleLine(), ui.bg, "Strobe/Teilstriche im Ruhezustand", 3.0)
        // ohne Ton zeigt die Zone idleLine, mit Ton zoneDim
        aa(zoneDim(), ui.bg, "Zone gestimmt mit Ton, nicht eingerastet", 3.0)
        assertTrue("Zone eingerastet hebt sich ab", ratio(TunrGreen, zoneDim()) > 1.5)
        for (s in states) aa(s, ui.bg, "Zeiger und Strobe in Zustandsfarbe", 3.0)
    }

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Saite gestimmt, Mikrofon-Pille")
        aa(ui.fg, ui.surface, "Instrument-Pille")
        aa(ui.fgMuted, ui.surface, "Saite inaktiv, Untertitel")
        aa(ui.fg, ui.surfaceHigh, "aktuelle Saite")
        aa(ui.accent, ui.surface, "Rand festgehaltene Saite", 3.0)
        aa(ui.fgMuted, ui.surface, "Rand aktuelle Saite", 3.0)
        aa(ui.fgMuted, ui.bg, "Kammerton oben")
    }
}
