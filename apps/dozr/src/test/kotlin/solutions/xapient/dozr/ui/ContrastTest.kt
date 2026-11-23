package solutions.xapient.dozr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.dozr.domain.Contrast

// Gemessener Kontrast aller Text-/Flaechenpaare (WCAG AA), auch im Mondschein und gedimmt
class ContrastTest {
    private val c = AappsColors(accent = DozrAccent)

    // hellste Stelle des Hintergrund-Verlaufs oben
    private val backdropTop = Color(0xFF0F1830)

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
        for (bg in listOf(c.bg, backdropTop)) {
            assertAtLeast(4.5, c.fg, bg, "Restzeit")
            assertAtLeast(4.5, c.fgMuted, bg, "Zustand / Restzeit fertig")
            assertAtLeast(4.5, c.accent, bg, "Blendet aus")
        }
    }

    @Test fun mainActions() {
        assertAtLeast(4.5, c.onAccent, c.accent, "Pille Start")
        assertAtLeast(4.5, c.fg, c.surface, "Pille Stopp")
        assertAtLeast(4.5, c.fg, c.accent.copy(alpha = 0.16f).compositeOver(c.bg), "Schnellwahl gewaehlt")
        assertAtLeast(4.5, c.fgMuted, c.bg, "Schnellwahl frei")
        assertAtLeast(3.0, c.accent, c.bg, "Schnellwahl Rand gewaehlt")
    }

    // Zahlen am Himmel: hellster Himmel am Horizont, auch wenn der Mond direkt daneben steht
    @Test fun skyLabels() {
        assertAtLeast(4.5, SkyLabel, SkyLow, "Zahl am Himmel")
        val moonshine = DozrAccent.copy(alpha = 0.30f * 0.45f).compositeOver(SkyLow)
        assertAtLeast(4.5, SkyLabel, moonshine, "Zahl im Mondschein")
    }

    // Grafik >= 3:1: Bogen, Mond, Rasten gegen den Himmel
    @Test fun skyGraphics() {
        assertAtLeast(3.0, DozrAccent.copy(alpha = ARC_MIN_ALPHA).compositeOver(SkyLow), SkyLow, "Bogen beim Ausblenden")
        assertAtLeast(3.0, MoonLight, SkyLow, "Mond")
        assertAtLeast(3.0, MoonShade.copy(alpha = 0.675f).compositeOver(SkyLow), SkyLow, "Mond halb ausgeblendet")
        assertAtLeast(3.0, TrackDot, SkyTop, "Rasten oben")
    }

    @Test fun fadeGraph() {
        assertAtLeast(3.0, DozrAccent.copy(alpha = 0.9f).compositeOver(c.bg), c.bg, "Kurve")
        assertAtLeast(3.0, DozrAccent.copy(alpha = 0.9f * DIM_ALPHA).compositeOver(c.bg), c.bg, "Kurve fertig")
        assertAtLeast(4.5, c.fgMuted, c.bg, "Zeiten")
    }

    @Test fun settings() {
        assertAtLeast(4.5, c.fg, c.surface, "Einstellung Titel")
        assertAtLeast(4.5, c.fgMuted, c.surface, "Einstellung Untertitel")
        assertAtLeast(4.5, c.onAccent, c.accent, "Segment gewaehlt")
        assertAtLeast(4.5, c.fgMuted, c.bg, "Segment frei")
    }
}
