package solutions.xapient.focusr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.focusr.domain.Contrast

// Gemessener Kontrast aller Text-/Flaechenpaare, auch gedimmte Zustaende (WCAG AA)
class ContrastTest {
    private val c = AappsColors(accent = FocusrAccent)

    private fun ratio(fg: Color, bg: Color) = Contrast.ratio(fg.toArgb(), bg.toArgb())

    private fun assertAtLeast(min: Double, fg: Color, bg: Color, what: String) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    @Test fun knownValues() {
        assertTrue(Math.abs(Contrast.ratio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()) - 21.0) < 0.01)
        assertTrue(Math.abs(Contrast.ratio(0xFF777777.toInt(), 0xFFFFFFFF.toInt()) - 4.48) < 0.01)
    }

    @Test fun readout() {
        assertAtLeast(4.5, c.fg, c.bg, "Restzeit")
        assertAtLeast(4.5, c.fgMuted, c.bg, "Restzeit pausiert / Zustand")
        assertAtLeast(4.5, c.accent, c.bg, "Zeit um")
    }

    // Zahlen auf dem Gehaeuse: hellste Stelle des Verlaufs zaehlt
    @Test fun dialLabels() {
        assertAtLeast(4.5, dialLabel(c), c.surfaceHigh, "Zahl auf Gehaeuse hell")
        assertAtLeast(4.5, dialLabel(c), c.surface, "Zahl auf Gehaeuse")
        // Lichthof bei voller Scheibe hellt das Gehaeuse auf
        val lit = c.accent.copy(alpha = 0.25f * 0.45f).compositeOver(c.surfaceHigh)
        assertAtLeast(4.5, dialLabel(c), lit, "Zahl im Lichthof")
    }

    @Test fun pill() {
        assertAtLeast(4.5, c.onAccent, c.accent, "Pille aktiv")
        assertAtLeast(4.5, c.fg, c.surface, "Pille inaktiv (Pause)")
    }

    // Grafik: Sektor und Griff gegen die Mulde mindestens 3:1, auch pausiert (halbe Deckkraft)
    @Test fun sectorGraphics() {
        assertAtLeast(3.0, AccentDeep, Well, "Sektor Rand")
        assertAtLeast(3.0, FocusrAccent, Well, "Sektor")
        assertAtLeast(3.0, AccentDeep.copy(alpha = DIM_ALPHA).compositeOver(Well), Well, "Sektor pausiert")
        assertAtLeast(3.0, lerp(AccentLight, Color.White, 0.55f).copy(alpha = DIM_ALPHA).compositeOver(Well), Well, "Griff pausiert")
    }

    @Test fun settings() {
        assertAtLeast(4.5, c.fg, c.surface, "Einstellung Titel")
        assertAtLeast(4.5, c.fgMuted, c.surface, "Einstellung Untertitel")
        assertAtLeast(4.5, c.onAccent, c.accent, "Segment gewaehlt")
        assertAtLeast(4.5, c.fgMuted, c.bg, "Segment frei")
    }
}
