package solutions.xapient.pulsr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.pulsr.ui.GLOW_BASE
import solutions.xapient.pulsr.ui.GLOW_FLASH
import solutions.xapient.pulsr.ui.PulsrAccent
import solutions.xapient.pulsr.ui.RING_GLOW
import solutions.xapient.pulsr.ui.dimSegment

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1, Grafik >= 3:1
class ContrastTest {
    private val ui = AappsColors(accent = PulsrAccent)
    // hellster Grund hinter der Zahl: Lichthof im Moment des Anschlags
    private val brightest = ui.accent.copy(alpha = GLOW_BASE + GLOW_FLASH).compositeOver(ui.bg)

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
        aa(ui.fg, brightest, "Zahl auf Lichthof")
        aa(ui.fgMuted, brightest, "Tempobezeichnung auf Lichthof")
        aa(ui.fgMuted, ui.bg, "Tempobezeichnung auf Grund")
    }

    // Schlag-Segmente zeigen die Taktart, auch gestoppt: Grafik-Kontrast >= 3:1
    @Test fun idleSegmentsVisible() {
        aa(dimSegment(), ui.bg, "Segment gedimmt", 3.0)
        // am Ring: Grundschein dauerhaft, das Aufblitzen nur Sekundenbruchteile
        val ring = ui.accent.copy(alpha = GLOW_BASE * RING_GLOW).compositeOver(ui.bg)
        aa(dimSegment(), ring, "Segment gedimmt am Ring", 3.0)
        val flashRing = ui.accent.copy(alpha = (GLOW_BASE + GLOW_FLASH) * RING_GLOW).compositeOver(ui.bg)
        aa(dimSegment(), flashRing, "Segment gedimmt beim Aufblitzen", 3.0)
        aa(ui.fgMuted, ui.bg, "Unterteilungs-Punkt", 3.0)
    }

    // aktiver Schlag hebt sich vom gedimmten ab
    @Test fun litSegmentStandsOut() = assertTrue(ratio(ui.accent, dimSegment()) > 1.5)

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Tinte auf Akzent (Start, gewaehlte Unterteilung, Tap)")
        aa(ui.fg, ui.surface, "TAP und Taktart")
        aa(ui.fgMuted, ui.surface, "Notenzeichen nicht gewaehlt")
        aa(ui.fg, ui.surfaceHigh, "gedrueckt")
        aa(ui.accent, ui.bg, "Akzent auf Grund", 3.0)
    }
}
