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
import solutions.xapient.pulsr.ui.LIT_HOLD
import solutions.xapient.pulsr.ui.idleSegment
import solutions.xapient.pulsr.ui.restSegment
import androidx.compose.ui.graphics.lerp

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
        aa(idleSegment(ui), ui.bg, "Segment im Leerlauf", 3.0)
        // am Ring: Grundschein dauerhaft
        val ring = ui.accent.copy(alpha = GLOW_BASE * RING_GLOW).compositeOver(ui.bg)
        aa(idleSegment(ui), ring, "Segment im Leerlauf am Ring", 3.0)
        aa(ui.fgMuted, ui.bg, "Unterteilungs-Punkt", 3.0)
    }

    // Spielen: laufender Schlag (auch zwischen den Anschlaegen) klar vor den ruhenden, auch beim Aufblitzen des Lichthofs.
    // Ruhende Schlaege bewusst unter 3:1: die Taktart steht im Stepper, der Ring zeigt hier den Schlag.
    @Test fun litSegmentStandsOut() {
        val lit = lerp(restSegment(ui), ui.accent, LIT_HOLD)
        aa(lit, restSegment(ui), "laufend zu ruhend", 2.5)
        aa(lit, ui.bg, "laufend auf Grund", 3.0)
        val flashRing = ui.accent.copy(alpha = (GLOW_BASE + GLOW_FLASH) * RING_GLOW).compositeOver(ui.bg)
        aa(restSegment(ui), flashRing, "ruhend sichtbar", 1.6)
    }

    @Test fun controls() {
        aa(ui.onAccent, ui.accent, "Tinte auf Akzent (Start, gewaehlte Unterteilung, Tap)")
        aa(ui.fg, ui.surface, "TAP und Taktart")
        aa(ui.fgMuted, ui.surface, "Notenzeichen nicht gewaehlt")
        aa(ui.fg, ui.surfaceHigh, "gedrueckt")
        aa(ui.accent, ui.bg, "Akzent auf Grund", 3.0)
    }
}
